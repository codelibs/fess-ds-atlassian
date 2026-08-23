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

import java.util.function.DoubleSupplier;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Retry timing for Atlassian API requests.
 *
 * <p>The defaults come from Atlassian's own guidance: start at two seconds, double each
 * attempt, cap at thirty, multiply by a jitter factor in [0.7, 1.3], and give up after four
 * retries. A {@code Retry-After} header is an instruction from the server and overrides all
 * of that, up to {@link #MAX_RETRY_AFTER_SECONDS}.</p>
 */
public class RetryPolicy {

    private static final Logger logger = LogManager.getLogger(RetryPolicy.class);

    /**
     * Ceiling applied to a server-supplied {@code Retry-After}.
     *
     * <p>One hour comfortably covers every value Atlassian documents -- their published example
     * is 1847 seconds -- while bounding the pathological case. Honouring an arbitrary
     * {@code Retry-After} verbatim would let a broken or hostile server park a crawler worker
     * indefinitely: the drain in {@code storeData} waits for the pool to terminate, so the whole
     * job would sit in the admin UI reporting "running" for as long as the server asked. Past the
     * cap the retry budget simply exhausts and {@code ignore_error} decides what that means,
     * which is predictable behaviour rather than an indefinite hold.</p>
     *
     * <p>Only this path is capped. The computed exponential backoff has its own
     * {@code maxDelayMillis} ceiling and is left alone.</p>
     */
    public static final long MAX_RETRY_AFTER_SECONDS = 3600L;

    /** {@link #MAX_RETRY_AFTER_SECONDS} in milliseconds. */
    public static final long MAX_RETRY_AFTER_MILLIS = MAX_RETRY_AFTER_SECONDS * 1000L;

    private static final long JITTER_FLOOR_PERCENT = 70L;

    private static final long JITTER_SPAN_PERCENT = 60L;

    private final int maxRetries;

    private final long initialDelayMillis;

    private final long maxDelayMillis;

    private final DoubleSupplier jitterSupplier;

    /**
     * Constructs a retry policy.
     *
     * @param maxRetries the number of retries after the first attempt
     * @param initialDelayMillis the delay before the first retry
     * @param maxDelayMillis the ceiling for the computed delay
     * @param jitterSupplier supplies a value in [0, 1] used to spread the delay
     */
    public RetryPolicy(final int maxRetries, final long initialDelayMillis, final long maxDelayMillis,
            final DoubleSupplier jitterSupplier) {
        this.maxRetries = maxRetries;
        this.initialDelayMillis = initialDelayMillis;
        this.maxDelayMillis = maxDelayMillis;
        this.jitterSupplier = jitterSupplier;
    }

    /**
     * Returns the policy documented in the Atlassian rate-limiting guide.
     *
     * @return the default policy
     */
    public static RetryPolicy defaults() {
        return new RetryPolicy(4, 2000L, 30000L, Math::random);
    }

    /**
     * Returns the number of retries allowed after the first attempt.
     *
     * @return the retry count
     */
    public int getMaxRetries() {
        return maxRetries;
    }

    /**
     * Returns whether a status code is worth retrying.
     *
     * @param statusCode the HTTP status code
     * @return true for 429 and 5xx
     */
    public boolean isRetryable(final int statusCode) {
        return statusCode == 429 || statusCode >= 500 && statusCode < 600;
    }

    /**
     * Returns how long to wait before the next attempt.
     *
     * <p>A {@code Retry-After} is honoured verbatim up to {@link #MAX_RETRY_AFTER_SECONDS}; beyond
     * that it is capped and the request the server actually made is logged at WARN, so an operator
     * can see why the crawl did not wait as long as it was told to.</p>
     *
     * @param attempt the zero-based retry index
     * @param retryAfterSeconds the server's Retry-After value in seconds, or null
     * @return the delay in milliseconds
     */
    public long delayMillis(final int attempt, final Long retryAfterSeconds) {
        if (retryAfterSeconds != null) {
            final long seconds = retryAfterSeconds.longValue();
            if (seconds > MAX_RETRY_AFTER_SECONDS) {
                // Compared in seconds, before multiplying: a server is free to send a value large
                // enough that "seconds * 1000" would overflow into a negative delay.
                logger.warn("Retry-After asked for {} seconds; capping at {}.", seconds, MAX_RETRY_AFTER_SECONDS);
                return MAX_RETRY_AFTER_MILLIS;
            }
            return seconds * 1000L;
        }
        long delay = initialDelayMillis;
        for (int i = 0; i < attempt && delay < maxDelayMillis; i++) {
            delay *= 2L;
        }
        if (delay > maxDelayMillis) {
            delay = maxDelayMillis;
        }
        final long jitterPercent = JITTER_FLOOR_PERCENT + (long) (jitterSupplier.getAsDouble() * JITTER_SPAN_PERCENT);
        return delay * jitterPercent / 100L;
    }
}
