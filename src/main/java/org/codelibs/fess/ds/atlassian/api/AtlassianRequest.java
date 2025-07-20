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

import java.net.URL;
import java.util.Map;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.util.UrlUtil;

import com.fasterxml.jackson.databind.ObjectMapper;

import net.minidev.json.JSONObject;

/**
 * Abstract base class for Atlassian API requests providing common HTTP functionality.
 */
public abstract class AtlassianRequest {

    /**
     * Default constructor for Atlassian request.
     */
    protected AtlassianRequest() {
        // Default constructor
    }

    private static final Logger logger = LogManager.getLogger(AtlassianRequest.class);

    /** JSON object mapper for request/response serialization. */
    protected static final ObjectMapper mapper = new ObjectMapper();

    /** HTTP GET method constant. */
    protected static final String GET = "GET";
    /** HTTP POST method constant. */
    protected static final String POST = "POST";
    /** HTTP PUT method constant. */
    protected static final String PUT = "PUT";
    /** HTTP DELETE method constant. */
    protected static final String DELETE = "DELETE";

    /** Function to create GET curl requests. */
    protected static final Function<String, CurlRequest> CURL_GET = Curl::get;
    /** Function to create POST curl requests. */
    protected static final Function<String, CurlRequest> CURL_POST = Curl::post;
    /** Function to create PUT curl requests. */
    protected static final Function<String, CurlRequest> CURL_PUT = Curl::put;
    /** Function to create DELETE curl requests. */
    protected static final Function<String, CurlRequest> CURL_DELETE = Curl::delete;

    /** Authentication instance for API requests. */
    protected Authentication authentication;
    /** Application home URL. */
    protected String appHome;
    /** HTTP connection timeout in milliseconds. */
    protected Integer connectionTimeout;
    /** HTTP read timeout in milliseconds. */
    protected Integer readTimeout;

    /**
     * Gets the application home URL.
     *
     * @return the application home URL
     */
    public String appHome() {
        return appHome;
    }

    /**
     * Gets the complete URL for this request.
     *
     * @return the request URL
     */
    public abstract String getURL();

    /**
     * Gets the query parameters for this request.
     *
     * @return the query parameter map, or null if no parameters
     */
    public Map<String, String> getQueryParamMap() {
        return null;
    }

    /**
     * Gets the request body parameters.
     *
     * @return the body parameter map, or null if no body
     */
    public Map<String, Object> getBodyMap() {
        return null;
    }

    /**
     * Executes the HTTP request using the specified method.
     *
     * @param requestMethod the HTTP method to use
     * @return the HTTP response
     */
    public CurlResponse getCurlResponse(final String requestMethod) {
        switch (requestMethod) {
        case GET:
            return getCurlResponse(CURL_GET, GET);
        case DELETE:
            return getCurlResponse(CURL_DELETE, DELETE);
        case POST:
            return getCurlResponse(CURL_POST, POST);
        case PUT:
            return getCurlResponse(CURL_PUT, PUT);
        default: {
            throw new IllegalArgumentException("Invalid request method : " + requestMethod);
        }
        }
    }

    /**
     * Executes the HTTP request using the specified curl method function.
     *
     * @param method the curl method function
     * @param requestMethod the HTTP method name
     * @return the HTTP response
     */
    public CurlResponse getCurlResponse(final Function<String, CurlRequest> method, final String requestMethod) {
        try {
            final StringBuilder urlBuf = new StringBuilder();
            urlBuf.append(getURL());

            final String queryParams = UrlUtil.buildQueryParameters(getQueryParamMap());
            if (!queryParams.isEmpty()) {
                urlBuf.append('?').append(queryParams);
            }

            final CurlRequest request = authentication.getCurlRequest(method, requestMethod, new URL(urlBuf.toString()));

            final Map<String, Object> bodyMap = getBodyMap();
            if (bodyMap != null) {
                final String source = new JSONObject(bodyMap).toJSONString();
                request.body(source);
            }

            request.onConnect((req, con) -> {
                if (logger.isDebugEnabled()) {
                    logger.debug("connectionTimeout: {}, readTimeout: {}", connectionTimeout, readTimeout);
                }
                if (connectionTimeout != null) {
                    con.setConnectTimeout(connectionTimeout);
                }
                if (readTimeout != null) {
                    con.setReadTimeout(readTimeout);
                }
            });

            return request.execute();
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + getURL(), e);
        }
    }

    /**
     * Sets the authentication for this request.
     *
     * @param authentication the authentication instance
     */
    public void setAuthentication(final Authentication authentication) {
        this.authentication = authentication;
    }

    /**
     * Sets the application home URL.
     *
     * @param appHome the application home URL
     */
    public void setAppHome(final String appHome) {
        this.appHome = appHome;
    }

    /**
     * Sets the HTTP connection timeout.
     *
     * @param connectionTimeout the connection timeout in milliseconds
     */
    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    /**
     * Sets the HTTP read timeout.
     *
     * @param readTimeout the read timeout in milliseconds
     */
    public void setReadTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

}
