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
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;

public class GetContentRequest extends ConfluenceRequest {

    private String id, status;
    private Integer version;
    private String[] expand;

    public GetContentRequest(final HttpRequestFactory httpRequestFactory, final String appHome, String id) {
        super(httpRequestFactory,  appHome);
        this.id = id;
    }

    @Override
    public GetContentResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(appHome(), id, status, version, expand);
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
            return fromJson(result);
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
    }

    public GetContentRequest status(String status) {
        this.status = status;
        return this;
    }

    public GetContentRequest version(int version) {
        this.version = version;
        return this;
    }

    public GetContentRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public static GetContentResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return new GetContentResponse(mapper.readValue(json, Content.class));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse content from: " + json, e);
        }
    }

    protected GenericUrl buildUrl(final String confluenceHome, final String id, final String status, final Integer version,
            final String[] expand) {
        final GenericUrl url = new GenericUrl(confluenceHome + "/rest/api/latest/content/" + id);
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