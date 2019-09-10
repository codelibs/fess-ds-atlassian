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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

public class GetSpaceRequest extends ConfluenceRequest {

    private final String spaceKey;
    private String[] expand;

    public GetSpaceRequest(final HttpRequestFactory httpRequestFactory, final String appHome, final String spaceKey) {
        super(httpRequestFactory, appHome);
        this.spaceKey = spaceKey;
    }

    public GetSpaceRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetSpaceResponse execute() {
        return parseResponse(getHttpResponseAsString(GET_REQUEST));
    }

    public static GetSpaceResponse parseResponse(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Space space = mapper.readValue(json, Space.class);
            return new GetSpaceResponse(space);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse space from: " + json, e);
        }
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/space/" + spaceKey);
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}
