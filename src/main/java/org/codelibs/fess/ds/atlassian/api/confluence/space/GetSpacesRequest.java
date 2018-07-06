/*
 * Copyright 2012-2018 CodeLibs Project and the Others.
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;

public class GetSpacesRequest extends ConfluenceRequest {

    private String spaceKey, type, status, label, favourite;
    private String[] expand;
    private Integer start, limit;

    public GetSpacesRequest(ConfluenceClient confluenceClient) {
        super(confluenceClient);
    }

    @Override
    public GetSpacesResponse execute() {
        try {
            final HttpRequest request = confluenceClient.request().buildGetRequest(buildUrl(
                    confluenceClient.confluenceHome(), spaceKey, type, status, label, favourite, expand, start, limit));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> map = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> spaces = (List<Map<String, Object>>) map.get("results");
            return new GetSpacesResponse(spaces);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GetSpacesRequest spaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetSpacesRequest type(String type) {
        this.type = type;
        return this;
    }

    public GetSpacesRequest status(String status) {
        this.status = status;
        return this;
    }

    public GetSpacesRequest label(String label) {
        this.label = label;
        return this;
    }

    public GetSpacesRequest favourite(String favourite) {
        this.favourite = favourite;
        return this;
    }

    public GetSpacesRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetSpacesRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetSpacesRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    protected GenericUrl buildUrl(final String confluenceHome, final String spaceKey, final String type,
            final String status, final String label, final String favourite, final String[] expand, final Integer start,
            final Integer limit) {
        final GenericUrl url = new GenericUrl(confluenceHome + "/rest/api/latest/space");
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