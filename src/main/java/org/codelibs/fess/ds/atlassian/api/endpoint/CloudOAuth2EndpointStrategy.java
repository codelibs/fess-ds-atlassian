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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianProduct;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuth2Authentication;
import org.codelibs.fess.ds.atlassian.api.util.UrlUtil;

import java.net.URI;
import java.net.URL;
import java.util.List;

/**
 * Endpoint strategy for Atlassian Cloud using OAuth2 authentication.
 */
public class CloudOAuth2EndpointStrategy implements EndpointStrategy {
    private static final Logger logger = LogManager.getLogger(CloudOAuth2EndpointStrategy.class);

    private static final String CLOUD_API_GATEWAY_URL = "https://api.atlassian.com";

    private static final String CLOUD_API_ACCESSIBLE_RESOURCES_URL = "https://api.atlassian.com/oauth/token/accessible-resources";

    private final String home;

    private final AtlassianProduct product;

    private final OAuth2Authentication authentication;

    private String cachedApiUrl = null;

    /**
     * Constructs a new Cloud OAuth2 endpoint strategy.
     *
     * @param home the Atlassian instance home URL
     * @param product the Atlassian product type
     * @param authentication the authentication instance
     */
    public CloudOAuth2EndpointStrategy(String home, AtlassianProduct product, Authentication authentication) {
        this.home = UrlUtil.normalizeUrl(home);
        this.product = product;
        this.authentication = (OAuth2Authentication) authentication;
    }

    @Override
    public String getHomeUrl() {
        if (AtlassianProduct.CONFLUENCE.equals(product)) {
            if (!home.endsWith("/wiki")) {
                return home + "/wiki";
            }
        }
        return home;
    }

    @Override
    public synchronized String getApiUrl() {
        if (cachedApiUrl != null) {
            return cachedApiUrl;
        }

        // Get Cloud ID
        final String cloudId = resolveCloudId(home, authentication);

        final String segment = product.segment();

        // Build API URL: https://api.atlassian.com/ex/{product}/{cloudId}
        final URI baseUri = URI.create(CLOUD_API_GATEWAY_URL);
        String path = "/ex/" + segment + "/" + cloudId;
        if (AtlassianProduct.CONFLUENCE.equals(product)) {
            path = path + "/wiki";
        }

        cachedApiUrl = UrlUtil.normalizeUrl(baseUri + path);

        logger.info("Resolved Atlassian Cloud API URL: {}", cachedApiUrl);

        return cachedApiUrl;
    }

    private String resolveCloudId(String home, OAuth2Authentication authentication) {
        ResolveCloudIdResponse response = resolveCloudIdInternal(home, authentication);
        if (response.statusCode == 401) {
            // refresh token
            authentication.refreshAccessToken();
            response = resolveCloudIdInternal(home, authentication);
        }

        if (response.statusCode != 200) {
            throw new AtlassianDataStoreException(
                    "Failed to access accessible resources. " + "Status: " + response.statusCode + ", Body: " + response.responseContent);
        }

        return response.cloudId;
    }

    private ResolveCloudIdResponse resolveCloudIdInternal(String home, OAuth2Authentication authentication) {
        try {
            final URL url = new URI(CLOUD_API_ACCESSIBLE_RESOURCES_URL).toURL();
            final CurlRequest request = authentication.getCurlRequest(Curl::get, "GET", url);

            try (CurlResponse response = request.execute()) {
                if (response.getHttpStatusCode() != 200) {
                    return new ResolveCloudIdResponse(response.getHttpStatusCode(), null, response.getContentAsString());
                }

                final ObjectMapper mapper = new ObjectMapper();
                final List<AccessibleResource> resources = mapper.readValue(response.getContentAsString(), new TypeReference<>() {
                });

                // Find sites that match the configured webUrl
                for (final AccessibleResource resource : resources) {
                    if (resource.url != null) {
                        // Normalize the resource URLs and compare them
                        final String resourceUrl = UrlUtil.normalizeUrl(resource.url);
                        if (home.startsWith(resourceUrl)) {
                            return new ResolveCloudIdResponse(response.getHttpStatusCode(), resource.id, response.getContentAsString());
                        }
                    }
                }

                if (logger.isWarnEnabled()) {
                    logger.warn("Accessible resources: {}", resources);
                    logger.warn("Target Web URL: {}", home);
                }
            }
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to resolve Cloud ID for url: " + home, e);
        }

        throw new AtlassianDataStoreException(
                "Cloud ID not found for configured URL: " + home + ". Please check if the user has access to this site.");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class AccessibleResource {
        private String id;
        private String url;
        private String name;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "AccessibleResource [id=" + id + ", url=" + url + ", name=" + name + "]";
        }
    }

    private record ResolveCloudIdResponse(int statusCode, String cloudId, String responseContent) {
    }
}
