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

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class DeploymentTest extends UnitDsTestCase {

    @Test
    public void test_detect_cloud_from_atlassian_net() {
        Assertions.assertEquals(Deployment.CLOUD, Deployment.detect("https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.CLOUD, Deployment.detect("https://example.atlassian.net/wiki"));
        Assertions.assertEquals(Deployment.CLOUD, Deployment.detect("https://EXAMPLE.ATLASSIAN.NET/"));
    }

    @Test
    public void test_detect_datacenter_for_other_hosts() {
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect("https://jira.example.com"));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect("https://wiki.example.com/confluence"));
    }

    @Test
    public void test_detect_datacenter_for_unparsable_home() {
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect(""));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect(null));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect("not a url"));
    }

    @Test
    public void test_detect_does_not_match_lookalike_host() {
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect("https://evil-atlassian.net"));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.detect("https://atlassian.net.example.com"));
    }

    @Test
    public void test_of_honours_explicit_value() {
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.of("datacenter", "https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.of("DATA_CENTER", "https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.of("dc", "https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.CLOUD, Deployment.of("cloud", "https://jira.example.com"));
    }

    @Test
    public void test_of_falls_back_to_detection_when_blank() {
        Assertions.assertEquals(Deployment.CLOUD, Deployment.of(null, "https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.CLOUD, Deployment.of("", "https://example.atlassian.net"));
        Assertions.assertEquals(Deployment.DATA_CENTER, Deployment.of("  ", "https://jira.example.com"));
    }

    @Test
    public void test_of_rejects_invalid_value() {
        Assertions.assertThrows(AtlassianDataStoreException.class, () -> Deployment.of("server", "https://jira.example.com"));
    }
}
