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
import org.codelibs.fess.ds.atlassian.api.authentication.OAuthAuthentication;
import org.codelibs.fess.entity.DataStoreParams;

public abstract class AtlassianClient {

    private static final Logger logger = LogManager.getLogger(AtlassianClient.class);

    // parameters
    protected static final String HOME_PARAM = "home";
    protected static final String AUTH_TYPE_PARAM = "auth_type";
    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";
    protected static final String BASIC_USERNAME_PARAM = "basic.username";
    protected static final String BASIC_PASS_PARAM = "basic.password";
    protected static final String PROXY_HOST_PARAM = "proxy_host";
    protected static final String PROXY_PORT_PARAM = "proxy_port";
    protected static final String HTTP_CONNECTION_TIMEOUT = "connection_timeout";
    protected static final String HTTP_READ_TIMEOUT = "read_timeout";

    // values for parameters
    protected static final String BASIC = "basic";
    protected static final String OAUTH = "oauth";

    protected Authentication authentication;
    protected Integer connectionTimeout;
    protected Integer readTimeout;

    protected AtlassianClient(final DataStoreParams paramMap) {

        final String home = getHome(paramMap);

        if (home.isEmpty()) {
            logger.warn("parameter \"{}\" required", HOME_PARAM);
            return;
        }

        final String authType = getAuthType(paramMap);
        switch (authType) {
        case BASIC: {
            final String username = getBasicUsername(paramMap);
            final String password = getBasicPass(paramMap);
            if (username.isEmpty() || password.isEmpty()) {
                throw new AtlassianDataStoreException(
                        "parameter \"" + BASIC_USERNAME_PARAM + "\" and \"" + BASIC_PASS_PARAM + " required for Basic authentication.");
            }
            authentication = new BasicAuthentication(username, password);
            break;
        }
        case OAUTH: {
            final String consumerKey = getConsumerKey(paramMap);
            final String privateKey = getPrivateKey(paramMap);
            final String verifier = getSecret(paramMap);
            final String accessToken = getAccessToken(paramMap);
            if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || accessToken.isEmpty()) {
                throw new AtlassianDataStoreException("parameter \"" + CONSUMER_KEY_PARAM + "\", \"" + PRIVATE_KEY_PARAM + "\", \""
                        + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" required for OAuth authentication.");
            }
            authentication = new OAuthAuthentication(consumerKey, privateKey, accessToken, verifier);
            break;
        }
        default: {
            throw new AtlassianDataStoreException(AUTH_TYPE_PARAM + " is empty or invalid.");
        }
        }

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

    protected <T extends AtlassianRequest> T createRequest(final T request) {
        request.setAuthentication(authentication);
        request.setAppHome(getAppHome());
        request.setConnectionTimeout(connectionTimeout);
        request.setReadTimeout(readTimeout);
        return request;
    }

    protected abstract String getAppHome();

    protected String getHome(final DataStoreParams paramMap) {
        return paramMap.getAsString(HOME_PARAM, StringUtil.EMPTY);
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
