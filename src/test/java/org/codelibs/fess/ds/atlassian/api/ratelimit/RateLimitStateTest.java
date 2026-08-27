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

public class RateLimitStateTest extends UnitDsTestCase {

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
}
