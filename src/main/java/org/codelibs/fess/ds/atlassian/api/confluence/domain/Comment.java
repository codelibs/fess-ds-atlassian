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
package org.codelibs.fess.ds.atlassian.api.confluence.domain;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a comment on Confluence content.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment {

    /** The title of the comment. */
    protected String title;

    /** The body content of the comment. */
    @JsonIgnore
    protected String body;

    /**
     * Default constructor for Comment.
     */
    public Comment() {
        // Default constructor
    }

    /**
     * Gets the comment title.
     *
     * @return the comment title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the comment body.
     *
     * @return the comment body
     */
    public String getBody() {
        return body;
    }

    /**
     * Unpacks the body content from the API response.
     *
     * @param body the body data from API response
     */
    @JsonProperty("body")
    public void unpackBody(final Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> view = (Map<String, Object>) body.get("view");
        final String value = (String) view.get("value");
        this.body = value;
    }

}
