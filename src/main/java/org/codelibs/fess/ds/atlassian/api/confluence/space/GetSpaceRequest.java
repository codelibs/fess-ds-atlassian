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
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

public class GetSpaceRequest extends ConfluenceRequest {

    private final String spaceKey;
    private String[] expand;

    public GetSpaceRequest(ConfluenceClient confluenceClient, String spaceKey) {
        super(confluenceClient);
        this.spaceKey = spaceKey;
    }

    @Override
    public GetSpaceResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(confluenceClient.confluenceHome(), spaceKey, expand);
        try {
            final HttpRequest request = confluenceClient.request().buildGetRequest(url);
            final HttpResponse response = request.execute();
            if (response.getStatusCode() != 200) {
                throw new HttpResponseException(response);
            }
            final Scanner s = new Scanner(response.getContent());
            s.useDelimiter("\\A");
            result = s.hasNext() ? s.next() : StringUtil.EMPTY;
            s.close();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new AtlassianDataStoreException(
                        "There is no space with the given key, or if the calling user does not have permission to view the space: "
                                + spaceKey,
                        e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
    }

    public GetSpaceRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public static GetSpaceResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Space space = mapper.readValue(json, Space.class);
            return new GetSpaceResponse(space);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse space from: " + json, e);
        }
    }

    protected GenericUrl buildUrl(final String confluenceHome, final String spaceKey, final String[] expand) {
        final GenericUrl url = new GenericUrl(confluenceHome + "/rest/api/latest/space/" + spaceKey);
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }
}