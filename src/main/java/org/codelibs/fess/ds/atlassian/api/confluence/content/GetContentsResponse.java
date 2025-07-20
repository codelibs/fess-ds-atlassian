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

    /**
     * Constructs a response with the given content list.
     *
     * @param contents the list of content items
     */
    public GetContentsResponse(final List<Content> contents) {
        this.contents = contents;
    }

    /**
     * Gets the list of content items.
     *
     * @return the list of content items
     */
    public List<Content> getContents() {
        return contents;
    }

}
