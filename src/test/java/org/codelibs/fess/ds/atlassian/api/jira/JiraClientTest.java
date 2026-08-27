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

import java.util.List;
import java.util.Map;

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
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JiraClientTest extends AtlassianClientTest {

    @Test
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
        Assertions.assertEquals(2, projects.size());
        for (int i = 0; i < projects.size(); i++) {
            final Project project = projects.get(i);
            Assertions.assertEquals("Project-" + i, project.getName());
        }
    }

    @Test
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
        Assertions.assertEquals(2, issues.size());
        for (int i = 0; i < issues.size(); i++) {
            final Issue issue = issues.get(i);
            assertTrue(issue.getKey().startsWith("Key-"));
            final Fields fields = issue.getFields();
            assertTrue(fields.getSummary().startsWith("Summary-"));
            final long totalComments = fields.getComment().getTotal();
            final List<Comment> comments = fields.getComment().getComments();
            Assertions.assertEquals(totalComments, (long) comments.size());
            for (int j = 0; j < comments.size(); j++) {
                final Comment comment = comments.get(j);
                Assertions.assertEquals("Comment-" + i + "-" + j, comment.getBody());
            }
        }
    }

    @Test
    public void test_getComments_parseResponse() {
        final String json = "{" + //
                "  \"total\": 2," + //
                "  \"comments\": [" + //
                "    { \"body\": \"Comment-0\" }," + //
                "    { \"body\": \"Comment-1\" }" + //
                "  ]" + //
                "}";
        final GetCommentsResponse response = GetCommentsRequest.parseResponse(json);
        final List<Comment> comments = response.getComments();
        Assertions.assertEquals(2, comments.size());
        // The reported total is what lets an exactly-full page end the crawl without a
        // pointless extra request, so the parser must carry it onto the response.
        Assertions.assertEquals(Long.valueOf(2L), response.getTotal());
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);
            Assertions.assertEquals("Comment-" + i, comment.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_search_parseResponse_withAdfDescription() {
        final String json = "{" + //
                "  \"total\": 1," + //
                "  \"issues\": [{" + //
                "      \"fields\": {" + //
                "        \"summary\": \"Task 1\"," + //
                "        \"description\": {" + //
                "          \"type\": \"doc\"," + //
                "          \"version\": 1," + //
                "          \"content\": [{" + //
                "            \"type\": \"paragraph\"," + //
                "            \"content\": [{" + //
                "              \"type\": \"text\"," + //
                "              \"text\": \"This is a description.\"" + //
                "            }]" + //
                "          }]" + //
                "        }," + //
                "        \"updated\": \"2026-01-23T16:50:17.666+0900\"" + //
                "      }," + //
                "      \"key\": \"KAN-1\"" + //
                "    }" + //
                "  ]" + //
                "}";
        final SearchResponse response = SearchRequest.parseResponse(json);
        final List<Issue> issues = response.getIssues();
        assertEquals(1, issues.size());
        final Issue issue = issues.get(0);
        assertEquals("KAN-1", issue.getKey());
        final Fields fields = issue.getFields();
        assertEquals("Task 1", fields.getSummary());
        assertTrue("description should be a Map", fields.getDescription() instanceof Map);
        final Map<String, Object> adf = (Map<String, Object>) fields.getDescription();
        assertEquals("doc", adf.get("type"));
    }

    @Test
    public void test_search_parseResponse_withStringDescription() {
        final String json = "{" + //
                "  \"total\": 1," + //
                "  \"issues\": [{" + //
                "      \"fields\": {" + //
                "        \"summary\": \"Task 2\"," + //
                "        \"description\": \"Plain text description\"," + //
                "        \"updated\": \"2026-01-23T16:50:17.666+0900\"" + //
                "      }," + //
                "      \"key\": \"KAN-2\"" + //
                "    }" + //
                "  ]" + //
                "}";
        final SearchResponse response = SearchRequest.parseResponse(json);
        final List<Issue> issues = response.getIssues();
        assertEquals(1, issues.size());
        final Fields fields = issues.get(0).getFields();
        assertTrue("description should be a String", fields.getDescription() instanceof String);
        assertEquals("Plain text description", fields.getDescription());
    }

    @Test
    public void test_search_parseResponse_withNullDescription() {
        final String json = "{" + //
                "  \"total\": 1," + //
                "  \"issues\": [{" + //
                "      \"fields\": {" + //
                "        \"summary\": \"Task 3\"," + //
                "        \"updated\": \"2026-01-23T16:50:17.666+0900\"" + //
                "      }," + //
                "      \"key\": \"KAN-3\"" + //
                "    }" + //
                "  ]" + //
                "}";
        final SearchResponse response = SearchRequest.parseResponse(json);
        final List<Issue> issues = response.getIssues();
        assertEquals(1, issues.size());
        assertNull(issues.get(0).getFields().getDescription());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void test_getComments_parseResponse_withAdfBody() {
        final String json = "{" + //
                "  \"total\": 1," + //
                "  \"comments\": [{" + //
                "    \"body\": {" + //
                "      \"type\": \"doc\"," + //
                "      \"version\": 1," + //
                "      \"content\": [{" + //
                "        \"type\": \"paragraph\"," + //
                "        \"content\": [{" + //
                "          \"type\": \"text\"," + //
                "          \"text\": \"ADF comment\"" + //
                "        }]" + //
                "      }]" + //
                "    }" + //
                "  }]" + //
                "}";
        final GetCommentsResponse response = GetCommentsRequest.parseResponse(json);
        final List<Comment> comments = response.getComments();
        assertEquals(1, comments.size());
        assertTrue("body should be a Map", comments.get(0).getBody() instanceof Map);
        final Map<String, Object> adf = (Map<String, Object>) comments.get(0).getBody();
        assertEquals("doc", adf.get("type"));
    }

}
