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
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Attachment;

import com.fasterxml.jackson.core.type.TypeReference;

/**
 * Request class for retrieving attachments of Confluence content.
 * Allows filtering and pagination of attachment results.
 */
public class GetAttachmentsOfContentRequest extends AtlassianRequest {

    private final String id;
    private Integer start;
    private Integer limit;
    private String filename;
    private String mediaType;
    private String[] expand;

    /**
     * Constructs a request to get attachments for the specified content ID.
     *
     * @param id the content ID
     */
    public GetAttachmentsOfContentRequest(final String id) {
        this.id = id;
    }

    /**
     * Sets the start index for pagination.
     *
     * @param start the start index
     * @return this request instance for method chaining
     */
    public GetAttachmentsOfContentRequest start(final int start) {
        this.start = start;
        return this;
    }

    /**
     * Sets the maximum number of attachments to return.
     *
     * @param limit the maximum number of results
     * @return this request instance for method chaining
     */
    public GetAttachmentsOfContentRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    /**
     * Filters attachments by filename.
     *
     * @param filename the filename to filter by
     * @return this request instance for method chaining
     */
    public GetAttachmentsOfContentRequest filename(final String filename) {
        this.filename = filename;
        return this;
    }

    /**
     * Filters attachments by media type.
     *
     * @param mediaType the media type to filter by
     * @return this request instance for method chaining
     */
    public GetAttachmentsOfContentRequest mediaType(final String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    /**
     * Specifies which properties to expand in the response.
     *
     * @param expand the properties to expand
     * @return this request instance for method chaining
     */
    public GetAttachmentsOfContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    /**
     * Executes the request and returns the response.
     *
     * @return the response containing attachments
     * @throws AtlassianDataStoreException if the request fails
     */
    public GetAttachmentsOfContentResponse execute() {
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
    public static GetAttachmentsOfContentResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetAttachmentsOfContentResponse(Collections.emptyList());
        }
        try {
            final String results = mapper.readTree(json).get("results").toString();
            return new GetAttachmentsOfContentResponse(mapper.readValue(results, new TypeReference<List<Attachment>>() {
            }));
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse attachments from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content/" + id + "/child/attachment";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (start != null) {
            queryParams.put("start", start.toString());
        }
        if (limit != null) {
            queryParams.put("limit", limit.toString());
        }
        if (filename != null) {
            queryParams.put("filename", filename);
        }
        if (mediaType != null) {
            queryParams.put("mediaType", mediaType);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetAttachmentsOfContentRequest [id=" + id + ", start=" + start + ", limit=" + limit + ", filename=" + filename
                + ", mediaType=" + mediaType + ", expand=" + Arrays.toString(expand) + "]";
    }

}
