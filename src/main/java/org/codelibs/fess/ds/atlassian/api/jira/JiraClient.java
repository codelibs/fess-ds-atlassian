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
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetIssueRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.codelibs.fess.entity.DataStoreParams;

/**
 * JIRA API client for accessing JIRA projects, issues, and comments.
 * Provides high-level methods for interacting with JIRA REST API.
 */
public class JiraClient extends AtlassianClient implements Closeable {

    /** Default maximum number of issues to retrieve per request. */
    protected static final String DEFAULT_ISSUE_MAX_RESULTS = "50";

    // parameters for Jira
    /** Parameter key for JQL query configuration. */
    protected static final String JQL_PARAM = "issue.jql";

    /** Parameter key for issue max results configuration. */
    protected static final String ISSUE_MAX_RESULTS_PARAM = "issue_max_results";

    /** The JIRA instance home URL. */
    protected final String jiraHome;

    /** The JQL query for filtering issues. */
    protected final String jql;

    /** The maximum number of issues to retrieve per request. */
    protected final Integer issueMaxResults;

    /**
     * Constructs a new JIRA client with the specified parameters.
     *
     * @param paramMap the configuration parameters
     */
    public JiraClient(final DataStoreParams paramMap) {
        super(paramMap);
        jiraHome = getHome(paramMap);
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
    protected String getJql(final DataStoreParams paramMap) {
        return paramMap.getAsString(JQL_PARAM);
    }

    /**
     * Gets the issue max results from parameters.
     *
     * @param paramMap the parameter map
     * @return the issue max results
     */
    protected Integer getIssueMaxResults(final DataStoreParams paramMap) {
        return Integer.parseInt(paramMap.getAsString(ISSUE_MAX_RESULTS_PARAM, DEFAULT_ISSUE_MAX_RESULTS));
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
     * Creates a request to get a specific project.
     *
     * @param projectIdOrKey the project ID or key
     * @return a GetProjectRequest instance
     */
    public GetProjectRequest project(final String projectIdOrKey) {
        return createRequest(new GetProjectRequest(projectIdOrKey));
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
     * Creates a request to get a specific issue.
     *
     * @param issueIdOrKey the issue ID or key
     * @return a GetIssueRequest instance
     */
    public GetIssueRequest issue(final String issueIdOrKey) {
        return createRequest(new GetIssueRequest(issueIdOrKey));
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

    /**
     * Retrieves all issues using pagination and passes them to the consumer.
     * Uses the configured JQL query to filter issues.
     *
     * @param consumer the consumer to process each issue
     */
    public void getIssues(final Consumer<Issue> consumer) {
        int startAt = 0;
        while (true) {
            final SearchResponse searchResponse =
                    search().jql(jql).startAt(startAt).maxResults(issueMaxResults).fields("summary", "description", "updated").execute();
            searchResponse.getIssues().forEach(consumer);
            if (searchResponse.getTotal() < issueMaxResults) {
                break;
            }
            startAt += issueMaxResults;
        }
    }

    /**
     * Retrieves all comments for a specific issue using pagination and passes them to the consumer.
     *
     * @param issueId the issue ID
     * @param consumer the consumer to process each comment
     */
    public void getComments(final String issueId, final Consumer<Comment> consumer) {
        int startAt = 0;
        while (true) {
            final GetCommentsResponse getCommentsResponse = comments(issueId).startAt(startAt).maxResults(issueMaxResults).execute();
            final List<Comment> comments = getCommentsResponse.getComments();
            comments.forEach(consumer);
            if (comments.size() < issueMaxResults) {
                break;
            }
            startAt += issueMaxResults;
        }
    }
}
