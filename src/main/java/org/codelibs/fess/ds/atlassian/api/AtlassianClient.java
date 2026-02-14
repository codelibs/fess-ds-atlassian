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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.authentication.BasicAuthentication;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuth2Authentication;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuthAuthentication;
import org.codelibs.fess.ds.atlassian.api.endpoint.CloudOAuth2EndpointStrategy;
import org.codelibs.fess.ds.atlassian.api.endpoint.DefaultEndpointStrategy;
import org.codelibs.fess.ds.atlassian.api.endpoint.EndpointStrategy;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exbhv.DataConfigBhv;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;

import java.util.stream.Collectors;

/**
 * Abstract base class for Atlassian API clients providing common authentication
 * and HTTP configuration functionality.
 */
public abstract class AtlassianClient {

    private static final Logger logger = LogManager.getLogger(AtlassianClient.class);

    // parameters
    /** Parameter key for the Atlassian instance home URL. */
    protected static final String HOME_PARAM = "home";
    /** Parameter key for Cloud. */
    protected static final String IS_CLOUD = "is_cloud";
    /** Parameter key for authentication type selection. */
    protected static final String AUTH_TYPE_PARAM = "auth_type";
    /** Parameter key for OAuth consumer key. */
    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    /** Parameter key for OAuth private key. */
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    /** Parameter key for OAuth secret/verifier. */
    protected static final String SECRET_PARAM = "oauth.secret";
    /** Parameter key for OAuth access token. */
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";

    /** Parameter key for OAuth2 access token. */
    protected static final String OAUTH2_ACCESS_TOKEN = "oauth2.access_token";
    /** Parameter key for OAuth2 refresh token. */
    protected static final String OAUTH2_REFRESH_TOKEN = "oauth2.refresh_token";
    /** Parameter key for OAuth2 client ID. */
    protected static final String OAUTH2_CLIENT_ID = "oauth2.client_id";
    /** Parameter key for OAuth2 client secret. */
    protected static final String OAUTH2_CLIENT_SECRET = "oauth2.client_secret";
    /** Parameter key for OAuth2 token URL. */
    protected static final String OAUTH2_TOKEN_URL = "oauth2.token_url";

    /** Parameter key for basic authentication username. */
    protected static final String BASIC_USERNAME_PARAM = "basic.username";
    /** Parameter key for basic authentication password. */
    protected static final String BASIC_PASS_PARAM = "basic.password";
    /** Parameter key for HTTP proxy host. */
    protected static final String PROXY_HOST_PARAM = "proxy_host";
    /** Parameter key for HTTP proxy port. */
    protected static final String PROXY_PORT_PARAM = "proxy_port";
    /** Parameter key for HTTP connection timeout. */
    protected static final String HTTP_CONNECTION_TIMEOUT = "connection_timeout";
    /** Parameter key for HTTP read timeout. */
    protected static final String HTTP_READ_TIMEOUT = "read_timeout";

    // values for parameters
    /** Authentication type constant for basic authentication. */
    protected static final String BASIC = "basic";
    /** Authentication type constant for OAuth authentication. */
    protected static final String OAUTH = "oauth";

    /** Authentication type constant for OAuth2 authentication. */
    protected static final String OAUTH2 = "oauth2";

    /** The authentication instance used for API requests. */
    protected Authentication authentication;
    /** Endpoint Strategy **/
    protected EndpointStrategy endpointStrategy;
    /** HTTP connection timeout in milliseconds. */
    protected Integer connectionTimeout;
    /** HTTP read timeout in milliseconds. */
    protected Integer readTimeout;

    /**
     * Constructs a new Atlassian client with the given parameters.
     *
     * @param dataConfig the data configuration
     * @param paramMap the configuration parameters
     * @param product the Atlassian product type
     */
    protected AtlassianClient(final DataConfig dataConfig, final DataStoreParams paramMap, final AtlassianProduct product) {

        final String home = paramMap.getAsString(HOME_PARAM, StringUtil.EMPTY);

        if (home.isEmpty()) {
            logger.warn("parameter \"{}\" required", HOME_PARAM);
            return;
        }

        final String authType = getAuthType(paramMap);
        switch (authType) {
        case BASIC: {
            logger.info("Setup basic authentication");
            final String username = getBasicUsername(paramMap);
            final String password = getBasicPass(paramMap);
            if (username.isEmpty() || password.isEmpty()) {
                throw new AtlassianDataStoreException(
                        "parameter \"" + BASIC_USERNAME_PARAM + "\" and \"" + BASIC_PASS_PARAM + " required for Basic authentication.");
            }
            authentication = new BasicAuthentication(username, password);
            endpointStrategy = new DefaultEndpointStrategy(home);
            break;
        }
        case OAUTH: {
            logger.info("Setup oauth1 authentication");
            final String consumerKey = getConsumerKey(paramMap);
            final String privateKey = getPrivateKey(paramMap);
            final String verifier = getSecret(paramMap);
            final String accessToken = getAccessToken(paramMap);
            if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || accessToken.isEmpty()) {
                throw new AtlassianDataStoreException("parameter \"" + CONSUMER_KEY_PARAM + "\", \"" + PRIVATE_KEY_PARAM + "\", \""
                        + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" required for OAuth authentication.");
            }
            authentication = new OAuthAuthentication(consumerKey, privateKey, accessToken, verifier);
            endpointStrategy = new DefaultEndpointStrategy(home);
            break;
        }
        case OAUTH2: {
            logger.info("Setup oauth2 authentication");
            final String accessToken = paramMap.getAsString(OAUTH2_ACCESS_TOKEN, StringUtil.EMPTY);
            final String refreshToken = paramMap.getAsString(OAUTH2_REFRESH_TOKEN, StringUtil.EMPTY);
            final String clientId = paramMap.getAsString(OAUTH2_CLIENT_ID, StringUtil.EMPTY);
            final String clientSecret = paramMap.getAsString(OAUTH2_CLIENT_SECRET, StringUtil.EMPTY);
            final String tokenUrl = paramMap.getAsString(OAUTH2_TOKEN_URL, OAuth2Authentication.DEFAULT_TOKEN_URL);

            if (accessToken.isEmpty() || clientId.isEmpty() || clientSecret.isEmpty()) {
                throw new AtlassianDataStoreException("Parameters required for OAuth2 are missing.");
            }
            authentication = new OAuth2Authentication(accessToken, refreshToken, clientId, clientSecret, tokenUrl, (tokenUpdateResult) -> {
                // Process for updating DataConfig by refresh token.
                final String paramStr = dataConfig.getHandlerParameterMap().entrySet().stream().map(e -> {
                    String value;
                    if (OAUTH2_ACCESS_TOKEN.equals(e.getKey())) {
                        value = tokenUpdateResult.getAccessToken();
                    } else if (OAUTH2_REFRESH_TOKEN.equals(e.getKey())) {
                        value = tokenUpdateResult.getRefreshToken();
                    } else {
                        value = e.getValue();
                    }
                    if (value != null) {
                        // Escape value.
                        value = value.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "\\r");
                    } else {
                        value = StringUtil.EMPTY;
                    }
                    return e.getKey() + "=" + value;
                }).collect(Collectors.joining("\n"));

                dataConfig.setHandlerParameter(paramStr);
                ComponentUtil.getComponent(DataConfigBhv.class).update(dataConfig);
                logger.info("Updated DataConfig: {}", dataConfig.getId());
            });

            final boolean isCloud = Boolean.parseBoolean(paramMap.getAsString(IS_CLOUD, "true"));
            if (isCloud) {
                endpointStrategy = new CloudOAuth2EndpointStrategy(home, product, authentication);
            } else {
                endpointStrategy = new DefaultEndpointStrategy(home);
            }
            break;
        }
        default: {
            throw new AtlassianDataStoreException(AUTH_TYPE_PARAM + " is empty or invalid.");
        }
        }

        logger.info("EndpointStrategy: {}", endpointStrategy.getClass().getName());

        final String httpProxyHost = getProxyHost(paramMap);
        final String httpProxyPort = getProxyPort(paramMap);
        if (!httpProxyHost.isEmpty()) {
            if (httpProxyPort.isEmpty()) {
                throw new AtlassianDataStoreException(PROXY_PORT_PARAM + " required.");
            }
            try {
                final int port = Integer.parseInt(httpProxyPort);
                authentication.setHttpProxy(httpProxyHost, port);
            } catch (final NumberFormatException e) {
                throw new AtlassianDataStoreException("parameter " + "'" + PROXY_PORT_PARAM + "' invalid.", e);
            }
        }

        if (paramMap.containsKey(HTTP_CONNECTION_TIMEOUT)) {
            connectionTimeout = Integer.parseInt(paramMap.getAsString(HTTP_CONNECTION_TIMEOUT));
        }
        if (paramMap.containsKey(HTTP_READ_TIMEOUT)) {
            readTimeout = Integer.parseInt(paramMap.getAsString(HTTP_READ_TIMEOUT));
        }
    }

    /**
     * Configures a request with authentication and timeout settings.
     *
     * @param <T> the request type
     * @param request the request to configure
     * @return the configured request
     */
    protected <T extends AtlassianRequest> T createRequest(final T request) {
        request.setAuthentication(authentication);
        request.setApiUrl(getApiUrl());
        request.setConnectionTimeout(connectionTimeout);
        request.setReadTimeout(readTimeout);
        return request;
    }

    /**
     * Gets the application home URL.
     *
     * @return the application home URL
     */
    protected abstract String getAppHome();

    /**
     * Gets the application API URL.
     *
     * @return the application API URL
     */
    protected abstract String getAppApiUrl();

    /**
     * Gets the home URL from the parameter map.
     *
     * @return the home URL
     */
    protected String getHome() {
        return endpointStrategy.getHomeUrl();
    }

    /**
     * Gets the API URL.
     *
     * @return the API URL
     */
    protected String getApiUrl() {
        return endpointStrategy.getApiUrl();
    }

    private String getBasicUsername(final DataStoreParams paramMap) {
        return paramMap.getAsString(BASIC_USERNAME_PARAM, StringUtil.EMPTY);
    }

    private String getBasicPass(final DataStoreParams paramMap) {
        return paramMap.getAsString(BASIC_PASS_PARAM, StringUtil.EMPTY);
    }

    private String getConsumerKey(final DataStoreParams paramMap) {
        return paramMap.getAsString(CONSUMER_KEY_PARAM, StringUtil.EMPTY);
    }

    private String getPrivateKey(final DataStoreParams paramMap) {
        return paramMap.getAsString(PRIVATE_KEY_PARAM, StringUtil.EMPTY);
    }

    private String getSecret(final DataStoreParams paramMap) {
        return paramMap.getAsString(SECRET_PARAM, StringUtil.EMPTY);
    }

    private String getAccessToken(final DataStoreParams paramMap) {
        return paramMap.getAsString(ACCESS_TOKEN_PARAM, StringUtil.EMPTY);
    }

    private String getAuthType(final DataStoreParams paramMap) {
        return paramMap.getAsString(AUTH_TYPE_PARAM, StringUtil.EMPTY);
    }

    private String getProxyHost(final DataStoreParams paramMap) {
        return paramMap.getAsString(PROXY_HOST_PARAM, StringUtil.EMPTY);
    }

    private String getProxyPort(final DataStoreParams paramMap) {
        return paramMap.getAsString(PROXY_PORT_PARAM, StringUtil.EMPTY);
    }

}
