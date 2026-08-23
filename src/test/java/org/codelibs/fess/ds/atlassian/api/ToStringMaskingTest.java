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

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ToStringMaskingTest extends UnitDsTestCase {

    @Test
    public void test_client_to_string_names_the_instance_without_leaking_credentials() {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", "https://example.atlassian.net");
        p.put("auth_type", "basic");
        p.put("basic.username", "crawler");
        p.put("basic.password", "s3cr3t");

        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            final String text = client.toString();
            Assertions.assertTrue(text.contains("example.atlassian.net"), "should name the instance: " + text);
            Assertions.assertFalse(text.contains("s3cr3t"), "must not contain the password: " + text);
            Assertions.assertFalse(text.contains("crawler"), "must not contain the username: " + text);
            Assertions.assertFalse(text.contains("@"), "must not fall back to Object.toString(): " + text);
        }
    }

    @Test
    public void test_projects_request_to_string_names_its_url() {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", "https://example.atlassian.net");
        p.put("auth_type", "basic");
        p.put("basic.username", "crawler");
        p.put("basic.password", "s3cr3t");

        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            final String text = client.projects().toString();
            Assertions.assertTrue(text.contains("/rest/api/3/project"), "should name the endpoint: " + text);
            Assertions.assertFalse(text.contains("s3cr3t"), "must not contain the password: " + text);
        }
    }
}
