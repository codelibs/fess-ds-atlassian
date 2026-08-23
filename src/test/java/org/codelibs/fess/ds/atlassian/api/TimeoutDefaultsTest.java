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

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class TimeoutDefaultsTest extends UnitDsTestCase {

    private static DataStoreParams base() {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", "https://example.atlassian.net");
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        return p;
    }

    @Test
    public void test_defaults_are_applied_when_unset() {
        try (JiraClient client = new JiraClient(new DataConfig(), base())) {
            Assertions.assertEquals(Integer.valueOf(20000), client.getConnectionTimeout());
            Assertions.assertEquals(Integer.valueOf(60000), client.getReadTimeout());
        }
    }

    @Test
    public void test_explicit_values_win() {
        final DataStoreParams p = base();
        p.put("connection_timeout", "1234");
        p.put("read_timeout", "5678");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals(Integer.valueOf(1234), client.getConnectionTimeout());
            Assertions.assertEquals(Integer.valueOf(5678), client.getReadTimeout());
        }
    }

    @Test
    public void test_unparsable_value_falls_back_to_the_default_without_throwing() {
        final DataStoreParams p = base();
        p.put("connection_timeout", "not-a-number");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals(Integer.valueOf(20000), client.getConnectionTimeout());
        }
    }

    @Test
    public void test_out_of_int_range_value_falls_back_to_the_default() {
        final DataStoreParams p = base();
        // A valid long, but far beyond int range. A bare (int) cast would truncate this to 0,
        // which HttpURLConnection reads as "wait forever".
        p.put("read_timeout", "4294967296");
        try (JiraClient client = new JiraClient(new DataConfig(), p)) {
            Assertions.assertEquals(Integer.valueOf(60000), client.getReadTimeout());
        }
    }
}
