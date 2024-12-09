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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.List;

import org.codelibs.fess.ds.atlassian.api.AtlassianClientTest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Fields;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Project;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.issue.GetCommentsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.codelibs.fess.entity.DataStoreParams;

public class JiraClientTest extends AtlassianClientTest {

    protected void doProductionTest() {

        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put(AUTH_TYPE_PARAM, "oauth");
        paramMap.put(CONSUMER_KEY_PARAM, "");
        paramMap.put(PRIVATE_KEY_PARAM, "");
        paramMap.put(SECRET_PARAM, "");
        paramMap.put(ACCESS_TOKEN_PARAM, "");
        final JiraClient jiraClient = new JiraClient(paramMap);
        doGetProjectsTest(jiraClient);
        doSearchTest(jiraClient);
        doGetCommentsTest(jiraClient);
    }

    protected void doGetProjectsTest(final JiraClient jiraClient) {
        final GetProjectsResponse response = jiraClient.projects().expand("description").execute();
        for (final Project project : response.getProjects()) {
            assertTrue("not contains \"name\"", project.getName() != null);
            assertTrue("not contains \"description\"", project.getDescription() != null);
        }
    }

    public void test_getProjects_parseResponse() {
        String json = "[{" + //
                "    \"name\": \"Project-0\"" + //
                "  }," + //
                "  {" + //
                "    \"name\": \"Project-1\"" + //
                "  }" + //
                "]";
        final GetProjectsResponse response = GetProjectsRequest.parseResponse(json);
        final List<Project> projects = response.getProjects();
        for (int i = 0; i < projects.size(); i++) {
            final Project project = projects.get(i);
            assertEquals(project.getName(), "Project-" + i);
        }
    }

    protected void doSearchTest(final JiraClient jiraClient) {
        final SearchResponse response = jiraClient.search().fields("summary", "description", "comment", "updated").execute();
        response.getIssues().forEach(issue -> {
            assertTrue(issue.getKey() != null);
            assertTrue("not contains \"fields\"", issue.getFields() != null);
            final Fields fields = issue.getFields();
            assertTrue("not contains \"summary\" in fields", fields.getSummary() != null);
            assertTrue("not contains \"description\" in fields", fields.getDescription() != null);
            assertTrue("not contains \"comment\" in fields", fields.getComment() != null);
            final long commentTotal = fields.getComment().getTotal();
            final List<Comment> comments = fields.getComment().getComments();
            assertEquals(comments.size(), commentTotal);
            for (final Comment comment : comments) {
                assertTrue("not contains \"body\" in comment", comment.getBody() != null);
            }
            assertTrue("not contains \"updated\" in fields", fields.getUpdated() != null);
            final String updated = fields.getUpdated();
            try {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(updated);
            } catch (final ParseException e) {
                assertTrue("failed to parse \"updated\": " + updated, false);
            }
        });
    }

    public void test_search_parseResponse() {
        final String json = "{" + //
                "  \"total\": 2," + //
                "  \"issues\": [{" + //
                "      \"fields\": {" + //
                "        \"summary\": \"Summary-0\"," + //
                "        \"comment\": {" + //
                "          \"total\": 2," + //
                "          \"comments\": [" + //
                "            { \"body\": \"Comment-0-0\" }," + //
                "            { \"body\": \"Comment-0-1\" }" + //
                "          ]" + //
                "        }" + //
                "      }," + //
                "      \"key\": \"Key-0\"" + //
                "    }," + //
                "    {" + //
                "      \"fields\": {" + //
                "        \"summary\": \"Summary-1\"," + //
                "        \"comment\": {" + //
                "          \"total\": 0," + //
                "          \"comments\": []" + //
                "        }" + //
                "      }," + //
                "      \"key\": \"Key-1\"" + //
                "    }" + //
                "  ]" + //
                "}";
        final SearchResponse response = SearchRequest.parseResponse(json);
        final List<Issue> issues = response.getIssues();
        for (int i = 0; i < issues.size(); i++) {
            final Issue issue = issues.get(i);
            assertTrue(issue.getKey().startsWith("Key-"));
            final Fields fields = issue.getFields();
            assertTrue(fields.getSummary().startsWith("Summary-"));
            final long totalComments = fields.getComment().getTotal();
            final List<Comment> comments = fields.getComment().getComments();
            assertEquals(comments.size(), totalComments);
            for (int j = 0; j < comments.size(); j++) {
                final Comment comment = comments.get(j);
                assertEquals(comment.getBody(), "Comment-" + i + "-" + j);
            }
        }
    }

    protected void doGetCommentsTest(final JiraClient jiraClient) {
        final List<Issue> issues = jiraClient.search().execute().getIssues();
        if (!issues.isEmpty()) {
            final String id = (String) issues.get(0).getId();
            final GetCommentsResponse response = jiraClient.comments(id).execute();
            for (final Comment comment : response.getComments()) {
                assertTrue("not contains \"body\"", comment.getBody() != null);
            }
        }
    }

    public void test_getComments_parseResponse() {
        final String json = "{" + //
                "  \"total\": 1," + //
                "  \"comments\": [" + //
                "    { \"body\": \"Comment-0\" }," + //
                "    { \"body\": \"Comment-1\" }" + //
                "  ]" + //
                "}";
        final GetCommentsResponse response = GetCommentsRequest.parseResponse(json);
        final List<Comment> comments = response.getComments();
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);
            assertEquals(comment.getBody(), "Comment-" + i);
        }
    }

}
