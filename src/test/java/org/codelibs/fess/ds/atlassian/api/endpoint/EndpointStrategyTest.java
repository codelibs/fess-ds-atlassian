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
package org.codelibs.fess.ds.atlassian.api.endpoint;

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.AtlassianProduct;
import org.codelibs.fess.ds.atlassian.api.Deployment;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class EndpointStrategyTest extends UnitDsTestCase {

    @Test
    public void test_cloud_confluence_appends_wiki() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.CONFLUENCE);
        Assertions.assertEquals("https://example.atlassian.net/wiki", strategy.getHomeUrl());
        Assertions.assertEquals("https://example.atlassian.net/wiki", strategy.getApiUrl());
    }

    @Test
    public void test_cloud_confluence_does_not_double_append_wiki() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net/wiki", AtlassianProduct.CONFLUENCE);
        Assertions.assertEquals("https://example.atlassian.net/wiki", strategy.getHomeUrl());
    }

    @Test
    public void test_cloud_confluence_normalizes_trailing_slash_before_appending() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net/", AtlassianProduct.CONFLUENCE);
        Assertions.assertEquals("https://example.atlassian.net/wiki", strategy.getHomeUrl());
    }

    @Test
    public void test_cloud_jira_does_not_append_wiki() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.JIRA);
        Assertions.assertEquals("https://example.atlassian.net", strategy.getHomeUrl());
    }

    @Test
    public void test_jira_api_base_differs_by_deployment() {
        Assertions.assertEquals("/rest/api/3",
                new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.JIRA).getJiraApiBase());
        Assertions.assertEquals("/rest/api/2", new DataCenterEndpointStrategy("https://jira.example.com").getJiraApiBase());
    }

    @Test
    public void test_deployment_is_reported() {
        Assertions.assertEquals(Deployment.CLOUD,
                new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.JIRA).getDeployment());
        Assertions.assertEquals(Deployment.DATA_CENTER, new DataCenterEndpointStrategy("https://jira.example.com").getDeployment());
    }

    @Test
    public void test_content_view_url_cloud_uses_space_path() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.CONFLUENCE);
        Assertions.assertEquals("https://example.atlassian.net/wiki/spaces/SUPP/pages/34803911",
                strategy.getContentViewUrl("34803911", "SUPP"));
    }

    @Test
    public void test_content_view_url_cloud_falls_back_without_space_key() {
        final EndpointStrategy strategy = new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.CONFLUENCE);
        Assertions.assertEquals("https://example.atlassian.net/wiki/pages/viewpage.action?pageId=34803911",
                strategy.getContentViewUrl("34803911", null));
    }

    @Test
    public void test_content_view_url_datacenter_uses_viewpage_action() {
        final EndpointStrategy strategy = new DataCenterEndpointStrategy("https://wiki.example.com");
        Assertions.assertEquals("https://wiki.example.com/pages/viewpage.action?pageId=34803911",
                strategy.getContentViewUrl("34803911", "SUPP"));
    }

    @Test
    public void test_issue_view_url_is_same_for_both_deployments() {
        Assertions.assertEquals("https://example.atlassian.net/browse/ABC-1",
                new CloudBasicEndpointStrategy("https://example.atlassian.net", AtlassianProduct.JIRA).getIssueViewUrl("ABC-1"));
        Assertions.assertEquals("https://jira.example.com/browse/ABC-1",
                new DataCenterEndpointStrategy("https://jira.example.com").getIssueViewUrl("ABC-1"));
    }
}
