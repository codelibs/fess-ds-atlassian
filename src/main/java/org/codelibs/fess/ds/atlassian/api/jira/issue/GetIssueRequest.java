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
package org.codelibs.fess.ds.atlassian.api.jira.issue;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request class for retrieving a JIRA issue.
 * Allows specification of fields, properties to expand, and custom properties.
 */
public class GetIssueRequest extends AtlassianRequest {

    private final String issueIdOrKey;
    private String[] fields;
    private String[] expand;
    private String[] properties;

    /**
     * Constructs a request to get a specific JIRA issue.
     *
     * @param issueIdOrKey the issue ID or key
     */
    public GetIssueRequest(final String issueIdOrKey) {
        this.issueIdOrKey = issueIdOrKey;
    }

    /**
     * Specifies which fields to include in the response.
     *
     * @param fields the fields to include
     * @return this request instance for method chaining
     */
    public GetIssueRequest fields(final String... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetIssueRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Specifies which custom properties to include in the response.
     *
     * @param properties the properties to include
     * @return this request instance for method chaining
     */
    public GetIssueRequest properties(final String... properties) {
        this.properties = properties;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing the issue
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetIssueResponse execute() {
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
    public static GetIssueResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetIssueResponse(null);
        }
        try {
            return new GetIssueResponse(mapper.readValue(json, new TypeReference<Issue>() {
            }));
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse issue from: \"" + json + "\"", e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/issue/" + issueIdOrKey;
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (fields != null) {
            queryParams.put("fields", String.join(",", fields));
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        if (properties != null) {
            queryParams.put("properties", String.join(",", properties));
        }
        return queryParams;
    }

}
