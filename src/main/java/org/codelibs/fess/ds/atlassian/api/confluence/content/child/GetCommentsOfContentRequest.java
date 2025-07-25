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
package org.codelibs.fess.ds.atlassian.api.confluence.content.child;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request class for retrieving comments of Confluence content.
 * Allows filtering, pagination, and depth control for comment results.
 */
public class GetCommentsOfContentRequest extends AtlassianRequest {

    private final String id;
    private Integer parentVersion;
    private Integer start;
    private Integer limit;
    private String location;
    private String depth;
    private String[] expand;

    /**
     * Constructs a request to get comments for the specified content ID.
     *
     * @param id the content ID
     */
    public GetCommentsOfContentRequest(final String id) {
        this.id = id;
    }

    /**
     * Sets the parent version to retrieve comments from.
     *
     * @param parentVersion the parent version number
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest parentVersion(final int parentVersion) {
        this.parentVersion = parentVersion;
        return this;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param start the start index
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest start(final int start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the maximum number of comments to return.
     *
     * @param limit the maximum number of results
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Sets the location filter for comments.
     *
     * @param location the location to filter by
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest location(final String location) {
        this.location = location;
        return this;
    }

    /**
     * Sets the depth for retrieving nested comments.
     *
     * @param depth the depth level
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest depth(final String depth) {
        this.depth = depth;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetCommentsOfContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing comments
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetCommentsOfContentResponse execute() {
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
    public static GetCommentsOfContentResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetCommentsOfContentResponse(Collections.emptyList());
        }
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Comment> comments = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Comment>>() {
            }));
            return new GetCommentsOfContentResponse(comments);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content/" + id + "/child/comment";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (parentVersion != null) {
            queryParams.put("parentVersion", parentVersion.toString());
        }
        if (start != null) {
            queryParams.put("start", start.toString());
        }
        if (limit != null) {
            queryParams.put("limit", limit.toString());
        }
        if (location != null) {
            queryParams.put("location", location);
        }
        if (depth != null) {
            queryParams.put("depth", depth);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetCommentsOfContentRequest [id=" + id + ", parentVersion=" + parentVersion + ", start=" + start + ", limit=" + limit
                + ", location=" + location + ", depth=" + depth + ", expand=" + Arrays.toString(expand) + "]";
    }

}
