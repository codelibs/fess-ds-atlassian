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
package org.codelibs.fess.ds.atlassian.api.confluence;

import java.util.List;

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ContentVersionTest extends UnitDsTestCase {

    @Test
    public void test_a_malformed_date_does_not_abort_the_page() {
        final String json =
                "{\"results\":[" + "{\"content\":{\"id\":\"1\",\"title\":\"Good\",\"version\":{\"when\":\"2026-08-01T00:00:00.000Z\"}}},"
                        + "{\"content\":{\"id\":\"2\",\"title\":\"Bad\",\"version\":{\"when\":\"not a date\"}}},"
                        + "{\"content\":{\"id\":\"3\",\"title\":\"AlsoGood\",\"version\":{\"when\":\"2026-08-02T00:00:00.000Z\"}}}" + "]}";

        final List<Content> contents = GetContentsRequest.parseResponse(json).getContents();

        Assertions.assertEquals(3, contents.size(), "one unparsable date must not drop the whole page");
        Assertions.assertNotNull(contents.get(0).getLastModified());
        Assertions.assertNull(contents.get(1).getLastModified(), "the malformed one loses only its timestamp");
        Assertions.assertNotNull(contents.get(2).getLastModified());
        Assertions.assertEquals("AlsoGood", contents.get(2).getTitle());
    }
}
