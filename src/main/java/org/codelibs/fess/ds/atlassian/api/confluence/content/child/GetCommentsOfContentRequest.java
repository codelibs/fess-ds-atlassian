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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;

import com.fasterxml.jackson.core.type.TypeReference;

public class GetCommentsOfContentRequest extends Request {

    private final String id;
    private Integer parentVersion;
    private Integer start;
    private Integer limit;
    private String location;
    private String depth;
    private String[] expand;

    public GetCommentsOfContentRequest(final Authentication authentication, final String appHome, final String id) {
        super(authentication, appHome);
        this.id = id;
    }

    public GetCommentsOfContentRequest parentVersion(final int parentVersion) {
        this.parentVersion = parentVersion;
        return this;
    }

    public GetCommentsOfContentRequest start(final int start) {
        this.start = start;
        return this;
    }

    public GetCommentsOfContentRequest limit(final int limit) {
        this.limit = limit;
        return this;
    }

    public GetCommentsOfContentRequest location(final String location) {
        this.location = location;
        return this;
    }

    public GetCommentsOfContentRequest depth(final String depth) {
        this.depth = depth;
        return this;
    }

    public GetCommentsOfContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetCommentsOfContentResponse execute() {
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    public static GetCommentsOfContentResponse parseResponse(final String json) {
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Comment> comments = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Comment>>(){}));
            return new GetCommentsOfContentResponse(comments);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content/" + id + "/child/comment";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (parentVersion != null) {
            queryParams.put("parentVersion", parentVersion.toString());
        }
        if (start != null) {
            queryParams.put("start", start.toString());
        }
        if (limit != null) {
            queryParams.put("limit", limit.toString());
        }
        if (location != null) {
            queryParams.put("location", location);
        }
        if (depth != null) {
            queryParams.put("depth", depth);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetCommentsOfContentRequest [id=" + id + ", parentVersion=" + parentVersion + ", start=" + start + ", limit=" + limit
                + ", location=" + location + ", depth=" + depth + ", expand=" + Arrays.toString(expand) + "]";
    }

}
