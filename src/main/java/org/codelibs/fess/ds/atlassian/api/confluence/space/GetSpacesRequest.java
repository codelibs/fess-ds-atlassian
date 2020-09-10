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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

import com.fasterxml.jackson.core.type.TypeReference;

public class GetSpacesRequest extends Request {

    private String spaceKey;
    private String type;
    private String status;
    private String label;
    private String favourite;
    private String[] expand;
    private Integer start;
    private Integer limit;

    public GetSpacesRequest(final Authentication authentication, final String appHome) {
        super(authentication, appHome);
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
        return parseResponse(getCurlResponse(GET).getContentAsString());
    }

    public static GetSpacesResponse parseResponse(String json) {
        try {
            final String results = mapper.readTree(json).get("results").toString();
            final List<Space> spaces = new ArrayList<>(mapper.readValue(results, new TypeReference<List<Space>>(){}));
            return new GetSpacesResponse(spaces);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse spaces from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/space";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (spaceKey != null) {
            queryParams.put("spaceKey", spaceKey);
        }
        if (type != null) {
            queryParams.put("type", type);
        }
        if (status != null) {
            queryParams.put("status", status);
        }
        if (label != null) {
            queryParams.put("label", label);
        }
        if (favourite != null) {
            queryParams.put("favourite", favourite);
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
