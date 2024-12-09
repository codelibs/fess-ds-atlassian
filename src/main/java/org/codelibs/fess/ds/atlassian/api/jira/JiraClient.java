/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsAction;
import org.codelibs.fess.helper.CrawlerStatsHelper.StatsKeyObject;
import org.codelibs.fess.util.ComponentUtil;

public class JiraClient extends AtlassianClient implements Closeable {

    protected static final String DEFAULT_ISSUE_MAX_RESULTS = "50";

    // parameters for Jira
    protected static final String JQL_PARAM = "issue.jql";
    protected static final String ISSUE_MAX_RESULTS_PARAM = "issue_max_results";

    protected final String jiraHome;
    protected final String jql;
    protected final Integer issueMaxResults;

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

    protected String getJql(final DataStoreParams paramMap) {
        return paramMap.getAsString(JQL_PARAM);
    }

    protected Integer getIssueMaxResults(final DataStoreParams paramMap) {
        return Integer.parseInt(paramMap.getAsString(ISSUE_MAX_RESULTS_PARAM, DEFAULT_ISSUE_MAX_RESULTS));
    }

    public String getJiraHome() {
        return jiraHome;
    }

    public GetProjectsRequest projects() {
        return createRequest(new GetProjectsRequest());
    }

    public GetProjectRequest project(final String projectIdOrKey) {
        return createRequest(new GetProjectRequest(projectIdOrKey));
    }

    public SearchRequest search() {
        return createRequest(new SearchRequest());
    }

    public GetIssueRequest issue(final String issueIdOrKey) {
        return createRequest(new GetIssueRequest(issueIdOrKey));
    }

    public GetCommentsRequest comments(final String issueIdOrKey) {
        return createRequest(new GetCommentsRequest(issueIdOrKey));
    }

    @Override
    protected String getAppHome() {
        return jiraHome;
    }

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
