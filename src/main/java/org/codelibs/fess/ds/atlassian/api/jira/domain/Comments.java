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

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a collection of comments in JIRA.
 * This class contains the total count and list of comments.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Comments {

    /** The total number of comments. */
    protected Long total;

    /** The list of comment objects. */
    protected List<Comment> comments;

    /**
     * Default constructor.
     */
    public Comments() {
    }

    /**
     * Gets the total number of comments.
     *
     * @return the total number of comments
     */
    public Long getTotal() {
        return total;
    }

    /**
     * Gets the list of comments.
     *
     * @return the list of comments
     */
    public List<Comment> getComments() {
        return comments;
    }

}
