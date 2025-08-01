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

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Project;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request class for retrieving all JIRA projects.
 * Supports expansion of project properties and filtering by recent projects.
 */
public class GetProjectsRequest extends AtlassianRequest {

    private String[] expand;
    private Integer recent;

    /**
     * Default constructor for GetProjectsRequest.
     */
    public GetProjectsRequest() {
        // Default constructor
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetProjectsRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Sets the number of recent projects to return.
     *
     * @param recent the number of recent projects
     * @return this request instance for method chaining
     */
    public GetProjectsRequest recent(final int recent) {
        this.recent = recent;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing projects
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetProjectsResponse execute() {
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    /**
     * Parses the JSON response into a response object.
     *
     * @param json the JSON response string
     * @return the parsed response
     * @throws AtlassianDataStoreException if parsing fails
     */
    public static GetProjectsResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetProjectsResponse(Collections.emptyList());
        }
        try {
            return new GetProjectsResponse(mapper.readValue(json, new TypeReference<List<Project>>() {
            }));
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse projects from: \"" + json + "\"", e);
        }
    }

    @Override
    public String getURL() {
        return appHome() + "/rest/api/latest/project";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        if (recent != null) {
            queryParams.put("recent", recent.toString());
        }
        return queryParams;
    }

}
