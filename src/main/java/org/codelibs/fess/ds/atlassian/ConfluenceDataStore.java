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
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.crawler.extractor.Extractor;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceDataStore extends AtlassianDataStore {

    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    protected static final String MIMETYPE_HTML = "text/html";

    protected static final String extractorName = "tikaExtractor";

    // scripts
    protected static final String CONTENT = "content";
    protected static final String CONTENT_TITLE = "title";
    protected static final String CONTENT_BODY = "body";
    protected static final String CONTENT_COMMENTS = "comments";
    protected static final String CONTENT_LAST_MODIFIED = "last_modified";
    protected static final String CONTENT_VIEW_URL = "view_url";

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

        final FessConfig fessConfig = ComponentUtil.getFessConfig();

        final ExecutorService executorService = newFixedThreadPool(getNumberOfThreads(paramMap));
        try (final ConfluenceClient client = createClient(paramMap)) {
            client.getContents(content ->
                    executorService.execute(() ->
                            processContent(dataConfig, callback, configMap, paramMap, scriptMap, defaultDataMap, fessConfig, client, content)
                    )
            );

            client.getBlogContents(content ->
                    executorService.execute(() ->
                            processContent(dataConfig, callback, configMap, paramMap, scriptMap, defaultDataMap, fessConfig, client, content)
                    )
            );

            if (logger.isDebugEnabled()) {
                logger.debug("Shutting down thread executor.");
            }
            executorService.shutdown();
            executorService.awaitTermination(60, TimeUnit.SECONDS);
        } catch(final InterruptedException e) {
            if (logger.isDebugEnabled()) {
                logger.debug("Interrupted.", e);
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    protected ConfluenceClient createClient(final Map<String, String> paramMap) {
        return new ConfluenceClient(paramMap);
    }

    protected void processContent(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, Object> configMap
            , final Map<String, String> paramMap, final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap,
                                  final FessConfig fessConfig, final ConfluenceClient client, final Content content) {
        final Map<String, Object> dataMap = new HashMap<>(defaultDataMap);
        final String confluenceHome = client.getConfluenceHome();
        final String url = getContentViewUrl(content, confluenceHome);
        try {

            final UrlFilter urlFilter = (UrlFilter) configMap.get(URL_FILTER);
            if (urlFilter != null && !urlFilter.match(url)) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Not matched: {}", url);
                }
                return;
            }

            logger.info("Crawling URL: {}", url);

            final Map<String, Object> resultMap = new LinkedHashMap<>(defaultDataMap);
            final Map<String, Object> contentMap = new HashMap<>();

            contentMap.put(CONTENT_TITLE, content.getTitle());
            contentMap.put(CONTENT_BODY, getExtractedTextFromBody(content.getBody()));
            contentMap.put(CONTENT_COMMENTS, getContentComments(content, client));
            contentMap.put(CONTENT_LAST_MODIFIED, getLastModifiedAsDate(content.getLastModified()));
            contentMap.put(CONTENT_VIEW_URL, url);
            resultMap.put(CONTENT, contentMap);

            if (logger.isDebugEnabled()) {
                logger.debug("contentMap: {}", contentMap);
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

    protected String getContentComments(final Content content, final ConfluenceClient client) {
        final StringBuilder sb = new StringBuilder();
        final String id = content.getId();

        client.getContentComments(id, comment -> {
                sb.append("\n\n");
                sb.append(comment.getBody());
            });

        return sb.toString();
    }

    protected Date getLastModifiedAsDate(final Long date) {
        return new Date(date * 1000L);
    }

    public static String getExtractedTextFromBody(final String body) {
        return getExtractedText(body, MIMETYPE_HTML);
    }

    public static String getExtractedText(final String text, final String mimeType) {
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

    protected String getContentViewUrl(final Content content, final String confluenceHome) {
        final String id = content.getId();
        final String type = content.getType();
        final Space space = content.getSpace();
        final String spaceKey = space.getKey();
        return confluenceHome + "/spaces/" + spaceKey + "/" + (type.equals("blogpost") ? "blog" : "page") + "/" + id;
    }

}
