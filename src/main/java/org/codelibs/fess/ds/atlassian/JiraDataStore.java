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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.exception.InterruptedRuntimeException;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.Constants;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;

/**
 * Data store implementation for crawling JIRA issues.
 * Retrieves issues and their comments from JIRA instances and indexes them in
 * Fess.
 */
public class JiraDataStore extends AtlassianDataStore {

    /** Logger instance for this class. */
    private static final Logger logger = LogManager.getLogger(JiraDataStore.class);

    // scripts
    /** Script variable name for issue data. */
    protected static final String ISSUE = "issue";

    /** Script variable name for issue summary. */
    protected static final String ISSUE_SUMMARY = "summary";

    /** Script variable name for issue description. */
    protected static final String ISSUE_DESCRIPTION = "description";

    /** Script variable name for issue comments. */
    protected static final String ISSUE_COMMENTS = "comments";

    /** Script variable name for issue last modified date. */
    protected static final String ISSUE_LAST_MODIFIED = "last_modified";

    /** Script variable name for issue view URL. */
    protected static final String ISSUE_VIEW_URL = "view_url";

    /**
     * Default constructor.
     */
    public JiraDataStore() {
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
        // processIssue runs on a pool thread, so an abort (ignore_error=false) it throws would
        // otherwise be swallowed by the executor. Capture the first one and rethrow it here so
        // the crawl actually stops instead of merely logging an uncaught exception in the pool.
        final AtomicReference<DataStoreCrawlingException> abortException = new AtomicReference<>();

        try (final JiraClient client = createClient(dataConfig, paramMap)) {
            client.getIssues(issue -> executorService.execute(() -> {
                try {
                    processIssue(dataConfig, callback, configMap, paramMap, scriptMap, defaultDataMap, client, issue);
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
     * Creates a JIRA client with the given parameters.
     *
     * @param dataConfig the data configuration
     * @param paramMap the data store parameters
     * @return the configured JIRA client
     */
    protected JiraClient createClient(final DataConfig dataConfig, final DataStoreParams paramMap) {
        return new JiraClient(dataConfig, paramMap);
    }

    /**
     * Processes a single JIRA issue and indexes it.
     *
     * @param dataConfig     the data configuration
     * @param callback       the index update callback
     * @param configMap      the configuration map
     * @param paramMap       the parameter map
     * @param scriptMap      the script map
     * @param defaultDataMap the default data map
     * @param client         the JIRA client
     * @param issue          the issue to process
     */
    protected void processIssue(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, Object> configMap,
            final DataStoreParams paramMap, final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap,
            final JiraClient client, final Issue issue) {
        final CrawlerStatsHelper crawlerStatsHelper = ComponentUtil.getCrawlerStatsHelper();
        final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
        final String url = getIssueViewUrl(issue, client);
        final StatsKeyObject statsKey = new StatsKeyObject(url);
        // paramMap is shared across worker threads; putting the per-issue stats key directly on it
        // would let concurrent threads overwrite each other's key. Store it on a thread-local copy
        // instead so callback.store() still receives it without the race.
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
            final Map<String, Object> issueMap = new HashMap<>();

            issueMap.put(ISSUE_SUMMARY, issue.getFields().getSummary());
            issueMap.put(ISSUE_DESCRIPTION, getIssueDescription(issue));
            issueMap.put(ISSUE_COMMENTS, getIssueComments(issue, client));
            issueMap.put(ISSUE_LAST_MODIFIED, getIssueLastModified(issue));
            issueMap.put(ISSUE_VIEW_URL, url);
            resultMap.put(ISSUE, issueMap);

            crawlerStatsHelper.record(statsKey, StatsAction.PREPARED);

            if (logger.isDebugEnabled()) {
                logger.debug("issueMap: {}", issueMap);
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
     * Gets the view URL for a JIRA issue.
     *
     * @param issue  the JIRA issue
     * @param client the JIRA client
     * @return the issue view URL
     */
    protected String getIssueViewUrl(final Issue issue, final JiraClient client) {
        return client.getEndpointStrategy().getIssueViewUrl(issue.getKey());
    }

    /**
     * Gets all comments for a JIRA issue as concatenated text.
     *
     * @param issue  the JIRA issue
     * @param client the JIRA client
     * @return the concatenated comments text
     */
    protected String getIssueComments(final Issue issue, final JiraClient client) {
        final StringBuilder sb = new StringBuilder();
        final String id = issue.getId();

        client.getComments(id, comment -> {
            sb.append("\n\n");
            sb.append(getCommentBodyText(comment));
        });

        return sb.toString();
    }

    /**
     * Gets the last modified date of a JIRA issue.
     *
     * @param issue the JIRA issue
     * @return the last modified date, or null if parsing fails
     */
    protected Date getIssueLastModified(final Issue issue) {
        final String updated = issue.getFields().getUpdated();
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(updated);
        } catch (final ParseException e) {
            logger.warn("Failed to parse: {}", updated, e);
        }
        return null;
    }

    /**
     * Gets the description of a JIRA issue as text.
     *
     * @param issue the JIRA issue
     * @return the description text
     */
    protected String getIssueDescription(final Issue issue) {
        final Object description = issue.getFields().getDescription();
        if (description instanceof String) {
            return (String) description;
        } else if (description instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> adf = (Map<String, Object>) description;
            return getExtractedTextFromAdf(adf);
        }
        return StringUtil.EMPTY;
    }

    /** ADF node types that end a block and therefore need a separator after them. */
    private static final Set<String> ADF_BLOCK_TYPES =
            Set.of("paragraph", "heading", "tableCell", "tableHeader", "listItem", "codeBlock", "blockquote", "panel");

    /**
     * Extracts text content from Atlassian Document Format (ADF) map.
     *
     * @param adf the ADF map
     * @return the extracted text
     */
    protected String getExtractedTextFromAdf(final Map<String, Object> adf) {
        final StringBuilder sb = new StringBuilder();
        extractTextFromAdf(adf, sb);
        return sb.toString().trim();
    }

    /**
     * Recursively extracts text from ADF objects.
     *
     * @param obj the ADF object (Map or List)
     * @param sb  the StringBuilder to append text to
     */
    @SuppressWarnings("unchecked")
    protected void extractTextFromAdf(final Object obj, final StringBuilder sb) {
        if (obj instanceof Map) {
            final Map<String, Object> map = (Map<String, Object>) obj;
            if (map.containsKey("text")) {
                final Object text = map.get("text");
                if (text != null) {
                    sb.append(text.toString());
                }
            }
            if (map.containsKey("attrs")) {
                final Object attrs = map.get("attrs");
                if (attrs instanceof Map) {
                    final Map<String, Object> attrMap = (Map<String, Object>) attrs;
                    final Object inlineText = "mention".equals(map.get("type")) ? attrMap.get("text")
                            : "inlineCard".equals(map.get("type")) ? attrMap.get("url") : null;
                    if (inlineText != null) {
                        sb.append(' ').append(inlineText).append(' ');
                    }
                }
            }
            if (map.containsKey("content")) {
                extractTextFromAdf(map.get("content"), sb);
            }

            final Object type = map.get("type");
            if (ADF_BLOCK_TYPES.contains(type)) {
                sb.append('\n');
            }
        } else if (obj instanceof List) {
            final List<Object> list = (List<Object>) obj;
            for (final Object item : list) {
                extractTextFromAdf(item, sb);
            }
        }
    }

    /**
     * Gets the body text of a comment.
     *
     * @param comment the comment
     * @return the body text
     */
    protected String getCommentBodyText(final Comment comment) {
        final Object body = comment.getBody();
        if (body instanceof String) {
            return getExtractedTextFromHtml((String) body);
        } else if (body instanceof Map) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> adf = (Map<String, Object>) body;
            return getExtractedTextFromAdf(adf);
        }
        return StringUtil.EMPTY;
    }

}
