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
package org.codelibs.fess.ds.atlassian.api.confluence.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request for retrieving Confluence content using the REST API.
 * Supports filtering by type, space, title, status, and pagination.
 */
public class GetContentsRequest extends AtlassianRequest {

    /** The content type filter (e.g., page, blogpost). */
    private String type;

    /** The space key filter. */
    private String spaceKey;

    /** The title filter. */
    private String title;

    /** The status filter. */
    private String status;

    /** The posting day filter. */
    private String postingDay;

    /** The expand parameters for additional data. */
    private String[] expand;

    /** The start index for pagination. */
    private Integer start;

    /** The limit for pagination. */
    private Integer limit;

    /**
     * Default constructor.
     */
    public GetContentsRequest() {
    }

    /**
     * Sets the content type filter.
     *
     * @param type the content type
     * @return this request instance for method chaining
     */
    public GetContentsRequest type(final String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the space key filter.
     *
     * @param spaceKey the space key
     * @return this request instance for method chaining
     */
    public GetContentsRequest spaceKey(final String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    /**
     * Sets the title filter.
     *
     * @param title the title
     * @return this request instance for method chaining
     */
    public GetContentsRequest title(final String title) {
        this.title = title;
        return this;
    }

    /**
     * Sets the status filter.
     *
     * @param status the status
     * @return this request instance for method chaining
     */
    public GetContentsRequest status(final String status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the posting day filter.
     *
     * @param postingDay the posting day
     * @return this request instance for method chaining
     */
    public GetContentsRequest postingDay(final String postingDay) {
        this.postingDay = postingDay;
        return this;
    }

    /**
     * Sets the expand parameters for additional data.
     *
     * @param expand the expand parameters
     * @return this request instance for method chaining
     */
    public GetContentsRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param start the start index
     * @return this request instance for method chaining
     */
    public GetContentsRequest start(final int start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the limit for pagination.
     *
     * @param limit the limit
     * @return this request instance for method chaining
     */
    public GetContentsRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing content list
     */
    public GetContentsResponse execute() {
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
     * Parses the JSON response into a GetContentsResponse object.
     *
     * @param json the JSON response string
     * @return the parsed response
     */
    public static GetContentsResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetContentsResponse(Collections.emptyList());
        }
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Content> contents = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Content>>() {
            }));
            return new GetContentsResponse(contents);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse contents from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (type != null) {
            queryParams.put("type", type);
        }
        if (spaceKey != null) {
            queryParams.put("spaceKey", spaceKey);
        }
        if (title != null) {
            queryParams.put("title", title);
        }
        if (status != null) {
            queryParams.put("status", status);
        }
        if (postingDay != null) {
            queryParams.put("postingDay", postingDay);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        if (start != null) {
            queryParams.put("start", start.toString());
        }
        if (limit != null) {
            queryParams.put("limit", limit.toString());
        }
        return queryParams;
    }

}
