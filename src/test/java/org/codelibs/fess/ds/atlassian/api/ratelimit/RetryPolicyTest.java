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

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RetryPolicyTest extends UnitDsTestCase {

    /** Jitter fixed at the midpoint, so the factor is exactly 1.0. */
    private static RetryPolicy fixed() {
        return new RetryPolicy(4, 2000L, 30000L, () -> 0.5d);
    }

    @Test
    public void test_defaults_match_the_documented_values() {
        final RetryPolicy policy = RetryPolicy.defaults();
        Assertions.assertEquals(4, policy.getMaxRetries());
    }

    @Test
    public void test_backoff_doubles_from_the_initial_delay() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(2000L, policy.delayMillis(0, null));
        Assertions.assertEquals(4000L, policy.delayMillis(1, null));
        Assertions.assertEquals(8000L, policy.delayMillis(2, null));
        Assertions.assertEquals(16000L, policy.delayMillis(3, null));
    }

    @Test
    public void test_backoff_is_capped() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(30000L, policy.delayMillis(4, null));
        Assertions.assertEquals(30000L, policy.delayMillis(20, null));
    }

    @Test
    public void test_jitter_spans_seventy_to_one_hundred_thirty_percent() {
        Assertions.assertEquals(1400L, new RetryPolicy(4, 2000L, 30000L, () -> 0.0d).delayMillis(0, null));
        Assertions.assertEquals(2600L, new RetryPolicy(4, 2000L, 30000L, () -> 1.0d).delayMillis(0, null));
    }

    /** Retry-After is the server's instruction; it wins over any computed backoff. */
    @Test
    public void test_retry_after_is_honoured_verbatim() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(1847000L, policy.delayMillis(0, Long.valueOf(1847L)));
        Assertions.assertEquals(1000L, policy.delayMillis(3, Long.valueOf(1L)));
        Assertions.assertEquals(0L, policy.delayMillis(0, Long.valueOf(0L)));
    }

    /**
     * The Finding-1 drain waits for the pool to terminate, so an uncapped Retry-After would park
     * the whole job -- reported as "running" in the admin UI -- for as long as the server asked.
     * Past the cap the retry budget exhausts normally and ignore_error decides what that means.
     */
    @Test
    public void test_retry_after_above_the_cap_is_capped() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(3600000L, policy.delayMillis(0, Long.valueOf(3601L)));
        Assertions.assertEquals(3600000L, policy.delayMillis(0, Long.valueOf(86400L)));
        Assertions.assertEquals(3600000L, policy.delayMillis(2, Long.valueOf(86400L)));
        Assertions.assertEquals(3600000L, RetryPolicy.MAX_RETRY_AFTER_MILLIS);
    }

    /**
     * The cap must not creep down onto legitimate values: exactly one hour is still honoured
     * verbatim, and so is Atlassian's documented 1847 s (pinned by
     * {@link #test_retry_after_is_honoured_verbatim()}, which is deliberately left unedited).
     */
    @Test
    public void test_retry_after_at_or_below_the_cap_is_verbatim() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(3600000L, policy.delayMillis(0, Long.valueOf(3600L)));
        Assertions.assertEquals(3599000L, policy.delayMillis(0, Long.valueOf(3599L)));
    }

    /**
     * A server is free to send a Retry-After large enough that "seconds * 1000" overflows into a
     * negative delay, which would be skipped rather than waited. The comparison happens in
     * seconds, before the multiplication, so the cap holds.
     */
    @Test
    public void test_an_overflowing_retry_after_is_capped_not_wrapped() {
        final RetryPolicy policy = fixed();
        Assertions.assertEquals(3600000L, policy.delayMillis(0, Long.valueOf(Long.MAX_VALUE)));
    }

    @Test
    public void test_retryable_statuses() {
        final RetryPolicy policy = fixed();
        Assertions.assertTrue(policy.isRetryable(429));
        Assertions.assertTrue(policy.isRetryable(500));
        Assertions.assertTrue(policy.isRetryable(503));
        Assertions.assertFalse(policy.isRetryable(200));
        Assertions.assertFalse(policy.isRetryable(400));
        Assertions.assertFalse(policy.isRetryable(401));
        Assertions.assertFalse(policy.isRetryable(404));
    }
}
