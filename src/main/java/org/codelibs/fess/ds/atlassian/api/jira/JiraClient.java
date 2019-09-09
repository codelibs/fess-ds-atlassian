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
package org.codelibs.fess.ds.atlassian.api.jira;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import com.google.api.client.http.HttpRequestFactory;

import com.google.api.client.http.apache.ApacheHttpTransport;
import org.apache.http.auth.AUTH;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetIssueRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JiraClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(JiraClient.class);

    protected static final int ISSUE_MAX_RESULTS = 50;

    // parameters
    protected static final String HOME_PARAM = "home";
    protected static final String AUTH_TYPE_PARAM = "auth_type";
    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";
    protected static final String USERNAME_PARAM = "basic.username";
    protected static final String PASSWORD_PARAM = "basic.password";
    protected static final String JQL_PARAM = "issue.jql";

    // values for parameters
    protected static final String BASIC = "basic";
    protected static final String OAUTH = "oauth";

    protected AtlassianClient atlassianClient;

    protected final String jiraHome;
    protected final String userName;
    protected final String password;
    protected final String consumerKey;
    protected final String privateKey;
    protected final String verifier;
    protected final String temporaryToken ;
    protected final long readInterval;
    protected final String jql;

    public JiraClient(final Map<String, String> paramMap) {

        jiraHome = getJiraHome(paramMap);
        userName = getUserName(paramMap);
        password = getPassword(paramMap);
        consumerKey = getConsumerKey(paramMap);
        privateKey = getPrivateKey(paramMap);
        verifier = getSecret(paramMap);
        temporaryToken = getAccessToken(paramMap);
        readInterval = getReadInterval(paramMap);
        jql = getJql(paramMap);

        if (jiraHome.isEmpty()) {
            logger.warn("parameter \"" + HOME_PARAM + "\" required");
            return;
        }

        final String authType = getAuthType(paramMap);
        switch (authType) {
            case BASIC: {
                if (userName.isEmpty() || password.isEmpty()) {
                    throw new AtlassianDataStoreException("parameter \"" + USERNAME_PARAM + "\" and \"" + PASSWORD_PARAM + " required for Basic authentication.");
                }
                atlassianClient = AtlassianClient.builder().basicAuth(jiraHome, userName, password).build();
                break;
            }
            case OAUTH: {
                if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || temporaryToken.isEmpty()) {
                    throw new AtlassianDataStoreException("parameter \"" + CONSUMER_KEY_PARAM + "\", \""
                            + PRIVATE_KEY_PARAM + "\", \"" + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" required for OAuth authentication.");
                }
                atlassianClient = AtlassianClient.builder().oAuthToken(jiraHome, accessToken -> {
                    accessToken.consumerKey = consumerKey;
                    accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner(privateKey);
                    accessToken.transport = new ApacheHttpTransport();
                    accessToken.verifier = verifier;
                    accessToken.temporaryToken = temporaryToken;
                }).build();
                break;
            }
            default: {
                throw new AtlassianDataStoreException(AUTH_TYPE_PARAM + " is empty or invalid.");
            }
        }

    }

    @Override
    public void close() {
        // TODO
    }

    protected String getJiraHome(final Map<String, String> paramMap) {
        if (paramMap.containsKey(HOME_PARAM)) {
            return paramMap.get(HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    public String getJiraHome() {
        return jiraHome;
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

    protected long getReadInterval(final Map<String, String> paramMap) {
        long readInterval = 0;
        final String value = paramMap.get("readInterval");
        if (StringUtil.isNotBlank(value)) {
            try {
                readInterval = Long.parseLong(value);
            } catch (final NumberFormatException e) {
                logger.warn("Invalid read interval: " + value);
            }
        }
        return readInterval;
    }

    protected String getAuthType(final Map<String, String> paramMap) {
        if (paramMap.containsKey(AUTH_TYPE_PARAM)) {
            return paramMap.get(AUTH_TYPE_PARAM);
        }
        return StringUtil.EMPTY;
    }


    public String jiraHome() {
        return atlassianClient.appHome();
    }

    public HttpRequestFactory request() {
        return atlassianClient.request();
    }

    public GetProjectsRequest getProjects() {
        return new GetProjectsRequest(this);
    }

    public GetProjectRequest getProject(String projectIdOrKey) {
        return new GetProjectRequest(this, projectIdOrKey);
    }

    public SearchRequest search() {
        return new SearchRequest(this);
    }

    public GetIssueRequest getIssue(String issueIdOrKey) {
        return new GetIssueRequest(this, issueIdOrKey);
    }

    public GetCommentsRequest getComments(String issueIdOrKey) {
        return new GetCommentsRequest(this, issueIdOrKey);
    }

    public void getIssues(final Consumer<Issue> consumer) {
        for (int startAt = 0; ; startAt += ISSUE_MAX_RESULTS) {
            final SearchResponse searchResponse = search().jql(jql).startAt(startAt).maxResults(ISSUE_MAX_RESULTS)
                    .fields("summary", "description", "updated").execute();
            searchResponse.getIssues().forEach(consumer);
            if (searchResponse.getTotal() < ISSUE_MAX_RESULTS) {
                break;
            }
        }
    }

    public void getComments(String issueId, final Consumer<Comment> consumer) {
        for (int startAt = 0;; startAt += ISSUE_MAX_RESULTS) {
            final GetCommentsResponse getCommentsResponse = getComments(issueId).startAt(startAt).maxResults(ISSUE_MAX_RESULTS).execute();
            final List<Comment> comments = getCommentsResponse.getComments();
            comments.forEach(consumer);
            if (comments.size() < ISSUE_MAX_RESULTS)
                break;
        }
    }

}