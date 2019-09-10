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
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;

public class GetCommentsOfContentRequest extends ConfluenceRequest {

    private final String id;
    private Integer parentVersion, start, limit;
    private String location, depth;
    private String[] expand;

    public GetCommentsOfContentRequest(final HttpRequestFactory httpRequestFactory, final String appHome, String id) {
        super(httpRequestFactory, appHome);
        this.id = id;
    }

    @Override
    public GetCommentsOfContentResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(appHome(), id, parentVersion, start, limit, location, depth, expand);
        try {
            final HttpRequest request = request().buildGetRequest(url);
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

    public GetCommentsOfContentRequest parentVersion(int parentVersion) {
        this.parentVersion = parentVersion;
        return this;
    }

    public GetCommentsOfContentRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetCommentsOfContentRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

    public GetCommentsOfContentRequest location(String location) {
        this.location = location;
        return this;
    }

    public GetCommentsOfContentRequest depth(String depth) {
        this.depth = depth;
        return this;
    }

    public GetCommentsOfContentRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public static GetCommentsOfContentResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        List<Comment> comments = new ArrayList<>();
        try {
            String results = mapper.readTree(json).get("results").toString();
            comments.addAll(mapper.readValue(results, new TypeReference<List<Comment>>() {
            }));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: " + json, e);
        }
        return new GetCommentsOfContentResponse(comments);
    }

    protected GenericUrl buildUrl(final String confluenceHome, final String id, final Integer parentVersion, final Integer start,
            final Integer limit, final String location, final String depth, final String[] expand) {
        final GenericUrl url = new GenericUrl(confluenceHome + "/rest/api/latest/content/" + id + "/child/comment");
        if (parentVersion != null) {
            url.put("parentVersion", parentVersion);
        }
        if (start != null) {
            url.put("start", start);
        }
        if (limit != null) {
            url.put("limit", limit);
        }
        if (location != null) {
            url.put("location", location);
        }
        if (depth != null) {
            url.put("depth", depth);
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}