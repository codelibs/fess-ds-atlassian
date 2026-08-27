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

import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.authentication.BasicAuthentication;
import org.codelibs.fess.ds.atlassian.api.ratelimit.RateLimitState;
import org.codelibs.fess.ds.atlassian.api.ratelimit.RetryPolicy;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Spec 3.3 item 4: "5xx and socket timeouts -- only idempotent GETs retry with the same backoff."
 *
 * <p>A socket timeout on a POST means the request may well have been processed, so replaying it
 * duplicates the write. Every caller goes through {@code getCurlResponse(GET)} today; attachment
 * support is where non-GET traffic first appears.</p>
 */
public class IdempotentRetryTest extends UnitDsTestCase {

    /**
     * Minimal concrete request. {@code getQueryParamMap()} is called exactly once per attempt by
     * {@code doExecute}, and by nothing else, so it doubles as an attempt counter that works even
     * when there is no server to count requests for us.
     */
    private static final class CountingRequest extends AtlassianRequest {

        private final String url;

        private final AtomicInteger attempts = new AtomicInteger();

        CountingRequest(final String url) {
            this.url = url;
            setAuthentication(new BasicAuthentication("user", "pass"));
            setRateLimitState(RateLimitState.of(0L));
            // Zero delay, zero jitter: the retry decision is what is under test, not the timing.
            setRetryPolicy(new RetryPolicy(4, 0L, 0L, () -> 0.0d));
            setConnectionTimeout(Integer.valueOf(2000));
            setReadTimeout(Integer.valueOf(2000));
        }

        @Override
        public String getURL() {
            return url;
        }

        @Override
        public java.util.Map<String, String> getQueryParamMap() {
            attempts.incrementAndGet();
            return null;
        }

        CurlResponse execute(final String requestMethod) {
            return getCurlResponse(requestMethod);
        }

        int attempts() {
            return attempts.get();
        }
    }

    @Test
    public void test_idempotent_methods_are_get_and_delete() {
        Assertions.assertTrue(AtlassianRequest.isIdempotent("GET"));
        Assertions.assertTrue(AtlassianRequest.isIdempotent("DELETE"));
        Assertions.assertFalse(AtlassianRequest.isIdempotent("POST"));
        Assertions.assertFalse(AtlassianRequest.isIdempotent("PUT"));
    }

    /**
     * "Connection reset" arrives as a bare {@link java.net.SocketException}, which is the single
     * most common transient failure on a multi-hour HTTPS crawl. curl4j wraps IO failures, so the
     * whole cause chain has to be walked.
     */
    @Test
    public void test_transient_transport_failures_include_connection_reset() {
        Assertions.assertTrue(AtlassianRequest.isTransientTransportFailure(new java.net.SocketException("Connection reset")));
        Assertions.assertTrue(AtlassianRequest.isTransientTransportFailure(new java.net.SocketTimeoutException("Read timed out")));
        Assertions.assertTrue(AtlassianRequest.isTransientTransportFailure(new java.net.ConnectException("Connection refused")));
        Assertions.assertTrue(AtlassianRequest
                .isTransientTransportFailure(new RuntimeException("wrapped", new java.net.SocketException("Connection reset"))));

        Assertions.assertFalse(AtlassianRequest.isTransientTransportFailure(new java.io.IOException("Premature EOF")));
        Assertions.assertFalse(AtlassianRequest.isTransientTransportFailure(new IllegalStateException("not transport")));
    }

    @Test
    public void test_a_500_is_retried_for_get() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/x", req -> MockAtlassianServer.status(500, "{\"message\":\"boom\"}"));

            final CountingRequest request = new CountingRequest(server.getBaseUrl() + "/x");
            Assertions.assertThrows(AtlassianDataStoreException.class, () -> request.execute("GET"));

            Assertions.assertEquals(5, request.attempts(), "one initial attempt plus four retries");
            Assertions.assertEquals(5, server.getRequests().size());
        }
    }

    @Test
    public void test_a_500_is_retried_for_delete() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/x", req -> MockAtlassianServer.status(500, "{\"message\":\"boom\"}"));

            final CountingRequest request = new CountingRequest(server.getBaseUrl() + "/x");
            Assertions.assertThrows(AtlassianDataStoreException.class, () -> request.execute("DELETE"));

            Assertions.assertEquals(5, request.attempts(), "DELETE is idempotent, so it retries like GET");
        }
    }

    @Test
    public void test_a_500_is_not_retried_for_post() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/x", req -> MockAtlassianServer.status(500, "{\"message\":\"boom\"}"));

            final CountingRequest request = new CountingRequest(server.getBaseUrl() + "/x");
            try (CurlResponse response = request.execute("POST")) {
                Assertions.assertEquals(500, response.getHttpStatusCode());
            }

            Assertions.assertEquals(1, request.attempts(), "a 5xx may mean the POST already landed; replaying it duplicates the write");
            Assertions.assertEquals(1, server.getRequests().size());
        }
    }

    @Test
    public void test_a_500_is_not_retried_for_put() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/x", req -> MockAtlassianServer.status(500, "{\"message\":\"boom\"}"));

            final CountingRequest request = new CountingRequest(server.getBaseUrl() + "/x");
            try (CurlResponse response = request.execute("PUT")) {
                Assertions.assertEquals(500, response.getHttpStatusCode());
            }

            Assertions.assertEquals(1, request.attempts());
        }
    }

    /** 429 means the request was rejected, not processed, so it stays retryable for any method. */
    @Test
    public void test_a_429_is_retried_for_post() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/x", req -> new MockAtlassianServer.MockResponse(429,
                    java.util.Map.of("Content-Type", "application/json", "Retry-After", "0"), "{\"message\":\"rate limited\"}"));

            final CountingRequest request = new CountingRequest(server.getBaseUrl() + "/x");
            Assertions.assertThrows(AtlassianDataStoreException.class, () -> request.execute("POST"));

            Assertions.assertEquals(5, request.attempts(), "a 429 was never processed, so replaying a POST is safe");
        }
    }

    @Test
    public void test_a_transport_failure_is_retried_for_get_but_not_for_post() throws Exception {
        // A port that nothing is listening on: connect() fails with ConnectException immediately,
        // so this exercises the transport branch without any wall-clock cost.
        final String deadUrl;
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            deadUrl = server.getBaseUrl() + "/x";
        }

        final CountingRequest get = new CountingRequest(deadUrl);
        Assertions.assertThrows(AtlassianDataStoreException.class, () -> get.execute("GET"));
        Assertions.assertEquals(5, get.attempts(), "one initial attempt plus four retries");

        final CountingRequest post = new CountingRequest(deadUrl);
        Assertions.assertThrows(AtlassianDataStoreException.class, () -> post.execute("POST"));
        Assertions.assertEquals(1, post.attempts(), "a transport failure gives no evidence that the POST was not processed");
    }
}
