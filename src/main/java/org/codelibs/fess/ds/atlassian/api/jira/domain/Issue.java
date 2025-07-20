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
 * Represents a JIRA issue.
 * Contains metadata and fields information for a JIRA issue.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Issue {

    /** The unique identifier of the issue. */
    protected String id;

    /** The self URL of the issue. */
    protected String self;

    /** The expand parameter for additional information. */
    protected String expand;

    /** The issue key (e.g., PROJECT-123). */
    protected String key;

    /** The fields data of the issue. */
    protected Fields fields;

    /**
     * Default constructor.
     */
    public Issue() {
    }

    /**
     * Gets the issue ID.
     *
     * @return the issue ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the self URL of the issue.
     *
     * @return the self URL
     */
    public String getSelf() {
        return self;
    }

    /**
     * Gets the expand parameter.
     *
     * @return the expand parameter
     */
    public String getExpand() {
        return expand;
    }

    /**
     * Gets the issue key.
     *
     * @return the issue key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the issue fields.
     *
     * @return the issue fields
     */
    public Fields getFields() {
        return fields;
    }

}
