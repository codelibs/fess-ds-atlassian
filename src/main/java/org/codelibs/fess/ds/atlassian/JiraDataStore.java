/*
 * Copyright 2012-2018 CodeLibs Project and the Others.
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
import java.util.Map;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.exception.MultipleCrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClientBuilder;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraDataStore extends AbstractDataStore {
    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    protected static final String JIRA_HOME_PARAM = "jira_home";
    protected static final String CONSUMER_KEY_PARAM = "consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "private_key";
    protected static final String SECRET_PARAM = "secret";
    protected static final String ACCESS_TOKEN_PARAM = "access_token";

    protected String getName() {
        return "JiraDataStore";
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback,
            final Map<String, String> paramMap, final Map<String, String> scriptMap,
            final Map<String, Object> defaultDataMap) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();

        final String jiraHome = getJiraHome(paramMap);
        final String consumerKey = getConsumerKey(paramMap);
        final String privateKey = getPrivateKey(paramMap);
        final String verifier = getSecret(paramMap);
        final String temporaryToken = getAccessToken(paramMap);

        if (jiraHome.isEmpty() || consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty()
                || temporaryToken.isEmpty()) {
            logger.warn("parameter \"" + JIRA_HOME_PARAM + "\" and \"" + CONSUMER_KEY_PARAM + "\" and \""
                    + PRIVATE_KEY_PARAM + "\" and \"" + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM
                    + "\" are required");
            return;
        }

        final JiraClient client = JiraClient.builder().oAuthToken(jiraHome, accessToken -> {
            accessToken.consumerKey = consumerKey;
            accessToken.signer = JiraClientBuilder.getOAuthRsaSigner(privateKey);
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = verifier;
            accessToken.temporaryToken = temporaryToken;
        }).build();

        final long readInterval = getReadInterval(paramMap);
        final int dataSize = paramMap.get("data.size") != null ? Integer.parseInt(paramMap.get("data.size")) : 10;
        boolean running = true;
        for (int i = 0; i < dataSize && running; i++) {
            final Map<String, Object> dataMap = new HashMap<>();
            try {
                dataMap.put(fessConfig.getIndexFieldUrl(), "http://fess.codelibs.org/?sample=" + i);
                dataMap.put(fessConfig.getIndexFieldHost(), "fess.codelibs.org");
                dataMap.put(fessConfig.getIndexFieldSite(), "fess.codelibs.org/" + i);
                dataMap.put(fessConfig.getIndexFieldTitle(), "Sample " + i);
                dataMap.put(fessConfig.getIndexFieldContent(), "Sample Test" + i);
                dataMap.put(fessConfig.getIndexFieldDigest(), "Sample Data" + i);
                dataMap.put(fessConfig.getIndexFieldAnchor(), "http://fess.codelibs.org/?from=" + i);
                dataMap.put(fessConfig.getIndexFieldContentLength(), i * 100L);
                dataMap.put(fessConfig.getIndexFieldLastModified(), new Date());
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

                String url;
                if (target instanceof DataStoreCrawlingException) {
                    final DataStoreCrawlingException dce = (DataStoreCrawlingException) target;
                    url = dce.getUrl();
                    if (dce.aborted()) {
                        running = false;
                    }
                } else {
                    url = "line:" + i;
                }
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, errorName, url, target);
            } catch (final Throwable t) {
                logger.warn("Crawling Access Exception at : " + dataMap, t);
                final String url = "line:" + i;
                final FailureUrlService failureUrlService = ComponentUtil.getComponent(FailureUrlService.class);
                failureUrlService.store(dataConfig, t.getClass().getCanonicalName(), url, t);

                if (readInterval > 0) {
                    sleep(readInterval);
                }

            }
        }
    }

    protected String getJiraHome(Map<String, String> paramMap) {
        if (paramMap.containsKey(JIRA_HOME_PARAM)) {
            return paramMap.get(JIRA_HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getConsumerKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONSUMER_KEY_PARAM)) {
            return paramMap.get(CONSUMER_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPrivateKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(PRIVATE_KEY_PARAM)) {
            return paramMap.get(PRIVATE_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getSecret(Map<String, String> paramMap) {
        if (paramMap.containsKey(SECRET_PARAM)) {
            return paramMap.get(SECRET_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAccessToken(Map<String, String> paramMap) {
        if (paramMap.containsKey(ACCESS_TOKEN_PARAM)) {
            return paramMap.get(ACCESS_TOKEN_PARAM);
        }
        return StringUtil.EMPTY;
    }

}
