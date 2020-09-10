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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

import com.fasterxml.jackson.databind.ObjectMapper;

public class GetContentRequest extends Request {

    private String id;
    private String status;
    private Integer version;
    private String[] expand;

    public GetContentRequest(final Authentication authentication, final String appHome, final String id) {
        super(authentication,  appHome);
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
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
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
    public String getURL() {
        return appHome + "/rest/api/latest/content/" + id;
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (status != null) {
            queryParams.put("status", status);
        }
        if (version != null) {
            queryParams.put("version", version.toString());
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetContentRequest [id=" + id + ", status=" + status + ", version=" + version + ", expand=" + Arrays.toString(expand) + "]";
    }

}
