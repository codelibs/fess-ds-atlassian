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

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class JiraPaginationTest extends UnitDsTestCase {

    private static DataStoreParams params(final String home, final String deployment) {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", home);
        p.put("deployment", deployment);
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        p.put("jira.max_results", "2");
        return p;
    }

    private static String issuesJson(final String... keys) {
        final StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append("{\"id\":\"")
                    .append(i)
                    .append("\",\"key\":\"")
                    .append(keys[i])
                    .append("\",\"fields\":{\"summary\":\"S-")
                    .append(keys[i])
                    .append("\"}}");
        }
        return buf.append(']').toString();
    }

    @Test
    public void test_cloud_follows_next_page_token() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> {
                final String token = req.query().get("nextPageToken");
                if (token == null) {
                    return MockAtlassianServer.json("{\"issues\":" + issuesJson("A-1", "A-2") + ",\"nextPageToken\":\"tok2\"}");
                }
                if ("tok2".equals(token)) {
                    return MockAtlassianServer.json("{\"issues\":" + issuesJson("A-3") + ",\"isLast\":true}");
                }
                return MockAtlassianServer.status(400, "{\"message\":\"bad token\"}");
            });

            final List<String> keys = new ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(List.of("A-1", "A-2", "A-3"), keys);
            Assertions.assertEquals(2, server.getRequests().size());
            Assertions.assertNull(server.getRequests().get(0).query().get("startAt"),
                    "startAt must not be sent to the enhanced JQL endpoint");
        }
    }

    /**
     * Regression guard for the reported infinite loop: the endpoint returns neither
     * {@code total} nor {@code nextPageToken}, so a full page must not imply another page.
     */
    @Test
    public void test_cloud_stops_when_no_token_is_returned() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer.json("{\"issues\":" + issuesJson("A-1", "A-2") + "}"));

            final List<String> keys = new ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(List.of("A-1", "A-2"), keys);
            Assertions.assertEquals(1, server.getRequests().size(), "a full page without a token must end the loop");
        }
    }

    /**
     * Regression guard for a server that ignores the paging parameter entirely.
     */
    @Test
    public void test_cloud_stops_when_token_never_advances() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql",
                    req -> MockAtlassianServer.json("{\"issues\":" + issuesJson("A-1", "A-2") + ",\"nextPageToken\":\"stuck\"}"));

            final List<String> keys = new ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(2, server.getRequests().size(), "must stop on the second identical token");
            Assertions.assertEquals(4, keys.size());
        }
    }

    @Test
    public void test_datacenter_uses_api_v2_and_start_at() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/2/search", req -> {
                final String startAt = req.query().getOrDefault("startAt", "0");
                if ("0".equals(startAt)) {
                    return MockAtlassianServer
                            .json("{\"startAt\":0,\"maxResults\":2,\"total\":3,\"issues\":" + issuesJson("D-1", "D-2") + "}");
                }
                return MockAtlassianServer.json("{\"startAt\":2,\"maxResults\":2,\"total\":3,\"issues\":" + issuesJson("D-3") + "}");
            });

            final List<String> keys = new ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "datacenter"))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(List.of("D-1", "D-2", "D-3"), keys);
            Assertions.assertEquals(2, server.getRequests().size());
        }
    }

    /**
     * Regression guard for the 2020 forum report: the DC server ignores startAt and
     * returns an empty page while still reporting a large total.
     */
    @Test
    public void test_datacenter_stops_when_server_ignores_start_at() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/2/search", req -> {
                final String startAt = req.query().getOrDefault("startAt", "0");
                if ("0".equals(startAt)) {
                    return MockAtlassianServer
                            .json("{\"startAt\":0,\"maxResults\":2,\"total\":15404,\"issues\":" + issuesJson("D-1", "D-2") + "}");
                }
                return MockAtlassianServer.json("{\"startAt\":0,\"maxResults\":2,\"total\":15404,\"issues\":[]}");
            });

            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "datacenter"))) {
                client.getIssues(issue -> {});
            }

            Assertions.assertEquals(2, server.getRequests().size(), "an empty page must end the loop even when total is large");
        }
    }

    @Test
    public void test_comments_use_api_base_of_deployment() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/2/issue/1/comment", req -> MockAtlassianServer
                    .json("{\"startAt\":0,\"maxResults\":2,\"total\":1,\"comments\":[{\"id\":\"c1\",\"body\":\"B\"}]}"));

            final List<String> bodies = new ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "datacenter"))) {
                client.getComments("1", comment -> bodies.add(String.valueOf(comment.getBody())));
            }

            Assertions.assertEquals(1, bodies.size());
            Assertions.assertEquals("/rest/api/2/issue/1/comment", server.getRequests().get(0).path());
        }
    }

    /**
     * Regression guard for the 2020 forum report: the server ignores startAt and
     * returns an empty page while still reporting a large total.
     */
    @Test
    public void test_comments_stop_when_server_ignores_start_at() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/issue/1/comment", req -> {
                final String startAt = req.query().getOrDefault("startAt", "0");
                if ("0".equals(startAt)) {
                    return MockAtlassianServer.json("{\"startAt\":0,\"maxResults\":2,\"total\":999,\"comments\":"
                            + "[{\"id\":\"c1\",\"body\":\"B1\"},{\"id\":\"c2\",\"body\":\"B2\"}]}");
                }
                return MockAtlassianServer.json("{\"startAt\":0,\"maxResults\":2,\"total\":999,\"comments\":[]}");
            });

            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getComments("1", comment -> {});
            }

            Assertions.assertEquals(2, server.getRequests().size(), "an empty page must end the loop even when total is large");
        }
    }
}
