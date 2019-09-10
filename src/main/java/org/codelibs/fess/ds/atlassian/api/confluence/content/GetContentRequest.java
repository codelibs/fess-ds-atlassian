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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

public class GetContentRequest extends ConfluenceRequest {

    private String id;
    private String status;
    private Integer version;
    private String[] expand;

    public GetContentRequest(final HttpRequestFactory httpRequestFactory, final String appHome, final String id) {
        super(httpRequestFactory,  appHome);
        this.id = id;
    }

    public GetContentRequest status(final String status) {
        this.status = status;
        return this;
    }

    public GetContentRequest version(final int version) {
        this.version = version;
        return this;
    }

    public GetContentRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetContentResponse execute() {
        return parseResponse(getHttpResponseAsString(GET_REQUEST));
    }

    public static GetContentResponse parseResponse(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return new GetContentResponse(mapper.readValue(json, Content.class));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse content from: " + json, e);
        }
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/content/" + id);
        if (status != null) {
            url.put("status", status);
        }
        if (version != null) {
            url.put("version", version);
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}
