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
package org.codelibs.fess.ds.atlassian.api.confluence.content;

import java.util.List;

import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

/**
 * Response for GetContentsRequest containing a list of Confluence content.
 */
public class GetContentsResponse {

    /** The list of content items. */
    protected final List<Content> contents;

    /** The cursor for the next page, or null when this is the last page. */
    protected final String nextCursor;

    /** The offset the endpoint actually served, echoed back from the request. */
    protected final Integer start;

    /**
     * Constructs a response with the given content list and no continuation cursor.
     *
     * @param contents the list of content items
     */
    public GetContentsResponse(final List<Content> contents) {
        this(contents, null);
    }

    /**
     * Constructs a response with the given content list and continuation cursor.
     *
     * @param contents the list of content items
     * @param nextCursor the cursor for the next page, may be null
     */
    public GetContentsResponse(final List<Content> contents, final String nextCursor) {
        this(contents, nextCursor, null);
    }

    /**
     * Constructs a response with the given content list, continuation cursor and served offset.
     *
     * @param contents the list of content items
     * @param nextCursor the cursor for the next page, may be null
     * @param start the offset the endpoint echoed back, may be null
     */
    public GetContentsResponse(final List<Content> contents, final String nextCursor, final Integer start) {
        this.contents = contents;
        this.nextCursor = nextCursor;
        this.start = start;
    }

    /**
     * Gets the list of content items.
     *
     * @return the list of content items
     */
    public List<Content> getContents() {
        return contents;
    }

    /**
     * Gets the cursor for the next page.
     *
     * @return the cursor, or null when this is the last page
     */
    public String getNextCursor() {
        return nextCursor;
    }

    /**
     * Gets the offset the endpoint reported serving.
     *
     * @return the offset, or null when the endpoint did not echo one
     */
    public Integer getStart() {
        return start;
    }

}
