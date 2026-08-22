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

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Drives a paginated Atlassian API endpoint to exhaustion.
 *
 * <p>Atlassian silently ignores paging parameters an endpoint does not accept. Jira Cloud's
 * {@code /rest/api/3/search/jql} ignores {@code startAt} and Confluence's {@code /rest/api/search}
 * ignores {@code start}, so a loop that trusts the server to advance can spin forever or stop
 * after the first page. This driver therefore stops as soon as the cursor fails to advance or an
 * empty page claims that more results exist.</p>
 */
public final class Paginator {

    private static final Logger logger = LogManager.getLogger(Paginator.class);

    private Paginator() {
        // utility class
    }

    /**
     * One page of results plus the cursor for the following page.
     *
     * @param <T> the item type
     * @param items the items on this page
     * @param next the cursor for the next page
     */
    public record Page<T>(List<T> items, PageCursor next) {
    }

    /**
     * Fetches every page and passes each item to the consumer.
     *
     * @param <T> the item type
     * @param description a short label used in warning messages, e.g. {@code "Jira issues"}
     * @param fetcher fetches one page for the given cursor
     * @param consumer receives each item
     */
    public static <T> void forEach(final String description, final Function<PageCursor, Page<T>> fetcher, final Consumer<T> consumer) {
        PageCursor cursor = PageCursor.first();
        String previousKey = cursor.key();

        while (true) {
            final Page<T> page = fetcher.apply(cursor);
            final List<T> items = page.items() == null ? List.of() : page.items();
            items.forEach(consumer);

            final PageCursor next = page.next();
            if (next == null || !next.hasNext()) {
                return;
            }

            if (items.isEmpty()) {
                logger.warn("Stopped paging {}: the server returned an empty page but reported more results. "
                        + "The paging parameter is likely being ignored.", description);
                return;
            }

            final String nextKey = next.key();
            if (nextKey.equals(previousKey)) {
                logger.warn("Stopped paging {}: the cursor did not advance ({}). " + "The server is ignoring the paging parameter.",
                        description, nextKey);
                return;
            }

            previousKey = nextKey;
            cursor = next;
        }
    }
}
