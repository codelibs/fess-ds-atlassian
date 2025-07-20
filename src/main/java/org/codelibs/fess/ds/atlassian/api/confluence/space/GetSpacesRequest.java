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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

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
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request class for retrieving multiple Confluence spaces.
 * Supports filtering by space key, type, status, label, and favorite status.
 * Also supports pagination and field expansion.
 */
public class GetSpacesRequest extends AtlassianRequest {

    private String spaceKey;
    private String type;
    private String status;
    private String label;
    private String favourite;
    private String[] expand;
    private Integer start;
    private Integer limit;

    /**
     * Default constructor for GetSpacesRequest.
     */
    public GetSpacesRequest() {
        // Default constructor
    }

    /**
     * Sets the space key filter.
     *
     * @param spaceKey the space key to filter by
     * @return this request instance for method chaining
     */
    public GetSpacesRequest spaceKey(final String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    /**
     * Sets the space type filter.
     *
     * @param type the space type to filter by
     * @return this request instance for method chaining
     */
    public GetSpacesRequest type(final String type) {
        this.type = type;
        return this;
    }

    /**
     * Sets the space status filter.
     *
     * @param status the space status to filter by
     * @return this request instance for method chaining
     */
    public GetSpacesRequest status(final String status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the label filter.
     *
     * @param label the label to filter by
     * @return this request instance for method chaining
     */
    public GetSpacesRequest label(final String label) {
        this.label = label;
        return this;
    }

    /**
     * Sets the favourite filter.
     *
     * @param favourite the favourite status to filter by
     * @return this request instance for method chaining
     */
    public GetSpacesRequest favourite(final String favourite) {
        this.favourite = favourite;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetSpacesRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param start the start index
     * @return this request instance for method chaining
     */
    public GetSpacesRequest start(final int start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the maximum number of spaces to return.
     *
     * @param limit the maximum number of results
     * @return this request instance for method chaining
     */
    public GetSpacesRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing spaces
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetSpacesResponse execute() {
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
    public static GetSpacesResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetSpacesResponse(Collections.emptyList());
        }
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Space> spaces = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Space>>() {
            }));
            return new GetSpacesResponse(spaces);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse spaces from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/space";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (spaceKey != null) {
            queryParams.put("spaceKey", spaceKey);
        }
        if (type != null) {
            queryParams.put("type", type);
        }
        if (status != null) {
            queryParams.put("status", status);
        }
        if (label != null) {
            queryParams.put("label", label);
        }
        if (favourite != null) {
            queryParams.put("favourite", favourite);
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
