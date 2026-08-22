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
package org.codelibs.fess.ds.atlassian.api.confluence;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfluencePaginationTest extends UnitDsTestCase {

    private static DataStoreParams params(final String home, final String deployment) {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", home);
        p.put("deployment", deployment);
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        p.put("confluence.limit", "2");
        return p;
    }

    private static String resultsJson(final String... titles) {
        final StringBuilder buf = new StringBuilder("[");
        for (int i = 0; i < titles.length; i++) {
            if (i > 0) {
                buf.append(',');
            }
            buf.append("{\"content\":{\"id\":\"")
                    .append(i)
                    .append("\",\"title\":\"")
                    .append(titles[i])
                    .append("\",\"space\":{\"key\":\"SP\"},\"body\":{\"view\":{\"value\":\"body\"}},")
                    .append("\"version\":{\"when\":\"2026-08-01T00:00:00.000Z\"}}}");
        }
        return buf.append(']').toString();
    }

    @Test
    public void test_cloud_follows_links_next_cursor() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/wiki/rest/api/search", req -> {
                final String cursor = req.query().get("cursor");
                if (cursor == null) {
                    return MockAtlassianServer.json("{\"results\":" + resultsJson("P1", "P2")
                            + ",\"_links\":{\"next\":\"/rest/api/search?cql=type+in+%28page%2Cblogpost%29&cursor=CUR2&limit=2\"}}");
                }
                if ("CUR2".equals(cursor)) {
                    return MockAtlassianServer.json("{\"results\":" + resultsJson("P3") + ",\"_links\":{}}");
                }
                return MockAtlassianServer.status(400, "{\"message\":\"bad cursor\"}");
            });

            final List<String> titles = new ArrayList<>();
            try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getContents(content -> titles.add(content.getTitle()));
            }

            Assertions.assertEquals(List.of("P1", "P2", "P3"), titles);
            Assertions.assertEquals(2, server.getRequests().size());
            Assertions.assertNull(server.getRequests().get(0).query().get("start"),
                    "start was removed from /rest/api/search in 2020 and must not be sent");
        }
    }

    /**
     * Regression guard for the 25-document truncation: a full page with no next link
     * must end the loop rather than silently requesting start=25.
     */
    @Test
    public void test_cloud_stops_when_no_next_link() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/wiki/rest/api/search",
                    req -> MockAtlassianServer.json("{\"results\":" + resultsJson("P1", "P2") + ",\"_links\":{}}"));

            final List<String> titles = new ArrayList<>();
            try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), params(server.getBaseUrl(), "cloud"))) {
                client.getContents(content -> titles.add(content.getTitle()));
            }

            Assertions.assertEquals(List.of("P1", "P2"), titles);
            Assertions.assertEquals(1, server.getRequests().size());
        }
    }

    @Test
    public void test_datacenter_uses_start_offset() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/search", req -> {
                final String start = req.query().getOrDefault("start", "0");
                if ("0".equals(start)) {
                    return MockAtlassianServer.json("{\"results\":" + resultsJson("D1", "D2") + "}");
                }
                return MockAtlassianServer.json("{\"results\":" + resultsJson("D3") + "}");
            });

            final List<String> titles = new ArrayList<>();
            try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), params(server.getBaseUrl(), "datacenter"))) {
                client.getContents(content -> titles.add(content.getTitle()));
            }

            Assertions.assertEquals(List.of("D1", "D2", "D3"), titles);
            Assertions.assertEquals(2, server.getRequests().size());
        }
    }

    @Test
    public void test_extract_cursor_from_next_link() {
        Assertions.assertEquals("CUR2",
                GetContentsRequest.extractCursor("/rest/api/search?cql=type+in+%28page%2Cblogpost%29&cursor=CUR2&limit=2"));
        Assertions.assertEquals("abc", GetContentsRequest.extractCursor("/rest/api/search?cursor=abc"));
        Assertions.assertNull(GetContentsRequest.extractCursor("/rest/api/search?limit=2"));
        Assertions.assertNull(GetContentsRequest.extractCursor(null));
        Assertions.assertNull(GetContentsRequest.extractCursor(""));
    }

    /**
     * Real Confluence Cloud cursors are opaque base64-ish strings containing {@code :} and
     * {@code =}, so {@code _links.next} carries them percent-encoded. Returning the raw encoded
     * form would re-encode it on the follow-up request and yield a 400 or an empty page on the
     * second page of every space.
     */
    @Test
    public void test_extract_cursor_percent_decodes_the_value() {
        Assertions.assertEquals("raw:Y29udGVudA==",
                GetContentsRequest.extractCursor("/rest/api/search?cql=type%3Dpage&cursor=raw%3AY29udGVudA%3D%3D&limit=25"));
    }
}
