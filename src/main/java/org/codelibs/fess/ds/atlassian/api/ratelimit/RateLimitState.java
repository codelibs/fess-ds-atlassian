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
package org.codelibs.fess.ds.atlassian.api.ratelimit;

/**
 * Rate-limit state shared by every request a single client issues.
 *
 * <p>Atlassian's guidance is explicit that a multi-threaded consumer should share its
 * rate-limit state rather than let each thread discover the limit independently, and that
 * parallelising to get around a limit makes things worse. One instance therefore lives on
 * the client and is handed to every request it builds.</p>
 */
public class RateLimitState {

    /** Extra pause applied once the server reports the quota is nearly exhausted. */
    public static final long DEFAULT_NEAR_LIMIT_DELAY_MILLIS = 5000L;

    private final long readIntervalMillis;

    private final long nearLimitDelayMillis;

    private volatile boolean nearLimit;

    /**
     * Constructs a rate-limit state.
     *
     * @param readIntervalMillis the configured interval between requests
     * @param nearLimitDelayMillis the extra pause while near the quota
     */
    public RateLimitState(final long readIntervalMillis, final long nearLimitDelayMillis) {
        this.readIntervalMillis = readIntervalMillis;
        this.nearLimitDelayMillis = nearLimitDelayMillis;
    }

    /**
     * Constructs a rate-limit state with the default near-limit pause.
     *
     * @param readIntervalMillis the configured interval between requests
     * @return the state
     */
    public static RateLimitState of(final long readIntervalMillis) {
        return new RateLimitState(readIntervalMillis, DEFAULT_NEAR_LIMIT_DELAY_MILLIS);
    }

    /**
     * Records the {@code X-RateLimit-NearLimit} header from a response.
     *
     * @param headerValue the header value, or null when absent
     */
    public void observeNearLimit(final String headerValue) {
        nearLimit = "true".equalsIgnoreCase(headerValue);
    }

    /**
     * Returns whether the server last reported the quota as nearly exhausted.
     *
     * @return true when near the limit
     */
    public boolean isNearLimit() {
        return nearLimit;
    }

    /**
     * Returns how long to wait before issuing the next request.
     *
     * @return the wait in milliseconds, possibly zero
     */
    public long nextWaitMillis() {
        return nearLimit ? readIntervalMillis + nearLimitDelayMillis : readIntervalMillis;
    }

    /**
     * Waits the interval returned by {@link #nextWaitMillis()}.
     *
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public void awaitBeforeRequest() throws InterruptedException {
        final long wait = nextWaitMillis();
        if (wait > 0L) {
            Thread.sleep(wait);
        }
    }
}
