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
package org.codelibs.fess.ds.atlassian.api.paging;

import java.util.ArrayList;
import java.util.List;

import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class PaginatorTest extends UnitDsTestCase {

    @Test
    public void test_token_pagination_collects_all_pages() {
        final List<String> collected = new ArrayList<>();
        Paginator.forEach("test", cursor -> {
            if (cursor.token() == null) {
                return new Paginator.Page<>(List.of("a", "b"), PageCursor.token("p2"));
            }
            if ("p2".equals(cursor.token())) {
                return new Paginator.Page<>(List.of("c"), PageCursor.done());
            }
            throw new IllegalStateException("unexpected token: " + cursor.token());
        }, collected::add);

        Assertions.assertEquals(List.of("a", "b", "c"), collected);
    }

    @Test
    public void test_offset_pagination_collects_all_pages() {
        final List<String> collected = new ArrayList<>();
        Paginator.forEach("test", cursor -> {
            final int offset = cursor.offset() == null ? 0 : cursor.offset();
            if (offset == 0) {
                return new Paginator.Page<>(List.of("a", "b"), PageCursor.offset(2));
            }
            return new Paginator.Page<>(List.of("c"), PageCursor.done());
        }, collected::add);

        Assertions.assertEquals(List.of("a", "b", "c"), collected);
    }

    @Test
    public void test_single_page_terminates() {
        final List<String> collected = new ArrayList<>();
        Paginator.forEach("test", cursor -> new Paginator.Page<>(List.of("only"), PageCursor.done()), collected::add);
        Assertions.assertEquals(List.of("only"), collected);
    }

    @Test
    public void test_empty_result_terminates() {
        final List<String> collected = new ArrayList<>();
        Paginator.forEach("test", cursor -> new Paginator.Page<>(List.<String> of(), PageCursor.done()), collected::add);
        Assertions.assertTrue(collected.isEmpty());
    }

    /**
     * Regression guard for the JIRA infinite loop: the server ignores the paging instruction
     * and keeps returning the same page while claiming there is more.
     *
     * <p>{@code forEach} begins with {@code cursor = PageCursor.first()}, whose key is
     * {@code "o:0"}, and records that as {@code previousKey} before the loop runs. The stalled
     * fetcher below returns {@code PageCursor.offset(0)} on every call, whose key is also
     * {@code "o:0"}. So the very first fetched page's next-cursor already matches the initial
     * key, and the loop stops after a single fetch - not two.</p>
     */
    @Test
    public void test_breaks_when_cursor_does_not_advance() {
        final List<String> collected = new ArrayList<>();
        final int[] calls = { 0 };
        Paginator.forEach("test", cursor -> {
            calls[0]++;
            return new Paginator.Page<>(List.of("same"), PageCursor.offset(0));
        }, collected::add);

        Assertions.assertEquals(1, calls[0], "must stop as soon as the first next-cursor matches the initial cursor");
        Assertions.assertEquals(List.of("same"), collected);
    }

    @Test
    public void test_breaks_when_token_does_not_advance() {
        final int[] calls = { 0 };
        Paginator.forEach("test", cursor -> {
            calls[0]++;
            return new Paginator.Page<>(List.of("x"), PageCursor.token("stuck"));
        }, item -> {});

        Assertions.assertEquals(2, calls[0], "must stop on the second identical token");
    }

    @Test
    public void test_breaks_when_page_is_empty_but_claims_more() {
        final int[] calls = { 0 };
        Paginator.forEach("test", cursor -> {
            calls[0]++;
            final int offset = cursor.offset() == null ? 0 : cursor.offset();
            return new Paginator.Page<>(List.of(), PageCursor.offset(offset + 50));
        }, item -> {});

        Assertions.assertEquals(1, calls[0], "must stop as soon as an empty page claims more");
    }

    @Test
    public void test_page_cursor_key_distinguishes_token_and_offset() {
        Assertions.assertEquals("t:abc", PageCursor.token("abc").key());
        Assertions.assertEquals("o:25", PageCursor.offset(25).key());
        Assertions.assertNotEquals(PageCursor.token("1").key(), PageCursor.offset(1).key());
    }
}
