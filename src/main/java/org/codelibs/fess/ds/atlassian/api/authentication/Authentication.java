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

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.util.function.Function;

import org.codelibs.curl.CurlRequest;

/**
 * Abstract base class for Atlassian API authentication methods.
 */
public abstract class Authentication {

    /**
     * Default constructor for authentication.
     */
    protected Authentication() {
        // Default constructor
    }

    /** HTTP proxy configuration. */
    protected Proxy httpProxy;

    /**
     * Sets the HTTP proxy configuration.
     *
     * @param httpProxyHost the proxy host
     * @param httpProxyPort the proxy port
     */
    public void setHttpProxy(final String httpProxyHost, final Integer httpProxyPort) {
        this.httpProxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost, httpProxyPort));
    }

    /**
     * Creates an authenticated curl request.
     *
     * @param request the curl request function
     * @param requestMethod the HTTP method
     * @param url the target URL
     * @return the authenticated curl request
     */
    public abstract CurlRequest getCurlRequest(final Function<String, CurlRequest> request, final String requestMethod, final URL url);

}
