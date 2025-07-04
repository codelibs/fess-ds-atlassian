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

public abstract class AtlassianRequest {

    private static final Logger logger = LogManager.getLogger(AtlassianRequest.class);

    protected static final ObjectMapper mapper = new ObjectMapper();

    protected static final String GET = "GET";
    protected static final String POST = "POST";
    protected static final String PUT = "PUT";
    protected static final String DELETE = "DELETE";

    protected static final Function<String, CurlRequest> CURL_GET = Curl::get;
    protected static final Function<String, CurlRequest> CURL_POST = Curl::post;
    protected static final Function<String, CurlRequest> CURL_PUT = Curl::put;
    protected static final Function<String, CurlRequest> CURL_DELETE = Curl::delete;

    protected Authentication authentication;
    protected String appHome;
    protected Integer connectionTimeout;
    protected Integer readTimeout;

    public String appHome() {
        return appHome;
    }

    public abstract String getURL();

    public Map<String, String> getQueryParamMap() {
        return null;
    }

    public Map<String, Object> getBodyMap() {
        return null;
    }

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

    public void setAuthentication(final Authentication authentication) {
        this.authentication = authentication;
    }

    public void setAppHome(final String appHome) {
        this.appHome = appHome;
    }

    public void setConnectionTimeout(final Integer connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setReadTimeout(final Integer readTimeout) {
        this.readTimeout = readTimeout;
    }

}
