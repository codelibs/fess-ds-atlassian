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

public class OAuth2Authentication extends Authentication {
    private static final Logger logger = LogManager.getLogger(OAuth2Authentication.class);

    public static final String DEFAULT_TOKEN_URL = "https://auth.atlassian.com/oauth/token";

    protected String accessToken;
    protected String refreshToken;
    protected final String clientId;
    protected final String clientSecret;
    protected final String tokenUrl;
    protected final Consumer<TokenUpdateResult> tokenUpdateCallback;

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

        // セキュリティのためトークンそのもののログ出力は削除
        // 必要であればデバッグレベルでマスクしたものを出力してください
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

    public synchronized void refreshAccessToken() {
        if (StringUtil.isBlank(refreshToken)) {
            throw new AtlassianDataStoreException("Refresh token is not available.");
        }

        final Map<String, String> params = new HashMap<>();
        params.put("grant_type", "refresh_token");
        params.put("refresh_token", refreshToken);
        params.put("client_id", clientId);
        params.put("client_secret", clientSecret);

        // AtlassianのトークンエンドポイントはPOSTでJSONボディを受け取ります
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

                tokenUpdateCallback.accept(new TokenUpdateResult(this.accessToken, this.refreshToken));
            } else {
                throw new AtlassianDataStoreException("Failed to refresh access token. Status: " + response.getHttpStatusCode() + " Body: "
                        + response.getContentAsString());
            }
        } catch (final CurlException | IOException e) {
            throw new AtlassianDataStoreException("Failed to refresh access token.", e);
        }
    }

    protected Map<String, String> parseJsonResponse(final String jsonString) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(jsonString, new TypeReference<Map<String, String>>() {
            });
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse JSON response: " + jsonString, e);
        }
    }

    // アクセストークンを外部(Client側)から更新の確認などで取得したい場合用
    public String getAccessToken() {
        return accessToken;
    }

    public static class TokenUpdateResult {
        private final String accessToken;
        private final String refreshToken;

        public TokenUpdateResult(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }

        public String getAccessToken() {
            return accessToken;
        }

        public String getRefreshToken() {
            return refreshToken;
        }
    }
}