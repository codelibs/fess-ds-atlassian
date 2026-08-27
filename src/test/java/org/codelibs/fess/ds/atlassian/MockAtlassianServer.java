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
package org.codelibs.fess.ds.atlassian;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import com.sun.net.httpserver.HttpServer;

/**
 * Test-only mock HTTP server backed by the JDK built-in HttpServer.
 * Intentionally dependency-free: no mocking library is used anywhere in the fess-ds-* repositories.
 */
public class MockAtlassianServer implements Closeable {

    /** A single request captured by the server. */
    public record RecordedRequest(String method, String path, Map<String, String> query, Map<String, String> headers, String body) {
    }

    /** A canned response returned by a handler. */
    public record MockResponse(int status, Map<String, String> headers, String body) {
    }

    private HttpServer server;

    private final Map<String, Function<RecordedRequest, MockResponse>> handlers = new ConcurrentHashMap<>();

    private final List<RecordedRequest> requests = Collections.synchronizedList(new ArrayList<>());

    /**
     * Builds a 200 JSON response.
     *
     * @param body the response body
     * @return the mock response
     */
    public static MockResponse json(final String body) {
        return new MockResponse(200, Map.of("Content-Type", "application/json"), body);
    }

    /**
     * Builds a response with an explicit status code.
     *
     * @param status the HTTP status code
     * @param body the response body
     * @return the mock response
     */
    public static MockResponse status(final int status, final String body) {
        return new MockResponse(status, Map.of("Content-Type", "application/json"), body);
    }

    /**
     * Starts the server on an ephemeral port.
     *
     * @return this instance
     * @throws IOException if the server cannot bind
     */
    public MockAtlassianServer start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            final String path = exchange.getRequestURI().getPath();
            final RecordedRequest recorded = new RecordedRequest(exchange.getRequestMethod(), path,
                    parseQuery(exchange.getRequestURI().getRawQuery()), lowerCaseHeaders(exchange), readBody(exchange.getRequestBody()));
            requests.add(recorded);

            final Function<RecordedRequest, MockResponse> handler = handlers.get(path);
            final MockResponse response =
                    handler == null ? new MockResponse(404, Map.of("Content-Type", "application/json"), "{\"message\":\"not found\"}")
                            : handler.apply(recorded);

            final byte[] payload = response.body() == null ? new byte[0] : response.body().getBytes(StandardCharsets.UTF_8);
            response.headers().forEach((k, v) -> exchange.getResponseHeaders().add(k, v));
            // HttpExchange reads a length of 0 as "unknown length, use chunked encoding";
            // -1 is the value that means "no response body".
            exchange.sendResponseHeaders(response.status(), payload.length == 0 ? -1 : payload.length);
            try (OutputStream out = exchange.getResponseBody()) {
                if (payload.length > 0) {
                    out.write(payload);
                }
            }
        });
        server.start();
        return this;
    }

    /**
     * Returns the base URL of the running server.
     *
     * @return the base URL without a trailing slash
     */
    public String getBaseUrl() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    /**
     * Registers a handler for an exact request path.
     *
     * @param path the request path
     * @param handler the handler producing a response
     */
    public void on(final String path, final Function<RecordedRequest, MockResponse> handler) {
        handlers.put(path, handler);
    }

    /**
     * Returns all requests captured so far, in arrival order.
     *
     * @return the recorded requests
     */
    public List<RecordedRequest> getRequests() {
        synchronized (requests) {
            return new ArrayList<>(requests);
        }
    }

    @Override
    public void close() {
        if (server != null) {
            server.stop(0);
            server = null;
        }
    }

    private static Map<String, String> parseQuery(final String rawQuery) {
        final Map<String, String> map = new HashMap<>();
        if (rawQuery == null || rawQuery.isEmpty()) {
            return map;
        }
        for (final String pair : rawQuery.split("&")) {
            final int eq = pair.indexOf('=');
            if (eq < 0) {
                map.put(URLDecoder.decode(pair, StandardCharsets.UTF_8), "");
            } else {
                map.put(URLDecoder.decode(pair.substring(0, eq), StandardCharsets.UTF_8),
                        URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
            }
        }
        return map;
    }

    private static Map<String, String> lowerCaseHeaders(final com.sun.net.httpserver.HttpExchange exchange) {
        final Map<String, String> map = new HashMap<>();
        exchange.getRequestHeaders().forEach((k, v) -> map.put(k.toLowerCase(Locale.ROOT), v.isEmpty() ? "" : String.join(",", v)));
        return map;
    }

    private static String readBody(final InputStream in) throws IOException {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            in.transferTo(out);
            return out.toString(StandardCharsets.UTF_8);
        }
    }
}
