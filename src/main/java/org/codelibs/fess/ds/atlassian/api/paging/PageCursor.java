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

/**
 * Continuation state for one page of a paginated Atlassian API response.
 * Exactly one of {@code token} and {@code offset} is meaningful for a given endpoint.
 *
 * @param token the opaque continuation token (JIRA {@code nextPageToken}, Confluence {@code cursor})
 * @param offset the zero-based item offset ({@code startAt} / {@code start})
 * @param hasNext whether the caller should request another page
 */
public record PageCursor(String token, Integer offset, boolean hasNext) {

    /**
     * Returns the cursor used for the very first request.
     *
     * @return the initial cursor
     */
    public static PageCursor first() {
        return new PageCursor(null, Integer.valueOf(0), true);
    }

    /**
     * Returns a cursor indicating that no further pages exist.
     *
     * @return the terminal cursor
     */
    public static PageCursor done() {
        return new PageCursor(null, null, false);
    }

    /**
     * Returns a token-based continuation cursor.
     *
     * @param token the continuation token
     * @return the cursor
     */
    public static PageCursor token(final String token) {
        return new PageCursor(token, null, true);
    }

    /**
     * Returns an offset-based continuation cursor.
     *
     * @param offset the next zero-based offset
     * @return the cursor
     */
    public static PageCursor offset(final int offset) {
        return new PageCursor(null, Integer.valueOf(offset), true);
    }

    /**
     * Returns a comparable key used to detect a cursor that fails to advance.
     *
     * @return the progress key
     */
    public String key() {
        return token != null ? "t:" + token : "o:" + offset;
    }
}
