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
package org.codelibs.fess.ds.atlassian.api.confluence.content.child;

import java.util.List;

import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;

/**
 * Response containing a list of comments from Confluence content.
 */
public class GetCommentsOfContentResponse {

    /** The list of comments returned by the API. */
    protected final List<Comment> comments;

    /** The cursor for the next page, or null when this is the last page. */
    protected final String nextCursor;

    /**
     * Constructs a response with the given comment list and no continuation cursor.
     *
     * @param comments the list of comments
     */
    public GetCommentsOfContentResponse(final List<Comment> comments) {
        this(comments, null);
    }

    /**
     * Constructs a response with the given comment list and continuation cursor.
     *
     * @param comments the list of comments
     * @param nextCursor the cursor for the next page, may be null
     */
    public GetCommentsOfContentResponse(final List<Comment> comments, final String nextCursor) {
        this.comments = comments;
        this.nextCursor = nextCursor;
    }

    /**
     * Returns the list of comments.
     *
     * @return the list of comments
     */
    public List<Comment> getComments() {
        return comments;
    }

    /**
     * Gets the cursor for the next page.
     *
     * @return the cursor, or null when this is the last page
     */
    public String getNextCursor() {
        return nextCursor;
    }

}
