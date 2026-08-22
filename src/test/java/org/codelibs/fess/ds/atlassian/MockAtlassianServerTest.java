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
package org.codelibs.fess.ds.atlassian;

import java.util.List;

import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class MockAtlassianServerTest extends UnitDsTestCase {

    @Test
    public void test_serves_registered_path_and_records_request() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/search", req -> MockAtlassianServer.json("{\"results\":[]}"));

            try (CurlResponse response = Curl.get(server.getBaseUrl() + "/rest/api/search?cql=type%3Dpage&limit=25")
                    .header("Authorization", "Basic dXNlcjpwYXNz")
                    .execute()) {
                Assertions.assertEquals(200, response.getHttpStatusCode());
                Assertions.assertEquals("{\"results\":[]}", response.getContentAsString());
            }

            final List<MockAtlassianServer.RecordedRequest> requests = server.getRequests();
            Assertions.assertEquals(1, requests.size());
            final MockAtlassianServer.RecordedRequest recorded = requests.get(0);
            Assertions.assertEquals("GET", recorded.method());
            Assertions.assertEquals("/rest/api/search", recorded.path());
            Assertions.assertEquals("type=page", recorded.query().get("cql"));
            Assertions.assertEquals("25", recorded.query().get("limit"));
            Assertions.assertEquals("Basic dXNlcjpwYXNz", recorded.headers().get("authorization"));
        }
    }

    @Test
    public void test_returns_404_for_unregistered_path() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            try (CurlResponse response = Curl.get(server.getBaseUrl() + "/nope").execute()) {
                Assertions.assertEquals(404, response.getHttpStatusCode());
            }
        }
    }

    @Test
    public void test_handler_can_vary_response_per_call() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/seq", req -> MockAtlassianServer.json("{\"n\":" + server.getRequests().size() + "}"));

            try (CurlResponse first = Curl.get(server.getBaseUrl() + "/seq").execute()) {
                Assertions.assertEquals("{\"n\":1}", first.getContentAsString());
            }
            try (CurlResponse second = Curl.get(server.getBaseUrl() + "/seq").execute()) {
                Assertions.assertEquals("{\"n\":2}", second.getContentAsString());
            }
        }
    }
}
