/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
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

import java.io.Closeable;
import java.util.Map;

import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.apache.ApacheHttpTransport;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AtlassianClient {

    private static final Logger logger = LoggerFactory.getLogger(JiraClient.class);

    // parameters
    protected static final String HOME_PARAM = "home";
    protected static final String AUTH_TYPE_PARAM = "auth_type";
    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";
    protected static final String USERNAME_PARAM = "basic.username";
    protected static final String PASSWORD_PARAM = "basic.password";

    // values for parameters
    protected static final String BASIC = "basic";
    protected static final String OAUTH = "oauth";

    protected HttpRequestFactory httpRequestFactory;

    public AtlassianClient(final Map<String, String> paramMap) {

        final String home = getHome(paramMap);
        final String userName = getUserName(paramMap);
        final String password = getPassword(paramMap);
        final String consumerKey = getConsumerKey(paramMap);
        final String privateKey = getPrivateKey(paramMap);
        final String verifier = getSecret(paramMap);
        final String temporaryToken = getAccessToken(paramMap);

        if (home.isEmpty()) {
            logger.warn("parameter \"{}\" required", HOME_PARAM);
            return;
        }

        final String authType = getAuthType(paramMap);
        switch (authType) {
            case BASIC: {
                if (userName.isEmpty() || password.isEmpty()) {
                    throw new AtlassianDataStoreException("parameter \"" + USERNAME_PARAM + "\" and \"" + PASSWORD_PARAM + " required for Basic authentication.");
                }
                httpRequestFactory = builder().basicAuth(userName, password).build();
                break;
            }
            case OAUTH: {
                if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || temporaryToken.isEmpty()) {
                    throw new AtlassianDataStoreException("parameter \"" + CONSUMER_KEY_PARAM + "\", \""
                            + PRIVATE_KEY_PARAM + "\", \"" + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" required for OAuth authentication.");
                }
                httpRequestFactory = builder().oAuthToken(home, accessToken -> {
                    accessToken.consumerKey = consumerKey;
                    accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner(privateKey);
                    accessToken.transport = new ApacheHttpTransport();
                    accessToken.verifier = verifier;
                    accessToken.temporaryToken = temporaryToken;
                }).build();
                break;
            }
            default: {
                throw new AtlassianDataStoreException(AUTH_TYPE_PARAM + " is empty or invalid.");
            }
        }

    }

    public static AtlassianClientBuilder builder() {
        return new AtlassianClientBuilder();
    }

    public HttpRequestFactory request() {
        return httpRequestFactory;
    }

    protected String getHome(Map<String, String> paramMap) {
        if (paramMap.containsKey(HOME_PARAM)) {
            return paramMap.get(HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getUserName(final Map<String, String> paramMap) {
        if (paramMap.containsKey(USERNAME_PARAM)) {
            return paramMap.get(USERNAME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPassword(final Map<String, String> paramMap) {
        if (paramMap.containsKey(PASSWORD_PARAM)) {
            return paramMap.get(PASSWORD_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getConsumerKey(final Map<String, String> paramMap) {
        if (paramMap.containsKey(CONSUMER_KEY_PARAM)) {
            return paramMap.get(CONSUMER_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPrivateKey(final Map<String, String> paramMap) {
        if (paramMap.containsKey(PRIVATE_KEY_PARAM)) {
            return paramMap.get(PRIVATE_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getSecret(final Map<String, String> paramMap) {
        if (paramMap.containsKey(SECRET_PARAM)) {
            return paramMap.get(SECRET_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAccessToken(final Map<String, String> paramMap) {
        if (paramMap.containsKey(ACCESS_TOKEN_PARAM)) {
            return paramMap.get(ACCESS_TOKEN_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAuthType(final Map<String, String> paramMap) {
        if (paramMap.containsKey(AUTH_TYPE_PARAM)) {
            return paramMap.get(AUTH_TYPE_PARAM);
        }
        return StringUtil.EMPTY;
    }

}
