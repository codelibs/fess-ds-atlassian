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
package org.codelibs.fess.ds.atlassian.api.issue;

import java.io.IOException;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

import org.codelibs.fess.ds.atlassian.api.JiraClient;
import org.codelibs.fess.ds.atlassian.api.Request;

public class GetIssueRequest extends Request {

    private final String issueIdOrKey;
    private String[] fields, expand, properties;

    public GetIssueRequest(JiraClient jiraClient, String issueIdOrKey) {
        super(jiraClient);
        this.issueIdOrKey = issueIdOrKey;
    }

    @Override
    public GetIssueResponse execute() {
        try {
            final HttpRequest request = jiraClient.request()
                    .buildGetRequest(buildUrl(jiraClient.jiraHome(), issueIdOrKey, fields, expand, properties));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final Map<String, Object> issue = mapper.readValue(result, new TypeReference<Map<String, Object>>() {
            });
            return new GetIssueResponse(issue);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GetIssueRequest fields(String... fields) {
        this.fields = fields;
        return this;
    }

    public GetIssueRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetIssueRequest properties(String... properties) {
        this.properties = properties;
        return this;
    }

    protected GenericUrl buildUrl(final String jiraHome, final String issueIdOrKey, final String[] fields,
            final String[] expand, final String[] properties) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/issue/" + issueIdOrKey);
        if (fields != null) {
            url.put("fields", String.join(",", fields));
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        if (properties != null) {
            url.put("properties", String.join(",", properties));
        }
        return url;
    }

}