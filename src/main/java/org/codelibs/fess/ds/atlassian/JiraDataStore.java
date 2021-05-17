/*
 * Copyright 2012-2021 CodeLibs Project and the Others.
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
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraDataStore extends AtlassianDataStore {

    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    // scripts
    protected static final String ISSUE = "issue";
    protected static final String ISSUE_SUMMARY = "summary";
    protected static final String ISSUE_DESCRIPTION = "description";
    protected static final String ISSUE_COMMENTS = "comments";
    protected static final String ISSUE_LAST_MODIFIED = "last_modified";
    protected static final String ISSUE_VIEW_URL = "view_url";

    @Override
    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final Map<String, Object> configMap = createConfigMap(paramMap);

        if (logger.isDebugEnabled()) {
            logger.debug("configMap: {}", configMap);
        }

        final ExecutorService executorService = newFixedThreadPool(getNumberOfThreads(paramMap));

        try (final JiraClient client = createClient(paramMap)) {
            client.getIssues(issue -> executorService
                    .execute(() -> processIssue(dataConfig, callback, configMap, paramMap, scriptMap, defaultDataMap, client, issue)));

            if (logger.isDebugEnabled()) {
                logger.debug("Shutting down thread executor.");
            }
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch (final InterruptedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Interrupted.", e);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    protected JiraClient createClient(final Map<String, String> paramMap) {
        return new JiraClient(paramMap);
    }

    protected void processIssue(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, Object> configMap,
            final Map<String, String> paramMap, final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap,
            final JiraClient client, final Issue issue) {
        final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
        final String url = getIssueViewUrl(issue, client);
        try {

            final UrlFilter urlFilter = (UrlFilter) configMap.get(URL_FILTER);
            if (urlFilter != null && !urlFilter.match(url)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not matched: {}", url);
                }
                return;
            }

            logger.info("Crawling URL: {}", url);

            final Map<String, Object> resultMap = new LinkedHashMap<>(paramMap);
            final Map<String, Object> issueMap = new HashMap<>();

            issueMap.put(ISSUE_SUMMARY, issue.getFields().getSummary());
            issueMap.put(ISSUE_DESCRIPTION, issue.getFields().getDescription());
            issueMap.put(ISSUE_COMMENTS, getIssueComments(issue, client));
            issueMap.put(ISSUE_LAST_MODIFIED, getIssueLastModified(issue));
            issueMap.put(ISSUE_VIEW_URL, url);
            resultMap.put(ISSUE, issueMap);

            if (logger.isDebugEnabled()) {
                logger.debug("issueMap: {}", issueMap);
            }

            for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                final Object convertValue = convertValue(entry.getValue(), resultMap);
                if (convertValue != null) {
                    dataMap.put(entry.getKey(), convertValue);
                }
            }

            if (logger.isDebugEnabled()) {
                logger.debug("dataMap: {}", dataMap);
            }

            callback.store(paramMap, dataMap);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : " + dataMap, e);

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
        } catch (final Throwable t) {
            logger.warn("Crawling Access Exception at : " + dataMap, t);
            final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
            failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), url, t);
        }
    }

    protected String getIssueViewUrl(final Issue issue, final JiraClient client) {
        return client.getJiraHome() + "/browse/" + issue.getKey();
    }

    protected String getIssueComments(final Issue issue, final JiraClient client) {
        final StringBuffer sb = new StringBuffer();
        final String id = issue.getId();

        client.getComments(id, comment -> {
            sb.append("\n\n");
            sb.append(getExtractedTextFromHtml(comment.getBody()));
        });

        return sb.toString();
    }

    protected Date getIssueLastModified(final Issue issue) {
        final String updated = issue.getFields().getUpdated();
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(updated);
        } catch (final ParseException e) {
            logger.warn("Failed to parse: " + updated, e);
        }
        return null;
    }

}
