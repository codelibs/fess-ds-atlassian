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

import java.net.URL;
import java.security.PrivateKey;
import java.util.function.Function;

import org.codelibs.curl.CurlRequest;
import org.codelibs.fess.ds.atlassian.api.util.OAuthUtil;

public class OAuthAuthentication extends Authentication {

    protected final String consumerKey;
    protected final PrivateKey privateKey;
    protected final String token;
    protected final String verifier;

    public OAuthAuthentication(final String consumerKey, final String privateKey, final String token, final String verifier) {
        this.consumerKey = consumerKey;
        this.privateKey = OAuthUtil.getPrivateKey(privateKey);
        this.token = token;
        this.verifier = verifier;
    }

    @Override
    public CurlRequest getCurlRequest(final Function<String, CurlRequest> method, final String requestMethod, final URL url) {
        final CurlRequest request = method.apply(url.toString());

        request.header("Authorization", OAuthUtil.getAuthorizationHeader(consumerKey, privateKey, token, verifier, requestMethod, url));

        if (httpProxy != null) {
            request.proxy(httpProxy);
        }

        return request;
    }

}
