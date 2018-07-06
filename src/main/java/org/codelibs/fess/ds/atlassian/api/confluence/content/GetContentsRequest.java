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
package org.codelibs.fess.ds.atlassian.api.confluence.content;

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

public class GetContentsRequest extends ConfluenceRequest {

    private String type, spaceKey, title, status, postingDay;
    private String[] expand;
    private Integer start, limit;

    public GetContentsRequest(ConfluenceClient confluenceClient) {
        super(confluenceClient);
    }

    @Override
    public GetContentsResponse execute() {
        try {
            final HttpRequest request = confluenceClient.request()
                    .buildGetRequest(buildUrl(confluenceClient.confluenceHome(), type, spaceKey, title, status,
                            postingDay, expand, start, limit));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> map = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> contents = (List<Map<String, Object>>) map.get("results");
            return new GetContentsResponse(contents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GetContentsRequest type(String type) {
        this.type = type;
        return this;
    }

    public GetContentsRequest spaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetContentsRequest title(String title) {
        this.title = title;
        return this;
    }

    public GetContentsRequest status(String status) {
        this.status = status;
        return this;
    }

    public GetContentsRequest postingDay(String postingDay) {
        this.postingDay = postingDay;
        return this;
    }

    public GetContentsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetContentsRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetContentsRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    protected GenericUrl buildUrl(final String jiraHome, final String type, final String spaceKey, final String title,
            final String status, final String postingDay, final String[] expand, final Integer start,
            final Integer limit) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/content");
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