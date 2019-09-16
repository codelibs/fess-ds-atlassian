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
package org.codelibs.fess.ds.atlassian.api.authentication;

import java.net.URL;
import java.util.Base64;
import java.util.function.Function;

import org.codelibs.curl.CurlRequest;

public class BasicAuthentication extends Authentication {

    protected final String username;
    protected final String password;

    public BasicAuthentication(final String username, final String password) {
        this.username = username;
        this.password = password;
    }

    public CurlRequest getCurlRequest(final Function<String, CurlRequest> method, final String requestMethod, final URL url) {
        final CurlRequest request =  method.apply(url.toString()).header("Authorization", "Basic " + encode(username + ":" + password));

        if (httpProxy != null) {
            request.proxy(httpProxy);
        }

        return request;
    }

    protected static String encode(final String s) {
        return Base64.getEncoder().encodeToString(s.getBytes());
    }

}
