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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClientBuilder;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
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
        final long readInterval = getReadInterval(paramMap);

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

        // get issues
        List<Map<String, Object>> issues = client.search().execute().getIssues();

        // store issues
        for (Map<String, Object> issue : issues) {
            processIssue(dataConfig, callback, paramMap, scriptMap, defaultDataMap, readInterval, jiraHome, issue);
        }

    }

    @SuppressWarnings("unchecked")
    protected void processIssue(final DataConfig dataConfig, final IndexUpdateCallback callback,
            final Map<String, String> paramMap, final Map<String, String> scriptMap,
            final Map<String, Object> defaultDataMap, final long readInterval, final String jiraHome,
            final Map<String, Object> issue) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final Map<String, Object> dataMap = new HashMap<>();
        dataMap.putAll(defaultDataMap);

        try {
            final String key = (String) issue.get("key");
            dataMap.put(fessConfig.getIndexFieldUrl(), jiraHome + "/browse/" + key);
            dataMap.put(fessConfig.getIndexFieldTitle(), issue.getOrDefault("summary", ""));

            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            String content = (String) fields.get("description");
            final Map<String, Object> commentObj = (Map<String, Object>) fields.get("comment");
            final List<Map<String, Object>> comments = (List<Map<String, Object>>) commentObj.get("comments");
            for (Map<String, Object> comment : comments) {
                content += "\n\n" + comment.get("body");
            }
            dataMap.put(fessConfig.getIndexFieldContent(), content);
            try {
                Date lastModified = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ")
                        .parse((String) fields.get("updated"));
                dataMap.put(fessConfig.getIndexFieldLastModified(), lastModified);
            } catch (final ParseException e) {
                logger.warn("Parse Exception", e);
            }
            callback.store(paramMap, dataMap);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : " + dataMap, e);
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
