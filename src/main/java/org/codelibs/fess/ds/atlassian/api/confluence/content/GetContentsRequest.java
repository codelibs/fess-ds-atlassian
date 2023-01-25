/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.AtlassianRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

import com.fasterxml.jackson.core.type.TypeReference;

public class GetContentsRequest extends AtlassianRequest {

    private String type;
    private String spaceKey;
    private String title;
    private String status;
    private String postingDay;
    private String[] expand;
    private Integer start;
    private Integer limit;

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
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (final Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    public static GetContentsResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetContentsResponse(Collections.emptyList());
        }
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Content> contents = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Content>>() {
            }));
            return new GetContentsResponse(contents);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse contents from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/content";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (type != null) {
            queryParams.put("type", type);
        }
        if (spaceKey != null) {
            queryParams.put("spaceKey", spaceKey);
        }
        if (title != null) {
            queryParams.put("title", title);
        }
        if (status != null) {
            queryParams.put("status", status);
        }
        if (postingDay != null) {
            queryParams.put("postingDay", postingDay);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        if (start != null) {
            queryParams.put("start", start.toString());
        }
        if (limit != null) {
            queryParams.put("limit", limit.toString());
        }
        return queryParams;
    }

}
