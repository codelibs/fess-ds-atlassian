/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Attachment;

public class GetAttachmentsOfContentRequest extends Request {

    private final String id;
    private Integer start;
    private Integer limit;
    private String filename;
    private String mediaType;
    private String[] expand;

    public GetAttachmentsOfContentRequest(final Authentication authentication, final String appHome, final String id) {
        super(authentication, appHome);
        this.id = id;
    }

    public GetAttachmentsOfContentRequest start(final int start) {
        this.start = start;
        return this;
    }

    public GetAttachmentsOfContentRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public GetAttachmentsOfContentRequest filename(final String filename) {
        this.filename = filename;
        return this;
    }

    public GetAttachmentsOfContentRequest mediaType(final String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public GetAttachmentsOfContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetAttachmentsOfContentResponse execute() {
        return parseResponse(getCurlResponse(GET).getContentAsString());
    }

    public static GetAttachmentsOfContentResponse parseResponse(final String json) {
        try {
            final String results = mapper.readTree(json).get("results").toString();
            return new GetAttachmentsOfContentResponse(mapper.readValue(results, new TypeReference<List<Attachment>>(){}));
        } catch (IOException e) {
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

}
