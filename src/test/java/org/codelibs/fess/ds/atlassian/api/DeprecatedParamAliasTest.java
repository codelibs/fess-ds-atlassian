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
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeprecatedParamAliasTest extends UnitDsTestCase {

    private static DataStoreParams base() {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", "https://example.atlassian.net");
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        return p;
    }

    @Test
    public void test_jira_new_keys_are_used() {
        final DataStoreParams p = base();
        p.put("jira.jql", "project = ABC");
        p.put("jira.max_results", "10");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals("project = ABC", client.getJql());
            Assertions.assertEquals(Integer.valueOf(10), client.getIssueMaxResults());
        }
    }

    @Test
    public void test_jira_deprecated_keys_still_work() {
        final DataStoreParams p = base();
        p.put("issue.jql", "project = OLD");
        p.put("issue_max_results", "7");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals("project = OLD", client.getJql());
            Assertions.assertEquals(Integer.valueOf(7), client.getIssueMaxResults());
        }
    }

    @Test
    public void test_jira_new_key_wins_over_deprecated() {
        final DataStoreParams p = base();
        p.put("issue.jql", "project = OLD");
        p.put("jira.jql", "project = NEW");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals("project = NEW", client.getJql());
        }
    }

    @Test
    public void test_jira_defaults_are_unchanged() {
        try (JiraClient client = new JiraClient(new DataConfig(), base())) {
            Assertions.assertEquals("created is not empty", client.getJql());
            Assertions.assertEquals(Integer.valueOf(50), client.getIssueMaxResults());
        }
    }

    @Test
    public void test_confluence_limit_new_and_deprecated_keys() {
        final DataStoreParams withNew = base();
        withNew.put("confluence.limit", "100");
        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), withNew)) {
            Assertions.assertEquals(Integer.valueOf(100), client.getContentLimit());
        }

        final DataStoreParams withOld = base();
        withOld.put("content_limit", "5");
        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), withOld)) {
            Assertions.assertEquals(Integer.valueOf(5), client.getContentLimit());
        }

        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), base())) {
            Assertions.assertEquals(Integer.valueOf(25), client.getContentLimit());
        }
    }
}
