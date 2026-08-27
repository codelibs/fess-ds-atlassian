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

import java.net.HttpURLConnection;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.curl.Curl;
import org.codelibs.curl.CurlRequest;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.authentication.AuthType;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuth2Authentication;
import org.codelibs.fess.ds.atlassian.api.endpoint.EndpointStrategy;
import org.codelibs.fess.ds.atlassian.api.ratelimit.RateLimitState;
import org.codelibs.fess.ds.atlassian.api.ratelimit.RetryPolicy;
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
    /** Application API URL. */
    protected String apiUrl;
    /** Endpoint strategy used to resolve deployment-specific paths and parameters. */
    protected EndpointStrategy endpointStrategy;
    /** HTTP connection timeout in milliseconds. */
    protected Integer connectionTimeout;
    /** HTTP read timeout in milliseconds. */
    protected Integer readTimeout;
    /** Retry policy applied to rate-limited and failed requests. */
    protected RetryPolicy retryPolicy = RetryPolicy.defaults();
    /** Shared rate-limit state observed across this request's client. */
    protected RateLimitState rateLimitState = RateLimitState.of(0L);

    /**
     * Sets the retry policy for this request.
     *
     * @param retryPolicy the retry policy
     */
    public void setRetryPolicy(final RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
    }

    /**
     * Sets the shared rate-limit state for this request.
     *
     * @param rateLimitState the rate-limit state
     */
    public void setRateLimitState(final RateLimitState rateLimitState) {
        this.rateLimitState = rateLimitState;
    }

    /**
     * Gets the application home URL.
     *
     * @return the application home URL
     */
    public String apiUrl() {
        return apiUrl;
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
            int attempt = 0;
            while (true) {
                rateLimitState.awaitBeforeRequest();

                final AtomicReference<HttpURLConnection> connectionRef = new AtomicReference<>();
                CurlResponse response;
                try {
                    response = doExecute(method, requestMethod, connectionRef);

                    if (response.getHttpStatusCode() == 401 && authentication.getAuthType() == AuthType.OAUTH2) {
                        closeQuietly(response);
                        ((OAuth2Authentication) authentication).refreshAccessToken();
                        connectionRef.set(null);
                        response = doExecute(method, requestMethod, connectionRef);
                    }
                } catch (final Exception e) {
                    // A read timeout is now reachable because timeouts have defaults. Without
                    // retrying it we would simply reproduce the reported failure where a slow
                    // Confluence response kills the crawl.
                    if (!isTransientTransportFailure(e) || attempt >= retryPolicy.getMaxRetries()) {
                        throw e;
                    }
                    final long transportDelay = retryPolicy.delayMillis(attempt, null);
                    logger.warn("{} failed with {}; retrying in {} ms (attempt {} of {}).", getURL(), e.getClass().getSimpleName(),
                            transportDelay, attempt + 1, retryPolicy.getMaxRetries());
                    if (transportDelay > 0L) {
                        Thread.sleep(transportDelay);
                    }
                    attempt++;
                    continue;
                }

                // curl4j's CurlResponse exposes no headers, so read them from the connection
                // captured in onConnect. Verified: they remain readable after execute() returns.
                final HttpURLConnection connection = connectionRef.get();
                if (connection != null) {
                    rateLimitState.observeNearLimit(connection.getHeaderField("X-RateLimit-NearLimit"));
                }

                final int statusCode = response.getHttpStatusCode();
                if (!retryPolicy.isRetryable(statusCode)) {
                    return response;
                }
                if (attempt >= retryPolicy.getMaxRetries()) {
                    closeQuietly(response);
                    throw new AtlassianDataStoreException(
                            "Gave up on " + getURL() + " after " + (attempt + 1) + " attempts; last status was " + statusCode + ".");
                }

                final Long retryAfterSeconds = parseRetryAfter(connection);
                final long delay = retryPolicy.delayMillis(attempt, retryAfterSeconds);
                logger.warn("{} returned {}; retrying in {} ms (attempt {} of {}).", getURL(), statusCode, delay, attempt + 1,
                        retryPolicy.getMaxRetries());
                closeQuietly(response);
                if (delay > 0L) {
                    Thread.sleep(delay);
                }
                attempt++;
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AtlassianDataStoreException("Interrupted while accessing " + getURL(), e);
        } catch (final AtlassianDataStoreException e) {
            throw e;
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + getURL(), e);
        }
    }

    private void closeQuietly(final CurlResponse response) {
        try {
            response.close();
        } catch (final Exception e) {
            logger.warn("Failed to close response.", e);
        }
    }

    /**
     * Returns whether a failure is a transport hiccup worth retrying.
     * curl4j wraps IO failures, so the cause chain is walked rather than the top-level type.
     */
    private boolean isTransientTransportFailure(final Throwable throwable) {
        for (Throwable t = throwable; t != null; t = t.getCause()) {
            if (t instanceof java.net.SocketTimeoutException || t instanceof java.net.ConnectException) {
                return true;
            }
            if (t.getCause() == t) {
                break;
            }
        }
        return false;
    }

    private Long parseRetryAfter(final HttpURLConnection connection) {
        if (connection == null) {
            return null;
        }
        final String value = connection.getHeaderField("Retry-After");
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(value.trim());
        } catch (final NumberFormatException e) {
            // Retry-After may also be an HTTP-date. Fall back to the computed backoff.
            logger.debug("Non-numeric Retry-After: {}", value);
            return null;
        }
    }

    private CurlResponse doExecute(final Function<String, CurlRequest> method, final String requestMethod,
            final AtomicReference<HttpURLConnection> connectionRef) throws Exception {
        final StringBuilder urlBuf = new StringBuilder();
        urlBuf.append(getURL());

        final String queryParams = UrlUtil.buildQueryParameters(getQueryParamMap());
        if (!queryParams.isEmpty()) {
            urlBuf.append('?').append(queryParams);
        }

        final CurlRequest request = authentication.getCurlRequest(method, requestMethod, new URI(urlBuf.toString()).toURL());

        final Map<String, Object> bodyMap = getBodyMap();
        if (bodyMap != null) {
            final String source = new JSONObject(bodyMap).toJSONString();
            request.body(source);
        }

        request.onConnect((req, con) -> {
            connectionRef.set(con);
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
     * Sets the application API URL.
     *
     * @param apiUrl the application home URL
     */
    public void setApiUrl(final String apiUrl) {
        this.apiUrl = apiUrl;
    }

    /**
     * Sets the endpoint strategy for this request.
     *
     * @param endpointStrategy the endpoint strategy
     */
    public void setEndpointStrategy(final EndpointStrategy endpointStrategy) {
        this.endpointStrategy = endpointStrategy;
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
