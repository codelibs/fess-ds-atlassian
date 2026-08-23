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
package org.codelibs.fess.ds.atlassian.api;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RateLimitRetryTest extends UnitDsTestCase {

    private static DataStoreParams params(final String home) {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", home);
        p.put("deployment", "cloud");
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        p.put("jira.max_results", "2");
        // Keep the suite fast: no read interval, and a Retry-After the server reports in whole
        // seconds is the only delay these tests can incur, so every fixture uses 0.
        p.put("read_interval", "0");
        return p;
    }

    private static String issuesJson(final String... keys) {
        final StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < keys.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append("{\"id\":\"").append(i).append("\",\"key\":\"").append(keys[i]).append("\",\"fields\":{\"summary\":\"S\"}}");
        }
        return buf.append(']').toString();
    }

    @Test
    public void test_retries_a_429_then_succeeds() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            final AtomicInteger calls = new AtomicInteger();
            server.on("/rest/api/3/search/jql", req -> {
                if (calls.incrementAndGet() == 1) {
                    return new MockAtlassianServer.MockResponse(429, Map.of("Content-Type", "application/json", "Retry-After", "0"),
                            "{\"message\":\"rate limited\"}");
                }
                return MockAtlassianServer.json("{\"issues\":" + issuesJson("A-1") + ",\"isLast\":true}");
            });

            final List<String> keys = new java.util.ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl()))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(List.of("A-1"), keys);
            Assertions.assertEquals(2, calls.get(), "the 429 must be retried exactly once before succeeding");
        }
    }

    @Test
    public void test_gives_up_after_the_retry_budget() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> new MockAtlassianServer.MockResponse(429,
                    Map.of("Content-Type", "application/json", "Retry-After", "0"), "{\"message\":\"rate limited\"}"));

            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl()))) {
                Assertions.assertThrows(AtlassianDataStoreException.class, () -> client.getIssues(issue -> {}));
            }

            Assertions.assertEquals(5, server.getRequests().size(), "one initial attempt plus four retries");
        }
    }

    @Test
    public void test_retries_a_500() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            final AtomicInteger calls = new AtomicInteger();
            server.on("/rest/api/3/search/jql", req -> {
                if (calls.incrementAndGet() == 1) {
                    return MockAtlassianServer.status(500, "{\"message\":\"boom\"}");
                }
                return MockAtlassianServer.json("{\"issues\":" + issuesJson("A-1") + ",\"isLast\":true}");
            });

            final List<String> keys = new java.util.ArrayList<>();
            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl()))) {
                client.getIssues(issue -> keys.add(issue.getKey()));
            }

            Assertions.assertEquals(List.of("A-1"), keys);
            Assertions.assertEquals(2, calls.get());
        }
    }

    /** A 4xx that is not 429 is the caller's fault; retrying it just wastes quota. */
    @Test
    public void test_does_not_retry_a_400() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer.status(400, "{\"message\":\"bad jql\"}"));

            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl()))) {
                Assertions.assertThrows(AtlassianDataStoreException.class, () -> client.getIssues(issue -> {}));
            }

            Assertions.assertEquals(1, server.getRequests().size(), "a 400 must not be retried");
        }
    }

    @Test
    public void test_near_limit_header_is_observed() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql",
                    req -> new MockAtlassianServer.MockResponse(200,
                            Map.of("Content-Type", "application/json", "X-RateLimit-NearLimit", "true"),
                            "{\"issues\":" + issuesJson("A-1") + ",\"isLast\":true}"));

            try (JiraClient client = new JiraClient(new DataConfig(), params(server.getBaseUrl()))) {
                client.getIssues(issue -> {});
                Assertions.assertTrue(client.getRateLimitState().isNearLimit(),
                        "the client must carry the near-limit signal forward to later requests");
            }
        }
    }
}
