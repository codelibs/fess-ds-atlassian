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
package org.codelibs.fess.ds.atlassian.api.authentication;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ClientCredentialsTest extends UnitDsTestCase {

    @Test
    public void test_client_credentials_grant_is_sent() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/oauth/token", req -> MockAtlassianServer.json("{\"access_token\":\"fresh-token\",\"expires_in\":3600}"));

            final OAuth2Authentication auth = new OAuth2Authentication(null, null, "client-id", "client-secret",
                    server.getBaseUrl() + "/oauth/token", OAuth2Authentication.GRANT_CLIENT_CREDENTIALS, result -> {});
            auth.refreshAccessToken();

            final List<MockAtlassianServer.RecordedRequest> requests = server.getRequests();
            Assertions.assertEquals(1, requests.size());
            // The token request body is JSON, not form encoding: the existing code posts
            // Content-Type: application/json with a Jackson-serialised map.
            Assertions.assertTrue(requests.get(0).body().contains("\"grant_type\":\"client_credentials\""),
                    "body was: " + requests.get(0).body());
            Assertions.assertFalse(requests.get(0).body().contains("refresh_token"),
                    "client_credentials must not send a refresh token: " + requests.get(0).body());
        }
    }

    @Test
    public void test_refresh_token_grant_is_unchanged() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/oauth/token", req -> MockAtlassianServer
                    .json("{\"access_token\":\"fresh-token\",\"refresh_token\":\"next-refresh\",\"expires_in\":3600}"));

            final OAuth2Authentication auth = new OAuth2Authentication("old-token", "old-refresh", "client-id", "client-secret",
                    server.getBaseUrl() + "/oauth/token", OAuth2Authentication.GRANT_AUTHORIZATION_CODE, result -> {});
            auth.refreshAccessToken();

            // JSON, not form encoding -- same as the sibling test above.
            Assertions.assertTrue(server.getRequests().get(0).body().contains("\"grant_type\":\"refresh_token\""),
                    "body was: " + server.getRequests().get(0).body());
        }
    }
}
