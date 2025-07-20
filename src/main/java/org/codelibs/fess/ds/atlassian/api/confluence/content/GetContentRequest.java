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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Request class for retrieving Confluence content.
 * Allows specification of status, version, and fields to expand.
 */
public class GetContentRequest extends AtlassianRequest {

    private final String id;
    private String status;
    private Integer version;
    private String[] expand;

    /**
     * Constructs a request to get Confluence content with the specified ID.
     *
     * @param id the content ID
     */
    public GetContentRequest(final String id) {
        this.id = id;
    }

    /**
     * Sets the status filter for the content.
     *
     * @param status the status to filter by
     * @return this request instance for method chaining
     */
    public GetContentRequest status(final String status) {
        this.status = status;
        return this;
    }

    /**
     * Sets the specific version of the content to retrieve.
     *
     * @param version the version number
     * @return this request instance for method chaining
     */
    public GetContentRequest version(final int version) {
        this.version = version;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing the content
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetContentResponse execute() {
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
    public static GetContentResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetContentResponse(null);
        }
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return new GetContentResponse(mapper.readValue(json, Content.class));
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse content from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content/" + id;
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (status != null) {
            queryParams.put("status", status);
        }
        if (version != null) {
            queryParams.put("version", version.toString());
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetContentRequest [id=" + id + ", status=" + status + ", version=" + version + ", expand=" + Arrays.toString(expand) + "]";
    }

}
