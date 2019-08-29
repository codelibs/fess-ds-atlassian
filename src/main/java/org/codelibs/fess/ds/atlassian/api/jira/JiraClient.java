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

import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetIssueRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;

import java.util.List;
import java.util.function.Consumer;

public class JiraClient {

    protected static final int ISSUE_MAX_RESULTS = 50;

    protected AtlassianClient client;

    public JiraClient(final AtlassianClient client) {
        this.client = client;
    }

    public String jiraHome() {
        return client.appHome();
    }

    public HttpRequestFactory request() {
        return client.request();
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

    public void getIssues(final String jql, final Consumer<Issue> consumer) {
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