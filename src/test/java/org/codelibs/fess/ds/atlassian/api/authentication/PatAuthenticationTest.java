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

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PatAuthenticationTest extends UnitDsTestCase {

    @Test
    public void test_pat_sends_a_bearer_header() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/2/search", req -> MockAtlassianServer.json("{\"startAt\":0,\"maxResults\":50,\"total\":0,\"issues\":[]}"));

            final DataStoreParams p = new DataStoreParams();
            p.put("home", server.getBaseUrl());
            p.put("deployment", "datacenter");
            p.put("auth_type", "pat");
            p.put("pat.token", "NjE1NDpwYXQ");

            try (JiraClient client = new JiraClient(new DataConfig(), p)) {
                client.getIssues(issue -> {});
            }

            final List<MockAtlassianServer.RecordedRequest> requests = server.getRequests();
            Assertions.assertEquals(1, requests.size());
            Assertions.assertEquals("Bearer NjE1NDpwYXQ", requests.get(0).headers().get("authorization"));
        }
    }

    @Test
    public void test_missing_token_is_rejected() {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", "https://jira.example.com");
        p.put("deployment", "datacenter");
        p.put("auth_type", "pat");

        final AtlassianDataStoreException thrown =
                Assertions.assertThrows(AtlassianDataStoreException.class, () -> new JiraClient(new DataConfig(), p).close());
        // Asserting only the exception type would be vacuous: the switch's pre-existing
        // `default:` branch throws the same type for any unrecognised auth_type, so the test
        // would pass even with the PAT case absent entirely. Pin the message instead.
        Assertions.assertTrue(thrown.getMessage().contains("pat.token"),
                "should name the missing parameter rather than merely rejecting the auth type: " + thrown.getMessage());
    }

    @Test
    public void test_auth_type_pat_is_recognised() {
        Assertions.assertEquals("pat", AuthType.PAT.getAuthType());
    }
}
