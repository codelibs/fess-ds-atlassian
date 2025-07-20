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
package org.codelibs.fess.ds.atlassian.api.jira.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a comment on a JIRA issue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comment {

    /** The body content of the comment. */
    protected String body;

    /**
     * Default constructor for Comment.
     */
    public Comment() {
        // Default constructor
    }

    /**
     * Gets the comment body.
     *
     * @return the comment body
     */
    public String getBody() {
        return body;
    }

}
