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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Attachment;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

public class GetAttachmentsOfContentRequest extends ConfluenceRequest {

    private final String id;
    private Integer start, limit;
    private String filename, mediaType;
    private String[] expand;

    public GetAttachmentsOfContentRequest(final HttpRequestFactory httpRequestFactory, final String appHome, String id) {
        super(httpRequestFactory, appHome);
        this.id = id;
    }

    public GetAttachmentsOfContentRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetAttachmentsOfContentRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    public GetAttachmentsOfContentRequest filename(String filename) {
        this.filename = filename;
        return this;
    }

    public GetAttachmentsOfContentRequest mediaType(String mediaType) {
        this.mediaType = mediaType;
        return this;
    }

    public GetAttachmentsOfContentRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetAttachmentsOfContentResponse execute() {
        return parseResponse(getHttpResponseAsString());
    }

    public static GetAttachmentsOfContentResponse parseResponse(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        final List<Attachment> attachments = new ArrayList<>();
        try {
            final String results = mapper.readTree(json).get("results").toString();
            attachments.addAll(mapper.readValue(results, new TypeReference<List<Attachment>>() {
            }));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse attachments from: " + json, e);
        }
        return new GetAttachmentsOfContentResponse(attachments);
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/content/" + id + "/child/attachment");
        if (start != null) {
            url.put("start", start);
        }
        if (limit != null) {
            url.put("limit", limit);
        }
        if (filename != null) {
            url.put("filename", filename);
        }
        if (mediaType != null) {
            url.put("mediaType", mediaType);
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}