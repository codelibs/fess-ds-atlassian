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
package org.codelibs.fess.ds.atlassian.api.jira.search;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;

/**
 * Request class for searching JIRA issues using JQL (JIRA Query Language).
 * Supports pagination, field selection, query validation, and result expansion.
 */
public class SearchRequest extends AtlassianRequest {

    private String jql;
    private Integer startAt;
    private Integer maxResults;
    private Boolean validateQuery;
    private String[] fields;
    private String[] expand;

    /**
     * Default constructor for SearchRequest.
     */
    public SearchRequest() {
        // Default constructor
    }

    /**
     * Sets the JQL query string.
     *
     * @param jql the JQL query
     * @return this request instance for method chaining
     */
    public SearchRequest jql(final String jql) {
        this.jql = jql;
        return this;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param startAt the start index
     * @return this request instance for method chaining
     */
    public SearchRequest startAt(final int startAt) {
        this.startAt = startAt;
        return this;
    }

    /**
     * Sets the maximum number of results to return.
     *
     * @param maxResults the maximum number of results
     * @return this request instance for method chaining
     */
    public SearchRequest maxResults(final int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    /**
     * Sets whether to validate the JQL query.
     *
     * @param validateQuery true to validate the query, false otherwise
     * @return this request instance for method chaining
     */
    public SearchRequest validateQuery(final boolean validateQuery) {
        this.validateQuery = validateQuery;
        return this;
    }

    /**
     * Specifies which fields to include in the response.
     *
     * @param fields the fields to include
     * @return this request instance for method chaining
     */
    public SearchRequest fields(final String... fields) {
        this.fields = fields;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public SearchRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Parses the JSON response into a response object.
     *
     * @param json the JSON response string
     * @return the parsed response
     * @throws AtlassianDataStoreException if parsing fails
     */
    public static SearchResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return SearchResponse.create(Collections.emptyList());
        }
        try {
            return mapper.readValue(json, SearchResponse.class);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse: \"" + json + "\"", e);
        }
    }

    /**
     * Executes the search request and returns the response.
     *
     * @return the response containing search results
     * @throws AtlassianDataStoreException if the request fails
     */
    public SearchResponse execute() {
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/search";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (jql != null && !jql.isEmpty()) {
            queryParams.put("jql", jql);
        }
        if (startAt != null) {
            queryParams.put("startAt", startAt.toString());
        }
        if (maxResults != null) {
            queryParams.put("maxResults", maxResults.toString());
        }
        if (validateQuery != null) {
            queryParams.put("validateQuery", validateQuery.toString());
        }
        if (fields != null) {
            queryParams.put("fields", String.join(",", fields));
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

}
