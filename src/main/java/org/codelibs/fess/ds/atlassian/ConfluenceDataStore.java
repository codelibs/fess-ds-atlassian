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

import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.exception.InterruptedRuntimeException;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Data store implementation for crawling Confluence content.
 * Retrieves pages, blog posts, and their comments from Confluence instances and indexes them in Fess.
 */
public class ConfluenceDataStore extends AtlassianDataStore {

    /** Logger instance for this class. */
    private static final Logger logger = LogManager.getLogger(ConfluenceDataStore.class);

    // scripts
    /** Script variable name for content data. */
    protected static final String CONTENT = "content";

    /** Script variable name for content title. */
    protected static final String CONTENT_TITLE = "title";

    /** Script variable name for content body. */
    protected static final String CONTENT_BODY = "body";

    /** Script variable name for content comments. */
    protected static final String CONTENT_COMMENTS = "comments";

    /** Script variable name for content last modified date. */
    protected static final String CONTENT_LAST_MODIFIED = "last_modified";

    /** Script variable name for content view URL. */
    protected static final String CONTENT_VIEW_URL = "view_url";

    /**
     * Default constructor.
     */
    public ConfluenceDataStore() {
    }

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final DataStoreParams paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final Map<String, Object> configMap = createConfigMap(paramMap);

        if (logger.isDebugEnabled()) {
            logger.debug("configMap: {}", configMap);
        }

        final ExecutorService executorService = newFixedThreadPool(getNumberOfThreads(paramMap));
        // processContent runs on a pool thread, so an abort (ignore_error=false) it throws would
        // otherwise be swallowed by the executor. Capture the first one and rethrow it here so
        // the crawl actually stops instead of merely logging an uncaught exception in the pool.
        final AtomicReference<DataStoreCrawlingException> abortException = new AtomicReference<>();
        try (final ConfluenceClient client = createClient(dataConfig, paramMap)) {
            client.getContents(content -> executorService.execute(() -> {
                try {
                    processContent(dataConfig, callback, configMap, paramMap, scriptMap, defaultDataMap, client, content);
                } catch (final DataStoreCrawlingException e) {
                    abortException.compareAndSet(null, e);
                }
            }));

            if (logger.isDebugEnabled()) {
                logger.debug("Shutting down thread executor.");
            }
            executorService.shutdown();
            // Wait for the pool to actually drain. A single request can now occupy a worker for
            // minutes -- five attempts at up to the read timeout plus exponential backoff -- and
            // for as long as the server asks when it supplies Retry-After. Giving up after one
            // 60-second window would let the finally block interrupt a worker that is correctly
            // waiting out a rate limit, losing the document (and, under ignore_error=false,
            // turning a transient 429 into a whole-crawl failure). The message stays as a
            // periodic progress signal.
            while (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                logger.warn("Waiting for crawler tasks to finish. Retries and rate-limit backoff can take minutes.");
            }
        } catch (final InterruptedException e) {
            throw new InterruptedRuntimeException(e);
        } finally {
            executorService.shutdownNow();
        }

        final DataStoreCrawlingException aborted = abortException.get();
        if (aborted != null) {
            throw aborted;
        }
    }

    /**
     * Creates a Confluence client with the given parameters.
     *
     * @param dataConfig the data configuration
     * @param paramMap the data store parameters
     * @return the configured Confluence client
     */
    protected ConfluenceClient createClient(final DataConfig dataConfig, final DataStoreParams paramMap) {
        return new ConfluenceClient(dataConfig, paramMap);
    }

    /**
     * Processes a single Confluence content item and indexes it.
     *
     * @param dataConfig the data configuration
     * @param callback the index update callback
     * @param configMap the configuration map
     * @param paramMap the parameter map
     * @param scriptMap the script map
     * @param defaultDataMap the default data map
     * @param client the Confluence client
     * @param content the content to process
     */
    protected void processContent(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, Object> configMap,
            final DataStoreParams paramMap, final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap,
            final ConfluenceClient client, final Content content) {
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();
        final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
        final String url = getContentViewUrl(content, client);
        final StatsKeyObject statsKey = new StatsKeyObject(url);
        // paramMap is shared across worker threads; putting the per-content stats key directly on
        // it would let concurrent threads overwrite each other's key. Store it on a thread-local
        // copy instead so callback.store() still receives it without the race.
        final DataStoreParams localParams = paramMap.newInstance();
        localParams.put(Constants.CRAWLER_STATS_KEY, statsKey);
        try {
            crawlerStatsHelper.begin(statsKey);

            final UrlFilter urlFilter = (UrlFilter) configMap.get(URL_FILTER);
            if (urlFilter != null && !urlFilter.match(url)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not matched: {}", url);
                }
                crawlerStatsHelper.discard(statsKey);
                return;
            }

            logger.info("Crawling URL: {}", url);

            final Map<String, Object> resultMap = new LinkedHashMap<>(defaultDataMap);
            final Map<String, Object> contentMap = new HashMap<>();

            contentMap.put(CONTENT_TITLE, content.getTitle());
            contentMap.put(CONTENT_BODY, getExtractedTextFromHtml(content.getBody()));
            contentMap.put(CONTENT_COMMENTS, getContentComments(content, client));
            contentMap.put(CONTENT_LAST_MODIFIED, getLastModifiedAsDate(content.getLastModified()));
            contentMap.put(CONTENT_VIEW_URL, url);
            resultMap.put(CONTENT, contentMap);

            crawlerStatsHelper.record(statsKey, StatsAction.PREPARED);

            if (logger.isDebugEnabled()) {
                logger.debug("contentMap: {}", contentMap);
            }

            final String scriptType = getScriptType(paramMap);
            for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                final Object convertValue = convertValue(scriptType, entry.getValue(), resultMap);
                if (convertValue != null) {
                    dataMap.put(entry.getKey(), convertValue);
                }
            }

            crawlerStatsHelper.record(statsKey, StatsAction.EVALUATED);

            if (logger.isDebugEnabled()) {
                logger.debug("dataMap: {}", dataMap);
            }

            if (dataMap.get("url") instanceof String statsUrl) {
                statsKey.setUrl(statsUrl);
            }

            callback.store(localParams, dataMap);
            crawlerStatsHelper.record(statsKey, StatsAction.FINISHED);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : {}", dataMap, e);

            Throwable target = e;
            if (target instanceof MultipleCrawlingAccessException) {
                final Throwable[] causes = ((MultipleCrawlingAccessException) target).getCauses();
                if (causes.length > 0) {
                    target = causes[causes.length - 1];
                }
            }

            String errorName;
            final Throwable cause = target.getCause();
            if (cause != null) {
                errorName = cause.getClass().getCanonicalName();
            } else {
                errorName = target.getClass().getCanonicalName();
            }

            final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
            failureUrlService.store(dataConfig, errorName, url, target);
            crawlerStatsHelper.record(statsKey, StatsAction.ACCESS_EXCEPTION);
            if (Boolean.FALSE.equals(configMap.get(IGNORE_ERROR))) {
                throw new DataStoreCrawlingException(url, "Failed to process " + url, target, true);
            }
        } catch (final Throwable t) {
            logger.warn("Failed to process : {}", dataMap, t);
            final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
            failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), url, t);
            crawlerStatsHelper.record(statsKey, StatsAction.EXCEPTION);
            if (Boolean.FALSE.equals(configMap.get(IGNORE_ERROR))) {
                throw new DataStoreCrawlingException(url, "Failed to process " + url, t, true);
            }
        } finally {
            crawlerStatsHelper.done(statsKey);
        }
    }

    /**
     * Gets all comments for a Confluence content item as concatenated text.
     *
     * @param content the Confluence content
     * @param client the Confluence client
     * @return the concatenated comments text
     */
    protected String getContentComments(final Content content, final ConfluenceClient client) {
        final StringBuilder sb = new StringBuilder();
        final String id = content.getId();

        client.getContentComments(id, comment -> {
            sb.append("\n\n");
            sb.append(getExtractedTextFromHtml(comment.getBody()));
        });

        return sb.toString();
    }

    /**
     * Converts a timestamp to a Date object.
     *
     * <p>{@code date} is null when {@link Content#unpackVersion} could not parse the source
     * timestamp; unboxing it here would NPE and, caught by {@link #processContent}'s catch-all,
     * silently drop the whole document instead of just its last-modified date.</p>
     *
     * @param date the timestamp in seconds, may be null
     * @return the Date object, or null if date is null
     */
    protected Date getLastModifiedAsDate(final Long date) {
        return date == null ? null : new Date(date);
    }

    /**
     * Builds the browser-facing URL of a Confluence content item.
     *
     * @param content the content item
     * @param client the Confluence client
     * @return the view URL
     */
    protected String getContentViewUrl(final Content content, final ConfluenceClient client) {
        final Space space = content.getSpace();
        return client.getEndpointStrategy().getContentViewUrl(content.getId(), space == null ? null : space.getKey());
    }

}
