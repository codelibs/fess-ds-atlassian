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
package org.codelibs.fess.ds.atlassian.api.confluence.content;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

public class GetContentsRequest extends ConfluenceRequest {

    private String type;
    private String spaceKey;
    private String title;
    private String status;
    private String postingDay;
    private String[] expand;
    private Integer start;
    private Integer limit;

    public GetContentsRequest(final HttpRequestFactory httpRequestFactory, final String appHome) {
        super(httpRequestFactory, appHome);
    }

    public GetContentsRequest type(final String type) {
        this.type = type;
        return this;
    }

    public GetContentsRequest spaceKey(final String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetContentsRequest title(final String title) {
        this.title = title;
        return this;
    }

    public GetContentsRequest status(final String status) {
        this.status = status;
        return this;
    }

    public GetContentsRequest postingDay(final String postingDay) {
        this.postingDay = postingDay;
        return this;
    }

    public GetContentsRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetContentsRequest start(final int start) {
        this.start = start;
        return this;
    }

    public GetContentsRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public GetContentsResponse execute() {
        return parseResponse(getHttpResponseAsString(GET));
    }

    public static GetContentsResponse parseResponse(final String json) {
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Content> contents = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Content>>(){}));
            return new GetContentsResponse(contents);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse contents from: " + json, e);
        }
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/content");
        if (type != null) {
            url.put("type", type);
        }
        if (spaceKey != null) {
            url.put("spaceKey", spaceKey);
        }
        if (title != null) {
            url.put("title", title);
        }
        if (status != null) {
            url.put("status", status);
        }
        if (postingDay != null) {
            url.put("postingDay", postingDay);
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        if (start != null) {
            url.put("start", start);
        }
        if (limit != null) {
            url.put("limit", limit);
        }
        return url;
    }

}
