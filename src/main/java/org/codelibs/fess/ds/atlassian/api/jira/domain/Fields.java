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
 * Represents the fields of a JIRA issue.
 * Contains essential information like summary, description, update time, and comments.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Fields {

    /** The summary of the issue. */
    protected String summary;

    /** The last updated timestamp of the issue. */
    protected String updated;

    /** The description of the issue. */
    protected String description;

    /** The comments associated with the issue. */
    protected Comments comment;

    /**
     * Default constructor.
     */
    public Fields() {
    }

    /**
     * Gets the issue summary.
     *
     * @return the issue summary
     */
    public String getSummary() {
        return summary;
    }

    /**
     * Gets the last updated timestamp.
     *
     * @return the last updated timestamp
     */
    public String getUpdated() {
        return updated;
    }

    /**
     * Gets the issue description.
     *
     * @return the issue description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Gets the issue comments.
     *
     * @return the issue comments
     */
    public Comments getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "Fields [summary=" + summary + ", updated=" + updated + ", description=" + description + "]";
    }

}
