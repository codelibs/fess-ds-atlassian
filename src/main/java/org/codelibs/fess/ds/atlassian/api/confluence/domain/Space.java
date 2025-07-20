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

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Represents a Confluence space.
 * Contains space metadata including key, name, and description.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Space {

    /** The space key. */
    protected String key;

    /** The space name. */
    protected String name;

    /** The space description. */
    protected String description;

    /**
     * Default constructor.
     */
    public Space() {
    }

    /**
     * Gets the space key.
     *
     * @return the space key
     */
    public String getKey() {
        return key;
    }

    /**
     * Gets the space name.
     *
     * @return the space name
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the space description.
     *
     * @return the space description
     */
    public String getDescription() {
        return description;
    }

}
