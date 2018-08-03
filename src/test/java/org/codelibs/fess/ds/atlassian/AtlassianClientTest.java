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
import java.util.List;
import java.util.Map;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesResponse;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsRequest;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchRequest;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastadi.ContainerTestCase;

public class AtlassianClientTest extends ContainerTestCase {

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_production() {
        // doProductionTest();
    }

    protected void doProductionTest() {
        final JiraClient jiraClient = new JiraClient(AtlassianClient.builder().oAuthToken("", accessToken -> {
            accessToken.consumerKey = "";
            accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner("");
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = "";
            accessToken.temporaryToken = "";
        }).build());

        final ConfluenceClient confluenceClient = new ConfluenceClient(AtlassianClient.builder().oAuthToken("", accessToken -> {
            accessToken.consumerKey = "";
            accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner("");
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = "";
            accessToken.temporaryToken = "";
        }).build());

        doGetProjectsTest(jiraClient);
        doSearchTest(jiraClient);
        doGetContentsTest(confluenceClient);
        doGetCommentsOfContentTest(confluenceClient);
        doGetSpacesTest(confluenceClient);
    }

    protected void doGetProjectsTest(final JiraClient jiraClient) {
        final GetProjectsResponse response = jiraClient.getProjects().expand("description").execute();
        for (final Map<String, Object> project : response.getProjects()) {
            assertTrue("not contains \"name\"", project.containsKey("name"));
            assertTrue("not contains \"description\"", project.containsKey("description"));
        }
    }

    public void test_getProjects_fromJson() {
        String json = "[{" + //
                "    \"name\": \"Project-0\"" + //
                "  }," + //
                "  {" + //
                "    \"name\": \"Project-1\"" + //
                "  }" + //
                "]";
        final GetProjectsResponse response = GetProjectsRequest.fromJson(json);
        final List<Map<String, Object>> projects = response.getProjects();
        for (int i = 0; i < projects.size(); i++) {
            final Map<String, Object> project = projects.get(i);
            assertEquals(project.get("name"), "Project-" + i);
        }
    }

    @SuppressWarnings("unchecked")
    protected void doSearchTest(final JiraClient jiraClient) {
        final SearchResponse response = jiraClient.search().fields("summary", "description", "comment", "updated").execute();
        for (final Map<String, Object> issue : response.getIssues()) {
            assertTrue(issue.containsKey("key"));
            assertTrue("not contains \"fields\"", issue.containsKey("fields"));
            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            assertTrue("not contains \"summary\" in fields", fields.containsKey("summary"));
            assertTrue("not contains \"description\" in fields", fields.containsKey("description"));
            assertTrue("not contains \"comment\" in fields", fields.containsKey("comment"));
            final Map<String, Object> commentObj = (Map<String, Object>) fields.get("comment");
            final int totalComments = (int) commentObj.get("total");
            final List<Map<String, Object>> comments = (List<Map<String, Object>>) commentObj.get("comments");
            assertEquals(comments.size(), totalComments);
            for (final Map<String, Object> comment : comments) {
                assertTrue("not contains \"body\" in comment", comment.containsKey("body"));
            }
            assertTrue("not contains \"updated\" in fields", fields.containsKey("updated"));
            final String updated = (String) fields.get("updated");
            try {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(updated);
            } catch (final ParseException e) {
                assertTrue("failed to parse \"updated\": " + updated, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void test_search_fromJson() {
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
        final SearchResponse response = SearchRequest.fromJson(json);
        final List<Map<String, Object>> issues = response.getIssues();
        for (int i = 0; i < issues.size(); i++) {
            final Map<String, Object> issue = issues.get(i);
            assertEquals(issue.get("key"), "Key-" + i);
            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            assertEquals(fields.get("summary"), "Summary-" + i);
            final Map<String, Object> commentObj = (Map<String, Object>) fields.get("comment");
            final int totalComments = (int) commentObj.get("total");
            final List<Map<String, Object>> comments = (List<Map<String, Object>>) commentObj.get("comments");
            assertEquals(comments.size(), totalComments);
            for (int j = 0; j < comments.size(); j++) {
                final Map<String, Object> comment = comments.get(j);
                assertEquals(comment.get("body"), "Comment-" + i + "-" + j);
            }
        }
    }

    @SuppressWarnings("unchecked")
    protected void doGetContentsTest(final ConfluenceClient confluenceClient) {
        final List<Map<String, Object>> contents = confluenceClient.getContents().expand("body.view", "version").execute().getContents();
        if (contents.size() > 0) {
            final Map<String, Object> content = contents.get(0);
            assertTrue("not contains \"title\"", content.containsKey("title"));

            assertTrue("not contains \"body\"", content.containsKey("body"));
            final Map<String, Object> body = (Map<String, Object>) content.get("body");
            assertTrue("not contains \"view\" in \"body\"", body.containsKey("view"));
            final Map<String, Object> view = (Map<String, Object>) body.get("view");
            assertTrue("not contains \"value\" in \"body.view\"", view.containsKey("value"));

            assertTrue("not contains \"version\"", content.containsKey("version"));
            final Map<String, Object> version = (Map<String, Object>) content.get("version");
            assertTrue("not contains \"when\" in \"version\"", version.containsKey("when"));
            final String when = (String) version.get("when");
            try {
                new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse(when);
            } catch (final ParseException e) {
                assertTrue("failed to parse \"when\": " + when, false);
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void test_getContents_fromJson() {
        final String json = "{" + //
                "  \"results\": [{" + //
                "      \"title\": \"Title-0\"," + //
                "      \"body\": { \"view\": { \"value\": \"Body-0\" } }," + //
                "      \"version\": { \"when\": \"2018-08-01T12:34:56.789Z\" }" + //
                "    }," + //
                "    {" + //
                "      \"title\": \"Title-1\"," + //
                "      \"body\": { \"view\": { \"value\": \"Body-1\" } }," + //
                "      \"version\": { \"when\": \"2018-08-01T12:34:56.789Z\" }" + //
                "    }" + //
                "  ]" + //
                "}";
        final GetContentsResponse response = GetContentsRequest.fromJson(json);
        final List<Map<String, Object>> contents = response.getContents();
        for (int i = 0; i < contents.size(); i++) {
            final Map<String, Object> content = contents.get(i);
            assertEquals(content.get("title"), "Title-" + i);
            final Map<String, Object> body = (Map<String, Object>) content.get("body");
            final Map<String, Object> view = (Map<String, Object>) body.get("view");
            assertEquals(view.get("value"), "Body-" + i);
            final Map<String, Object> version = (Map<String, Object>) content.get("version");
            assertEquals(version.get("when"), "2018-08-01T12:34:56.789Z");
        }
    }

    @SuppressWarnings("unchecked")
    protected void doGetCommentsOfContentTest(final ConfluenceClient confluenceClient) {
        final List<Map<String, Object>> contents = confluenceClient.getContents().execute().getContents();
        if (contents.size() > 0) {
            final String id = (String) contents.get(0).get("id");
            final GetCommentsOfContentResponse response =
                    confluenceClient.getCommentsOfContent(id).depth("all").expand("body.view").execute();
            for (final Map<String, Object> comment : response.getComments()) {
                assertTrue("not contains \"title\"", comment.containsKey("title"));
                assertTrue("not contains \"body\"", comment.containsKey("body"));
                final Map<String, Object> body = (Map<String, Object>) comment.get("body");
                assertTrue("not contains \"view\" in \"body\"", body.containsKey("view"));
                final Map<String, Object> view = (Map<String, Object>) body.get("view");
                assertTrue("not contains \"value\" in \"body.view\"", view.containsKey("value"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void test_getCommentsOfContent_fromJson() {
        String json = "{" + //
                "  \"results\": [" + //
                "    {" + //
                "      \"title\": \"Title-0\"," + //
                "      \"body\": { \"view\": { \"value\": \"<p>Comment-0</p>\" } }" + //
                "    }," + //
                "    {" + //
                "      \"title\": \"Title-1\"," + //
                "      \"body\": { \"view\": { \"value\": \"<p>Comment-1</p>\" } }" + //
                "    }" + //
                "  ]" + //
                "}";
        final GetCommentsOfContentResponse response = GetCommentsOfContentRequest.fromJson(json);
        final List<Map<String, Object>> comments = response.getComments();
        for (int i = 0; i < comments.size(); i++) {
            final Map<String, Object> comment = comments.get(i);
            assertEquals(comment.get("title"), "Title-" + i);
            final Map<String, Object> body = (Map<String, Object>) comment.get("body");
            final Map<String, Object> view = (Map<String, Object>) body.get("view");
            assertEquals(view.get("value"), "<p>Comment-" + i + "</p>");
        }
    }

    protected void doGetSpacesTest(final ConfluenceClient confluenceClient) {
        final GetSpacesResponse response = confluenceClient.getSpaces().expand("description").execute();
        for (final Map<String, Object> space : response.getSpaces()) {
            assertTrue("not contains \"name\"", space.containsKey("name"));
            assertTrue("not contains \"description\"", space.containsKey("description"));
        }
    }

    public void test_getSpaces_fromJson() {
        String json = "{" + //
                "  \"results\": [" + //
                "    { \"name\": \"Space-0\" }," + //
                "    { \"name\": \"Space-1\" }" + //
                "  ]" + //
                "}";
        final GetSpacesResponse response = GetSpacesRequest.fromJson(json);
        final List<Map<String, Object>> spaces = response.getSpaces();
        for (int i = 0; i < spaces.size(); i++) {
            final Map<String, Object> space = spaces.get(i);
            assertEquals(space.get("name"), "Space-" + i);
        }
    }

}
