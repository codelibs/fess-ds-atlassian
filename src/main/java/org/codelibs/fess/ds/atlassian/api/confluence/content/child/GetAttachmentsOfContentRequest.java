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
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

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

    public GetAttachmentsOfContentRequest(ConfluenceClient confluenceClient, String id) {
        super(confluenceClient);
        this.id = id;
    }

    @Override
    public GetAttachmentsOfContentResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(confluenceClient.confluenceHome(), id, start, limit, filename, mediaType, expand);
        try {
            final HttpRequest request = confluenceClient.request().buildGetRequest(url);
            final HttpResponse response = request.execute();
            if (response.getStatusCode() != 200) {
                throw new HttpResponseException(response);
            }
            final Scanner s = new Scanner(response.getContent());
            s.useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            s.close();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new AtlassianDataStoreException(
                        "There is no content with the given id, or the calling user does not have permission to view the content: " + id,
                        e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
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

    public static GetAttachmentsOfContentResponse fromJson(String json) {
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

    protected GenericUrl buildUrl(final String confluenceHome, final String id, final Integer start, final Integer limit,
            final String filename, final String mediaType, final String[] expand) {
        final GenericUrl url = new GenericUrl(confluenceHome + "/rest/api/latest/content/" + id + "/child/attachment");
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