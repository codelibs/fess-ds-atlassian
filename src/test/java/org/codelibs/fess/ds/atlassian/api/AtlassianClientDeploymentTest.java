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

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.endpoint.CloudBasicEndpointStrategy;
import org.codelibs.fess.ds.atlassian.api.endpoint.DataCenterEndpointStrategy;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AtlassianClientDeploymentTest extends UnitDsTestCase {

    private static DataStoreParams basicParams(final String home) {
        final DataStoreParams params = new DataStoreParams();
        params.put("home", home);
        params.put("auth_type", "basic");
        params.put("basic.username", "user");
        params.put("basic.password", "pass");
        return params;
    }

    @Test
    public void test_cloud_confluence_home_gets_wiki_segment() {
        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), basicParams("https://example.atlassian.net"))) {
            Assertions.assertInstanceOf(CloudBasicEndpointStrategy.class, client.getEndpointStrategy());
            Assertions.assertEquals("https://example.atlassian.net/wiki", client.getConfluenceHome());
        }
    }

    @Test
    public void test_datacenter_confluence_home_is_used_verbatim() {
        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), basicParams("https://wiki.example.com"))) {
            Assertions.assertInstanceOf(DataCenterEndpointStrategy.class, client.getEndpointStrategy());
            Assertions.assertEquals("https://wiki.example.com", client.getConfluenceHome());
        }
    }

    @Test
    public void test_explicit_deployment_overrides_detection() {
        final DataStoreParams params = basicParams("https://example.atlassian.net");
        params.put("deployment", "datacenter");
        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), params)) {
            Assertions.assertInstanceOf(DataCenterEndpointStrategy.class, client.getEndpointStrategy());
            Assertions.assertEquals("https://example.atlassian.net", client.getConfluenceHome());
        }
    }

    @Test
    public void test_deprecated_is_cloud_still_honoured() {
        final DataStoreParams params = basicParams("https://jira.example.com");
        params.put("is_cloud", "true");
        try (JiraClient client = new JiraClient(new DataConfig(), params)) {
            Assertions.assertInstanceOf(CloudBasicEndpointStrategy.class, client.getEndpointStrategy());
        }
    }

    @Test
    public void test_deployment_wins_over_is_cloud() {
        final DataStoreParams params = basicParams("https://jira.example.com");
        params.put("is_cloud", "true");
        params.put("deployment", "datacenter");
        try (JiraClient client = new JiraClient(new DataConfig(), params)) {
            Assertions.assertInstanceOf(DataCenterEndpointStrategy.class, client.getEndpointStrategy());
        }
    }

    @Test
    public void test_jira_api_base_follows_deployment() {
        try (JiraClient cloud = new JiraClient(new DataConfig(), basicParams("https://example.atlassian.net"))) {
            Assertions.assertEquals("/rest/api/3", cloud.getEndpointStrategy().getJiraApiBase());
        }
        try (JiraClient dc = new JiraClient(new DataConfig(), basicParams("https://jira.example.com"))) {
            Assertions.assertEquals("/rest/api/2", dc.getEndpointStrategy().getJiraApiBase());
        }
    }

    /**
     * {@code /rest/api/3} does not exist on Data Center, so the projects endpoint must resolve
     * its API version through the endpoint strategy like every other versioned JIRA request.
     */
    @Test
    public void test_projects_url_follows_deployment() {
        try (JiraClient cloud = new JiraClient(new DataConfig(), basicParams("https://example.atlassian.net"))) {
            Assertions.assertEquals("https://example.atlassian.net/rest/api/3/project", cloud.projects().getURL());
        }
        try (JiraClient dc = new JiraClient(new DataConfig(), basicParams("https://jira.example.com"))) {
            Assertions.assertEquals("https://jira.example.com/rest/api/2/project", dc.projects().getURL());
        }
    }

    @Test
    public void test_missing_home_throws_instead_of_npe() {
        final DataStoreParams params = new DataStoreParams();
        params.put("auth_type", "basic");
        params.put("basic.username", "user");
        params.put("basic.password", "pass");
        Assertions.assertThrows(AtlassianDataStoreException.class, () -> new JiraClient(new DataConfig(), params));
    }
}
