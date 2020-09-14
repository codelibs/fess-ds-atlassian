/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.crawler.extractor.Extractor;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AtlassianDataStore extends AbstractDataStore {

    private static final Logger logger = LoggerFactory.getLogger(AtlassianDataStore.class);

    protected static final String MIMETYPE_HTML = "text/html";

    // parameters
    protected static final String IGNORE_ERROR = "ignore_error";
    protected static final String INCLUDE_PATTERN = "include_pattern";
    protected static final String EXCLUDE_PATTERN = "exclude_pattern";
    protected static final String URL_FILTER = "url_filter";
    protected static final String NUMBER_OF_THREADS = "number_of_threads";
    protected static final String READ_INTERVAL = "read_interval";

    protected String extractorName = "tikaExtractor";

    public void setExtractorName(final String extractorName) {
        this.extractorName = extractorName;
    }

    protected UrlFilter getUrlFilter(final Map<String, String> paramMap) {
        final UrlFilter urlFilter = ComponentUtil.getComponent(UrlFilter.class);
        final String include = paramMap.get(INCLUDE_PATTERN);
        if (StringUtil.isNotBlank(include)) {
            urlFilter.addInclude(include);
        }
        final String exclude = paramMap.get(EXCLUDE_PATTERN);
        if (StringUtil.isNotBlank(exclude)) {
            urlFilter.addExclude(exclude);
        }
        urlFilter.init(paramMap.get(Constants.CRAWLING_INFO_ID));
        if (logger.isDebugEnabled()) {
            logger.debug("urlFilter: {}", urlFilter);
        }
        return urlFilter;
    }

    protected ExecutorService newFixedThreadPool(final int nThreads) {
        if (logger.isDebugEnabled()) {
            logger.debug("Executor Thread Pool: {}", nThreads);
        }
        return new ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(nThreads),
                new ThreadPoolExecutor.CallerRunsPolicy());
    }

    protected Integer getNumberOfThreads(final Map<String, String> paramMap) {
        return Integer.parseInt(paramMap.getOrDefault(NUMBER_OF_THREADS, "1"));
    }

    protected boolean isIgnoreError(final Map<String, String> paramMap) {
        return paramMap.getOrDefault(IGNORE_ERROR, Constants.TRUE).equalsIgnoreCase(Constants.TRUE);
    }

    protected Map<String, Object> createConfigMap(final Map<String, String> paramMap) {
        final Map<String, Object> configMap = new HashMap<>();
        configMap.put(IGNORE_ERROR, isIgnoreError(paramMap));
        configMap.put(URL_FILTER, getUrlFilter(paramMap));
        configMap.put(READ_INTERVAL, getReadInterval(paramMap));
        return configMap;
    }

    public String getExtractedTextFromHtml(final String body) {
        return getExtractedText(body, MIMETYPE_HTML);
    }

    public String getExtractedText(final String text, final String mimeType) {
        Extractor extractor = ComponentUtil.getExtractorFactory().getExtractor(mimeType);
        final InputStream in = new ByteArrayInputStream(text.getBytes());
        if (extractor == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("use a default extractor as {} by {}", extractorName, mimeType);
            }
            extractor = ComponentUtil.getComponent(extractorName);
        }
        return extractor.getText(in, null).getContent();
    }

}
