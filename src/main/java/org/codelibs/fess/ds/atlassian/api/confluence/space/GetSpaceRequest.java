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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

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

public class GetSpaceRequest extends ConfluenceRequest {

    private final String spaceKey;
    private String[] expand;

    public GetSpaceRequest(ConfluenceClient confluenceClient, String spaceKey) {
        super(confluenceClient);
        this.spaceKey = spaceKey;
    }

    @Override
    public GetSpaceResponse execute() {
        try {
            final HttpRequest request = confluenceClient.request()
                    .buildGetRequest(buildUrl(confluenceClient.confluenceHome(), spaceKey, expand));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> space = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            return new GetSpaceResponse(space);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GetSpaceRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    protected GenericUrl buildUrl(final String jiraHome, final String spaceKey, final String[] expand) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/space/" + spaceKey);
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }
}