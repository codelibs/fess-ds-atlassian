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

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * OAuth2 authentication implementation for Atlassian API.
 */
public class OAuth2Authentication extends Authentication {
    private static final Logger logger = LogManager.getLogger(OAuth2Authentication.class);

    /** The default token URL for Atlassian OAuth2. */
    public static final String DEFAULT_TOKEN_URL = "https://auth.atlassian.com/oauth/token";
    private static final long MIN_REFRESH_INTERVAL = 3000;

    /** The access token. */
    protected String accessToken;
    /** The refresh token. */
    protected String refreshToken;
    /** The client ID. */
    protected final String clientId;
    /** The client secret. */
    protected final String clientSecret;
    /** The token URL. */
    protected final String tokenUrl;
    /** The callback for token updates. */
    protected final Consumer<TokenUpdateResult> tokenUpdateCallback;
    private volatile long lastRefreshTime = 0;

    /**
     * Constructs a new OAuth2 authentication.
     *
     * @param accessToken the access token
     * @param refreshToken the refresh token
     * @param clientId the client ID
     * @param clientSecret the client secret
     * @param tokenUrl the token URL
     * @param tokenUpdateCallback the callback for token updates
     */
    public OAuth2Authentication(final String accessToken, final String refreshToken, final String clientId, final String clientSecret,
            final String tokenUrl, final Consumer<TokenUpdateResult> tokenUpdateCallback) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.tokenUrl = tokenUrl;
        this.tokenUpdateCallback = tokenUpdateCallback;
    }

    @Override
    public CurlRequest getCurlRequest(final Function<String, CurlRequest> method, final String requestMethod, final URL url) {
        final CurlRequest request = method.apply(url.toString());

        if (logger.isDebugEnabled()) {
            logger.debug("Setting OAuth2 Authorization header.");
        }

        request.header("Authorization", "Bearer " + accessToken);
        request.header("Accept", "application/json");

        if (httpProxy != null) {
            request.proxy(httpProxy);
        }

        return request;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.OAUTH2;
    }

    /**
     * Refreshes the access token using the refresh token.
     */
    public synchronized void refreshAccessToken() {
        final long currentTime = System.currentTimeMillis();

        // Skip refreshing token if token was refreshed recently.
        if (currentTime - lastRefreshTime < MIN_REFRESH_INTERVAL) {
            logger.debug("Access token was refreshed recently. Skipping.");
            return;
        }

        if (StringUtil.isBlank(refreshToken)) {
            throw new AtlassianDataStoreException("Refresh token is not available.");
        }

        final Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);

        final CurlRequest request = Curl.post(tokenUrl).header("Content-Type", "application/json");

        if (httpProxy != null) {
            request.proxy(httpProxy);
        }

        final ObjectMapper mapper = new ObjectMapper();
        try {
            final String jsonBody = mapper.writeValueAsString(params);
            request.body(jsonBody);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to create JSON body for refresh token request.", e);
        }

        try (CurlResponse response = request.execute()) {
            if (response.getHttpStatusCode() == 200) {
                final String content = response.getContentAsString();
                final Map<String, String> tokenMap = parseJsonResponse(content);

                final String newAccessToken = tokenMap.get("access_token");
                if (StringUtil.isNotBlank(newAccessToken)) {
                    this.accessToken = newAccessToken;
                }

                final String newRefreshToken = tokenMap.get("refresh_token");
                if (StringUtil.isNotBlank(newRefreshToken)) {
                    this.refreshToken = newRefreshToken;
                }

                logger.info("Refreshed access token successfully.");

                this.lastRefreshTime = System.currentTimeMillis();
                tokenUpdateCallback.accept(new TokenUpdateResult(this.accessToken, this.refreshToken));
            } else {
                throw new AtlassianDataStoreException("Failed to refresh access token. Status: " + response.getHttpStatusCode() + " Body: "
                        + response.getContentAsString());
            }
        } catch (final CurlException | IOException e) {
            throw new AtlassianDataStoreException("Failed to refresh access token.", e);
        }
    }

    /**
     * Parses a JSON response string into a map.
     *
     * @param jsonString the JSON string to parse
     * @return the parsed map
     */
    protected Map<String, String> parseJsonResponse(final String jsonString) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonString, new TypeReference<Map<String, String>>() {
            });
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse JSON response: " + jsonString, e);
        }
    }

    /**
     * Returns the access token.
     *
     * @return the access token
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Holds the result of a token update operation.
     */
    public static class TokenUpdateResult {
        private final String accessToken;
        private final String refreshToken;

        /**
         * Constructs a new token update result.
         *
         * @param accessToken the new access token
         * @param refreshToken the new refresh token
         */
        public TokenUpdateResult(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        /**
         * Returns the access token.
         *
         * @return the access token
         */
        public String getAccessToken() {
            return accessToken;
        }

        /**
         * Returns the refresh token.
         *
         * @return the refresh token
         */
        public String getRefreshToken() {
            return refreshToken;
        }
    }
}