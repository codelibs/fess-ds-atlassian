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
package org.codelibs.fess.ds.atlassian.api.jira;

import java.io.Closeable;
import java.util.List;
import java.util.function.Consumer;

import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianProduct;
import org.codelibs.fess.ds.atlassian.api.Deployment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.codelibs.fess.ds.atlassian.api.paging.PageCursor;
import org.codelibs.fess.ds.atlassian.api.paging.Paginator;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;

/**
 * JIRA API client for accessing JIRA projects, issues, and comments.
 * Provides high-level methods for interacting with JIRA REST API.
 */
public class JiraClient extends AtlassianClient implements Closeable {

    /** Default maximum number of issues to retrieve per request. */
    protected static final String DEFAULT_ISSUE_MAX_RESULTS = "50";

    /** Default JQL matching every issue. */
    protected static final String DEFAULT_JQL = "created is not empty";

    // parameters for Jira
    /** Parameter key for the JQL query. */
    protected static final String JQL_PARAM = "jira.jql";

    /**
     * Deprecated parameter key for the JQL query.
     *
     * @deprecated Use {@link #JQL_PARAM} instead. Will be removed in 16.0.
     */
    @Deprecated
    protected static final String DEPRECATED_JQL_PARAM = "issue.jql";

    /** Parameter key for the search page size. */
    protected static final String ISSUE_MAX_RESULTS_PARAM = "jira.max_results";

    /**
     * Deprecated parameter key for the search page size.
     *
     * @deprecated Use {@link #ISSUE_MAX_RESULTS_PARAM} instead. Will be removed in 16.0.
     */
    @Deprecated
    protected static final String DEPRECATED_ISSUE_MAX_RESULTS_PARAM = "issue_max_results";

    /** The JIRA instance home URL. */
    protected final String jiraHome;

    /** The JIRA api URL **/
    protected final String jiraApiUrl;

    /** The JQL query for filtering issues. */
    protected final String jql;

    /** The maximum number of issues to retrieve per request. */
    protected final Integer issueMaxResults;

    /**
     * Constructs a new JIRA client with the specified parameters.
     *
     * @param dataConfig the data configuration
     * @param paramMap the configuration parameters
     */
    public JiraClient(final DataConfig dataConfig, final DataStoreParams paramMap) {
        super(dataConfig, paramMap, AtlassianProduct.JIRA);
        jiraHome = getHome();
        jiraApiUrl = getApiUrl();
        jql = getJql(paramMap);
        issueMaxResults = getIssueMaxResults(paramMap);
    }

    @Override
    public void close() {
        // TODO
    }

    /**
     * Gets the JQL query from parameters.
     *
     * @param paramMap the parameter map
     * @return the JQL query
     */
    public String getJql(final DataStoreParams paramMap) {
        return getParamWithDeprecatedAlias(paramMap, JQL_PARAM, DEPRECATED_JQL_PARAM, DEFAULT_JQL);
    }

    /**
     * Gets the search page size from parameters.
     *
     * @param paramMap the parameter map
     * @return the page size
     */
    public Integer getIssueMaxResults(final DataStoreParams paramMap) {
        return Integer.valueOf(getParamWithDeprecatedAlias(paramMap, ISSUE_MAX_RESULTS_PARAM, DEPRECATED_ISSUE_MAX_RESULTS_PARAM,
                DEFAULT_ISSUE_MAX_RESULTS));
    }

    /**
     * Returns the resolved JQL query.
     *
     * @return the JQL query
     */
    public String getJql() {
        return jql;
    }

    /**
     * Returns the resolved search page size.
     *
     * @return the page size
     */
    public Integer getIssueMaxResults() {
        return issueMaxResults;
    }

    /**
     * Gets the JIRA home URL.
     *
     * @return the JIRA home URL
     */
    public String getJiraHome() {
        return jiraHome;
    }

    /**
     * Creates a request to get all projects.
     *
     * @return a GetProjectsRequest instance
     */
    public GetProjectsRequest projects() {
        return createRequest(new GetProjectsRequest());
    }

    /**
     * Creates a search request for issues.
     *
     * @return a SearchRequest instance
     */
    public SearchRequest search() {
        return createRequest(new SearchRequest());
    }

    /**
     * Creates a request to get comments for a specific issue.
     *
     * @param issueIdOrKey the issue ID or key
     * @return a GetCommentsRequest instance
     */
    public GetCommentsRequest comments(final String issueIdOrKey) {
        return createRequest(new GetCommentsRequest(issueIdOrKey));
    }

    @Override
    protected String getAppHome() {
        return jiraHome;
    }

    @Override
    protected String getAppApiUrl() {
        return jiraApiUrl;
    }

    /**
     * Retrieves all issues using pagination and passes them to the consumer.
     * Uses the configured JQL query to filter issues.
     *
     * @param consumer the consumer to process each issue
     */
    public void getIssues(final Consumer<Issue> consumer) {
        final boolean cloud = getEndpointStrategy().getDeployment() == Deployment.CLOUD;
        Paginator.forEach("Jira issues", cursor -> {
            final SearchRequest request = search().jql(jql).maxResults(issueMaxResults).fields("summary", "description", "updated");
            if (cloud) {
                if (cursor.token() != null) {
                    request.nextPageToken(cursor.token());
                }
            } else {
                request.startAt(cursor.offset() == null ? 0 : cursor.offset().intValue());
            }

            final SearchResponse response = request.execute();
            final List<Issue> issues = response.getIssues() == null ? List.of() : response.getIssues();

            if (cloud) {
                final String token = response.getNextPageToken();
                final boolean last = Boolean.TRUE.equals(response.getIsLast()) || token == null;
                return new Paginator.Page<>(issues, last ? PageCursor.done() : PageCursor.token(token));
            }

            final int offset = cursor.offset() == null ? 0 : cursor.offset().intValue();
            if (Paginator.isOffsetIgnored("Jira issues", request.getURL(), offset, response.getStartAt())) {
                return new Paginator.Page<>(issues, PageCursor.done());
            }

            final Long total = response.getTotal();
            final boolean last = total != null ? offset + issues.size() >= total.longValue() : issues.size() < issueMaxResults.intValue();
            return new Paginator.Page<>(issues, last ? PageCursor.done() : PageCursor.offset(offset + issues.size()));
        }, consumer);
    }

    /**
     * Retrieves all comments for a specific issue using pagination and passes them to the consumer.
     *
     * @param issueId the issue ID
     * @param consumer the consumer to process each comment
     */
    public void getComments(final String issueId, final Consumer<Comment> consumer) {
        final String description = "Jira comments of " + issueId;
        Paginator.forEach(description, cursor -> {
            final int offset = cursor.offset() == null ? 0 : cursor.offset().intValue();
            final GetCommentsRequest request = comments(issueId).startAt(offset).maxResults(issueMaxResults);
            final GetCommentsResponse response = request.execute();
            final List<Comment> comments = response.getComments() == null ? List.of() : response.getComments();

            if (Paginator.isOffsetIgnored(description, request.getURL(), offset, response.getStartAt())) {
                return new Paginator.Page<>(comments, PageCursor.done());
            }

            final Long total = response.getTotal();
            final boolean last =
                    total != null ? offset + comments.size() >= total.longValue() : comments.size() < issueMaxResults.intValue();
            return new Paginator.Page<>(comments, last ? PageCursor.done() : PageCursor.offset(offset + comments.size()));
        }, consumer);
    }
}
