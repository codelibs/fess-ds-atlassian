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
import java.util.function.Function;

import org.codelibs.curl.CurlRequest;

/**
 * Personal Access Token authentication for Atlassian Data Center.
 *
 * <p>PATs exist on Jira Server/Data Center 8.14.0 and later and Confluence Server/Data Center
 * 7.9.0 and later. Atlassian Cloud has no equivalent; there the API-token flow is HTTP Basic.</p>
 */
public class PatAuthentication extends Authentication {

    /** The personal access token. */
    protected final String token;

    /**
     * Constructs a new PAT authentication with the given token.
     *
     * @param token the personal access token
     */
    public PatAuthentication(final String token) {
        this.token = token;
    }

    @Override
    public CurlRequest getCurlRequest(final Function<String, CurlRequest> method, final String requestMethod, final URL url) {
        final CurlRequest request = method.apply(url.toString()).header("Authorization", "Bearer " + token);

        if (httpProxy != null) {
            request.proxy(httpProxy);
        }

        return request;
    }

    @Override
    public AuthType getAuthType() {
        return AuthType.PAT;
    }
}
