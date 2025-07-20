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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comments;

/**
 * Request class for retrieving comments from a JIRA issue.
 * Supports pagination, ordering, and field expansion.
 */
public class GetCommentsRequest extends AtlassianRequest {

    private final String issueIdOrKey;
    private Long startAt;
    private Integer maxResults;
    private String orderBy;
    private String[] expand;

    /**
     * Constructs a request to get comments for the specified JIRA issue.
     *
     * @param issueIdOrKey the issue ID or key
     */
    public GetCommentsRequest(final String issueIdOrKey) {
        this.issueIdOrKey = issueIdOrKey;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param startAt the start index
     * @return this request instance for method chaining
     */
    public GetCommentsRequest startAt(final long startAt) {
        this.startAt = startAt;
        return this;
    }

    /**
     * Sets the maximum number of comments to return.
     *
     * @param maxResults the maximum number of results
     * @return this request instance for method chaining
     */
    public GetCommentsRequest maxResults(final int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    /**
     * Sets the ordering for the comments.
     *
     * @param orderBy the field to order by
     * @return this request instance for method chaining
     */
    public GetCommentsRequest orderBy(final String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetCommentsRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing comments
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetCommentsResponse execute() {
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
    public static GetCommentsResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetCommentsResponse(Collections.emptyList());
        }
        try {
            return new GetCommentsResponse(mapper.readValue(json, Comments.class).getComments());
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: \"" + json + "\"", e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/issue/" + issueIdOrKey + "/comment";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (startAt != null) {
            queryParams.put("startAt", startAt.toString());
        }
        if (maxResults != null) {
            queryParams.put("maxResults", maxResults.toString());
        }
        if (orderBy != null) {
            queryParams.put("orderBy", orderBy);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetCommentsRequest [issueIdOrKey=" + issueIdOrKey + ", startAt=" + startAt + ", maxResults=" + maxResults + ", orderBy="
                + orderBy + ", expand=" + Arrays.toString(expand) + "]";
    }
}
