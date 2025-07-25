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

    /**
     * Constructs a response with the given list of comments.
     *
     * @param comments the list of comments
     */
    public GetCommentsOfContentResponse(final List<Comment> comments) {
        this.comments = comments;
    }

    /**
     * Returns the list of comments.
     *
     * @return the list of comments
     */
    public List<Comment> getComments() {
        return comments;
    }

}
