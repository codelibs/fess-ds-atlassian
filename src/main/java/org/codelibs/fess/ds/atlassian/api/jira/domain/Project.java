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
 * Represents a JIRA project.
 * Contains project metadata including ID, key, name, and description.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Project {

    /** The self URL of the project. */
    protected String self;

    /** The project key. */
    protected String key;

    /** The unique identifier of the project. */
    protected Long id;

    /** The project name. */
    protected String name;

    /** The project description. */
    protected String description;

    /**
     * Default constructor.
     */
    public Project() {
    }

    /**
     * Gets the self URL of the project.
     *
     * @return the self URL
     */
    public String getSelf() {
        return self;
    }

    /**
     * Gets the project key.
     *
     * @return the project key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the project name.
     *
     * @return the project name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the project ID.
     *
     * @return the project ID
     */
    public Long getId() {
        return id;
    }

    /**
     * Gets the project description.
     *
     * @return the project description
     */
    public String getDescription() {
        return description;
    }

}
