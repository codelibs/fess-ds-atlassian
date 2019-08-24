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

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraDataStore extends AbstractDataStore {
    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    // parameters
    protected static final String HOME_PARAM = "home";

    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";

    protected static final String USERNAME_PARAM = "basicauth.username";
    protected static final String PASSWORD_PARAM = "basicauth.password";

    protected static final String JQL_PARAM = "issue.jql";

    // scripts
    protected static final String ISSUE = "issue";
    protected static final String ISSUE_SUMMARY = "summary";
    protected static final String ISSUE_DESCRIPTION = "description";
    protected static final String ISSUE_COMMENTS = "comments";
    protected static final String ISSUE_LAST_MODIFIED = "last_modified";
    protected static final String ISSUE_VIEW_URL = "view_url";

    protected static final int ISSUE_MAX_RESULTS = 50;

    protected String getName() {
        return this.getClass().getSimpleName();
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();

        final String jiraHome = getJiraHome(paramMap);

        final String userName = getUserName(paramMap);
        final String password = getPassword(paramMap);

        final String consumerKey = getConsumerKey(paramMap);
        final String privateKey = getPrivateKey(paramMap);
        final String verifier = getSecret(paramMap);
        final String temporaryToken = getAccessToken(paramMap);

        final long readInterval = getReadInterval(paramMap);

        final String jql = getJql(paramMap);

        boolean basic = false;
        if (jiraHome.isEmpty()) {
            logger.warn("parameter \"" + HOME_PARAM + "\" is required");
            return;
        } else if (!userName.isEmpty() && !password.isEmpty()) {
            basic = true;
        } else if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || temporaryToken.isEmpty()) {
            logger.warn("parameter \"" + USERNAME_PARAM + "\" and \"" + PASSWORD_PARAM + "\" or \"" + CONSUMER_KEY_PARAM + "\", \""
                    + PRIVATE_KEY_PARAM + "\", \"" + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" are required");
            return;
        }

        final JiraClient client = basic ? new JiraClient(AtlassianClient.builder().basicAuth(jiraHome, userName, password).build())
                : new JiraClient(AtlassianClient.builder().oAuthToken(jiraHome, accessToken -> {
            accessToken.consumerKey = consumerKey;
            accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner(privateKey);
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = verifier;
            accessToken.temporaryToken = temporaryToken;
        }).build());

        client.getIssues(jql, issue -> {
            processIssue(dataConfig, callback, paramMap, scriptMap, defaultDataMap, fessConfig, client, readInterval, jiraHome, issue);
        });
    }

    protected void processIssue(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap, final FessConfig fessConfig,
            final JiraClient client, final long readInterval, final String jiraHome, final Issue issue) {
        final Map<String, Object> dataMap = new HashMap<>();
        dataMap.putAll(defaultDataMap);
        final Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.putAll(paramMap);
        final Map<String, Object> issueMap = new HashMap<>();

        try {
            // TODO
            issueMap.put(ISSUE_SUMMARY, issue.getFields().getSummary());
            issueMap.put(ISSUE_DESCRIPTION, issue.getFields().getDescription());
            issueMap.put(ISSUE_COMMENTS, getIssueComments(issue, client));
            issueMap.put(ISSUE_LAST_MODIFIED, getIssueLastModified(issue));
            issueMap.put(ISSUE_VIEW_URL, getIssueViewUrl(issue, jiraHome));
            resultMap.put(ISSUE, issueMap);

            for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                final Object convertValue = convertValue(entry.getValue(), resultMap);
                if (convertValue != null) {
                    dataMap.put(entry.getKey(), convertValue);
                }
            }
            callback.store(paramMap, dataMap);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : " + dataMap, e);
        }
    }

    protected String getIssueViewUrl(final Issue issue, final String jiraHome) {
        return jiraHome + "/browse/" + issue.getKey();
    }

    protected String getIssueComments(final Issue issue, final JiraClient client) {
        final StringBuilder sb = new StringBuilder();
        final String id = issue.getId();

        for (int startAt = 0;; startAt += ISSUE_MAX_RESULTS) {
            final List<Comment> comments =
                    client.getComments(id).startAt(startAt).maxResults(ISSUE_MAX_RESULTS).execute().getComments();

            for (final Comment comment : comments) {
                sb.append("\n\n");
                sb.append(comment.getBody());
            }

            if (comments.size() < ISSUE_MAX_RESULTS)
                break;
        }
        return sb.toString();
    }

    protected Date getIssueLastModified(final Issue issue) {
        @SuppressWarnings("unchecked")
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

    protected String getJiraHome(final Map<String, String> paramMap) {
        if (paramMap.containsKey(HOME_PARAM)) {
            return paramMap.get(HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getUserName(final Map<String, String> paramMap) {
        if (paramMap.containsKey(USERNAME_PARAM)) {
            return paramMap.get(USERNAME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPassword(final Map<String, String> paramMap) {
        if (paramMap.containsKey(PASSWORD_PARAM)) {
            return paramMap.get(PASSWORD_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getConsumerKey(final Map<String, String> paramMap) {
        if (paramMap.containsKey(CONSUMER_KEY_PARAM)) {
            return paramMap.get(CONSUMER_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPrivateKey(final Map<String, String> paramMap) {
        if (paramMap.containsKey(PRIVATE_KEY_PARAM)) {
            return paramMap.get(PRIVATE_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getSecret(final Map<String, String> paramMap) {
        if (paramMap.containsKey(SECRET_PARAM)) {
            return paramMap.get(SECRET_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAccessToken(final Map<String, String> paramMap) {
        if (paramMap.containsKey(ACCESS_TOKEN_PARAM)) {
            return paramMap.get(ACCESS_TOKEN_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getJql(final Map<String, String> paramMap) {
        if (paramMap.containsKey(JQL_PARAM)) {
            return paramMap.get(JQL_PARAM);
        }
        return StringUtil.EMPTY;
    }

    public Issue parseIssueJson(final String content) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(content, Issue.class);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse: \"" + content + "\"", e);
        }
    }

}
