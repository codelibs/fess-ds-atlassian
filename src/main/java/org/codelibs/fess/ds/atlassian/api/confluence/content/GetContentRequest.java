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
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;

public class GetContentRequest extends ConfluenceRequest {

    private String id, status;
    private Integer version;
    private String[] expand;

    public GetContentRequest(ConfluenceClient confluenceClient, String id) {
        super(confluenceClient);
        this.id = id;
    }

    @Override
    public GetContentResponse execute() {
        try {
            final HttpRequest request = confluenceClient.request()
                    .buildGetRequest(buildUrl(confluenceClient.confluenceHome(), id, status, version, expand));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> content = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            return new GetContentResponse(content);
        } catch (IOException e) {
            throw new RuntimeException(e);
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

    protected GenericUrl buildUrl(final String jiraHome, final String id, final String status, final Integer version,
            final String[] expand) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/content/" + id);
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