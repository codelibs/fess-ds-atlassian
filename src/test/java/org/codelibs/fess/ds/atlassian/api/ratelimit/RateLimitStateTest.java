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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RateLimitStateTest extends UnitDsTestCase {

    /**
     * Interval used by the timing tests. Small enough to keep the suite fast (the pacing test
     * costs about 150 ms of wall clock, the sequential one about 100 ms) and large enough that a
     * "must not wait" assertion is not decided by scheduler noise.
     */
    private static final long INTERVAL_MILLIS = 100L;

    /** Interval for the multi-thread pacing test; see {@link #INTERVAL_MILLIS}. */
    private static final long PACING_INTERVAL_MILLIS = 50L;

    @Test
    public void test_plain_state_waits_only_the_read_interval() {
        final RateLimitState state = new RateLimitState(100L, 5000L);
        Assertions.assertFalse(state.isNearLimit());
        Assertions.assertEquals(100L, state.nextWaitMillis());
    }

    @Test
    public void test_near_limit_adds_the_extra_delay() {
        final RateLimitState state = new RateLimitState(100L, 5000L);
        state.observeNearLimit("true");
        Assertions.assertTrue(state.isNearLimit());
        Assertions.assertEquals(5100L, state.nextWaitMillis());
    }

    @Test
    public void test_near_limit_clears_when_the_header_stops_saying_true() {
        final RateLimitState state = new RateLimitState(100L, 5000L);
        state.observeNearLimit("true");
        state.observeNearLimit("false");
        Assertions.assertFalse(state.isNearLimit());
        Assertions.assertEquals(100L, state.nextWaitMillis());
    }

    @Test
    public void test_absent_header_clears_the_flag() {
        final RateLimitState state = new RateLimitState(100L, 5000L);
        state.observeNearLimit("TRUE");
        Assertions.assertTrue(state.isNearLimit());
        state.observeNearLimit(null);
        Assertions.assertFalse(state.isNearLimit());
    }

    @Test
    public void test_zero_read_interval_means_no_wait_when_not_near_limit() {
        final RateLimitState state = RateLimitState.of(0L);
        Assertions.assertEquals(0L, state.nextWaitMillis());
    }

    @Test
    public void test_zero_read_interval_does_not_wait() throws Exception {
        final RateLimitState state = RateLimitState.of(0L);
        final long start = System.currentTimeMillis();
        state.awaitBeforeRequest();
        state.awaitBeforeRequest();
        Assertions.assertTrue(System.currentTimeMillis() - start < 100L, "an unconfigured interval must not pace anything");
    }

    /**
     * Sequential calls advance a shared reservation: the first request through an idle gate
     * departs immediately, and the next one waits out the interval the first reserved.
     */
    @Test
    public void test_sequential_calls_advance_the_reservation() throws Exception {
        final RateLimitState state = new RateLimitState(INTERVAL_MILLIS, 0L);

        final long t0 = System.currentTimeMillis();
        state.awaitBeforeRequest();
        final long t1 = System.currentTimeMillis();
        state.awaitBeforeRequest();
        final long t2 = System.currentTimeMillis();

        Assertions.assertTrue(t1 - t0 < INTERVAL_MILLIS, "the first request through an idle gate must not wait: " + (t1 - t0) + " ms");
        Assertions.assertTrue(t2 - t1 >= INTERVAL_MILLIS,
                "the second request must wait out the slot the first reserved: " + (t2 - t1) + " ms");
    }

    /**
     * The point of sharing the state: with four threads and a 50 ms interval the client must issue
     * roughly 20 req/s, not 80. Sleeping on each calling thread makes all four sleep concurrently
     * and the whole batch finishes in about one interval.
     */
    @Test
    public void test_concurrent_calls_are_paced_through_the_gate_not_in_parallel() throws Exception {
        final int threads = 4;
        final RateLimitState state = new RateLimitState(PACING_INTERVAL_MILLIS, 0L);
        final CountDownLatch ready = new CountDownLatch(threads);
        final CountDownLatch go = new CountDownLatch(1);
        final CountDownLatch done = new CountDownLatch(threads);
        final AtomicBoolean failed = new AtomicBoolean();

        for (int i = 0; i < threads; i++) {
            final Thread t = new Thread(() -> {
                try {
                    ready.countDown();
                    go.await();
                    state.awaitBeforeRequest();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failed.set(true);
                } finally {
                    done.countDown();
                }
            });
            t.setDaemon(true);
            t.start();
        }

        Assertions.assertTrue(ready.await(5L, TimeUnit.SECONDS));
        final long start = System.currentTimeMillis();
        go.countDown();
        Assertions.assertTrue(done.await(5L, TimeUnit.SECONDS));
        final long elapsed = System.currentTimeMillis() - start;

        Assertions.assertFalse(failed.get(), "no worker may be interrupted");
        Assertions.assertTrue(elapsed >= (threads - 1) * PACING_INTERVAL_MILLIS,
                threads + " threads must pace through the gate one interval apart, taking at least "
                        + (threads - 1) * PACING_INTERVAL_MILLIS + " ms; took " + elapsed + " ms");
    }
}
