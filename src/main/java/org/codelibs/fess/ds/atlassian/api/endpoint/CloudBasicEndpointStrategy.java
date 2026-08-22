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

import org.codelibs.fess.ds.atlassian.api.AtlassianProduct;
import org.codelibs.fess.ds.atlassian.api.Deployment;
import org.codelibs.fess.ds.atlassian.api.util.UrlUtil;

/**
 * Endpoint strategy for Atlassian Cloud when the site URL is addressed directly
 * (basic, API token, OAuth 1.0a and PAT authentication).
 * Confluence Cloud serves its REST API under {@code /wiki}, so the segment is appended when missing.
 */
public class CloudBasicEndpointStrategy implements EndpointStrategy {

    private static final String WIKI_SEGMENT = "/wiki";

    private final String home;

    /**
     * Constructs a Cloud endpoint strategy.
     *
     * @param home the site home URL
     * @param product the Atlassian product
     */
    public CloudBasicEndpointStrategy(final String home, final AtlassianProduct product) {
        final String normalized = UrlUtil.normalizeUrl(home);
        if (product == AtlassianProduct.CONFLUENCE && !normalized.endsWith(WIKI_SEGMENT)) {
            this.home = normalized + WIKI_SEGMENT;
        } else {
            this.home = normalized;
        }
    }

    @Override
    public String getHomeUrl() {
        return home;
    }

    @Override
    public String getApiUrl() {
        return home;
    }

    @Override
    public Deployment getDeployment() {
        return Deployment.CLOUD;
    }
}
