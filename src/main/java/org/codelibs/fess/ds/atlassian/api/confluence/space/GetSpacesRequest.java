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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

public class GetSpacesRequest extends ConfluenceRequest {

    private String spaceKey;
    private String type;
    private String status;
    private String label;
    private String favourite;
    private String[] expand;
    private Integer start;
    private Integer limit;

    public GetSpacesRequest(final HttpRequestFactory httpRequestFactory, final String appHome) {
        super(httpRequestFactory, appHome);
    }

    public GetSpacesRequest spaceKey(final String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetSpacesRequest type(final String type) {
        this.type = type;
        return this;
    }

    public GetSpacesRequest status(final String status) {
        this.status = status;
        return this;
    }

    public GetSpacesRequest label(final String label) {
        this.label = label;
        return this;
    }

    public GetSpacesRequest favourite(final String favourite) {
        this.favourite = favourite;
        return this;
    }

    public GetSpacesRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetSpacesRequest start(final int start) {
        this.start = start;
        return this;
    }

    public GetSpacesRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public GetSpacesResponse execute() {
        return parseResponse(getHttpResponseAsString());
    }

    public static GetSpacesResponse parseResponse(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        final List<Space> spaces = new ArrayList<>();
        try {
            final String results = mapper.readTree(json).get("results").toString();
            spaces.addAll(mapper.readValue(results, new TypeReference<List<Space>>() {
            }));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse spaces from: " + json, e);
        }
        return new GetSpacesResponse(spaces);
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/space");
        if (spaceKey != null) {
            url.put("spaceKey", spaceKey);
        }
        if (type != null) {
            url.put("type", type);
        }
        if (status != null) {
            url.put("status", status);
        }
        if (label != null) {
            url.put("label", label);
        }
        if (favourite != null) {
            url.put("favourite", favourite);
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
