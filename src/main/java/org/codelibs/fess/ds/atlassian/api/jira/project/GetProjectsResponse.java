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
package org.codelibs.fess.ds.atlassian.api.jira.project;

import java.util.List;

import org.codelibs.fess.ds.atlassian.api.jira.domain.Project;

/**
 * Response containing a list of JIRA projects.
 */
public class GetProjectsResponse {

    /** The list of projects returned by the API. */
    protected final List<Project> projects;

    /**
     * Constructs a response with the given list of projects.
     *
     * @param projects the list of projects
     */
    public GetProjectsResponse(final List<Project> projects) {
        this.projects = projects;
    }

    /**
     * Returns the list of projects.
     *
     * @return the list of projects
     */
    public List<Project> getProjects() {
        return projects;
    }

}
