/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.codelibs.fess.ds.atlassian;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Abstract base class for Atlassian data stores providing common functionality
 * for crawling JIRA and Confluence instances.
 */
public abstract class AtlassianDataStore extends AbstractDataStore {

    /**
     * Default constructor for Atlassian data store.
     */
    protected AtlassianDataStore() {
        super();
    }

    private static final Logger logger = LogManager.getLogger(AtlassianDataStore.class);

    /** MIME type constant for HTML content. */
    protected static final String MIMETYPE_HTML = "text/html";

    // parameters
    /** Parameter key for ignoring errors during crawling. */
    protected static final String IGNORE_ERROR = "ignore_error";
    /** Parameter key for URL include patterns. */
    protected static final String INCLUDE_PATTERN = "include_pattern";
    /** Parameter key for URL exclude patterns. */
    protected static final String EXCLUDE_PATTERN = "exclude_pattern";
    /** Parameter key for URL filter configuration. */
    protected static final String URL_FILTER = "url_filter";
    /** Parameter key for number of threads configuration. */
    protected static final String NUMBER_OF_THREADS = "number_of_threads";

    /** Name of the text extractor to use for content processing. */
    protected String extractorName = "tikaExtractor";

    /**
     * Sets the name of the text extractor.
     *
     * @param extractorName the extractor name to set
     */
    public void setExtractorName(final String extractorName) {
        this.extractorName = extractorName;
    }

    /**
     * Creates and configures a URL filter based on the provided parameters.
     *
     * @param paramMap the parameter map containing filter configuration
     * @return configured URL filter
     */
    protected UrlFilter getUrlFilter(final DataStoreParams paramMap) {
        final UrlFilter urlFilter = ComponentUtil.getComponent(UrlFilter.class);
        final String include = paramMap.getAsString(INCLUDE_PATTERN);
        if (StringUtil.isNotBlank(include)) {
            urlFilter.addInclude(include);
        }
        final String exclude = paramMap.getAsString(EXCLUDE_PATTERN);
        if (StringUtil.isNotBlank(exclude)) {
            urlFilter.addExclude(exclude);
        }
        urlFilter.init(paramMap.getAsString(Constants.CRAWLING_INFO_ID));
        if (logger.isDebugEnabled()) {
            logger.debug("urlFilter: {}", urlFilter);
        }
        return urlFilter;
    }

    /**
     * Creates a new fixed thread pool executor.
     *
     * @param nThreads the number of threads in the pool
     * @return the configured executor service
     */
    protected ExecutorService newFixedThreadPool(final int nThreads) {
        if (logger.isDebugEnabled()) {
            logger.debug("Executor Thread Pool: {}", nThreads);
        }
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(nThreads),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    /**
     * Gets the number of threads from the parameter map.
     *
     * @param paramMap the parameter map
     * @return the number of threads, defaults to 1
     */
    protected Integer getNumberOfThreads(final DataStoreParams paramMap) {
        return getIntParam(paramMap, NUMBER_OF_THREADS, 1);
    }

    /**
     * Reads an int parameter, falling back to the default when absent, unparsable, or out of
     * int range, instead of letting a bad value (e.g. {@code number_of_threads=abc}) throw a
     * {@link NumberFormatException} that kills the crawl at startup.
     *
     * @param paramMap the configuration parameters
     * @param key the parameter key
     * @param defaultValue the value used when absent, unparsable or out of range
     * @return the resolved value
     */
    protected int getIntParam(final DataStoreParams paramMap, final String key, final int defaultValue) {
        final String value = paramMap.getAsString(key, StringUtil.EMPTY);
        if (StringUtil.isBlank(value)) {
            return defaultValue;
        }
        try {
            final long longValue = Long.parseLong(value.trim());
            if (longValue < Integer.MIN_VALUE || longValue > Integer.MAX_VALUE) {
                logger.warn("Parameter \"{}\" is out of range: {}. Using {}.", key, longValue, defaultValue);
                return defaultValue;
            }
            return (int) longValue;
        } catch (final NumberFormatException e) {
            logger.warn("Parameter \"{}\" is not a number: \"{}\". Using {}.", key, value, defaultValue);
            return defaultValue;
        }
    }

    /**
     * Checks if errors should be ignored during crawling.
     *
     * @param paramMap the parameter map
     * @return true if errors should be ignored, false otherwise
     */
    protected boolean isIgnoreError(final DataStoreParams paramMap) {
        return Constants.TRUE.equalsIgnoreCase(paramMap.getAsString(IGNORE_ERROR, Constants.TRUE));
    }

    /**
     * Creates a configuration map from the provided parameters.
     *
     * @param paramMap the parameter map
     * @return the configuration map
     */
    protected Map<String, Object> createConfigMap(final DataStoreParams paramMap) {
        final Map<String, Object> configMap = new HashMap<>();
        configMap.put(IGNORE_ERROR, isIgnoreError(paramMap));
        configMap.put(URL_FILTER, getUrlFilter(paramMap));
        return configMap;
    }

    /**
     * Extracts text content from HTML.
     *
     * @param body the HTML content
     * @return the extracted text
     */
    public String getExtractedTextFromHtml(final String body) {
        return getExtractedText(body, MIMETYPE_HTML);
    }

    /**
     * Extracts text content from the given text using the specified MIME type.
     *
     * @param text the content to extract text from
     * @param mimeType the MIME type of the content
     * @return the extracted text
     */
    public String getExtractedText(final String text, final String mimeType) {
        try (final InputStream in = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8))) {
            // Ignore mimeType because the extractor specified by extractorName is always used.
            return ComponentUtil.getExtractorFactory().builder(in, null).extractorName(extractorName).extract().getContent();
        } catch (final Exception e) {
            if (!ComponentUtil.getFessConfig().isCrawlerIgnoreContentException()) {
                throw new CrawlingAccessException(e);
            }
            if (logger.isDebugEnabled()) {
                logger.warn("Could not get a text.", e);
            } else {
                logger.warn("Could not get a text. {}", e.getMessage());
            }
            return StringUtil.EMPTY;
        }
    }
}
