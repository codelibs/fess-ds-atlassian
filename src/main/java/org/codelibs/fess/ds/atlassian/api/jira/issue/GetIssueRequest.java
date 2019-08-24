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
package org.codelibs.fess.ds.atlassian.api.jira.issue;

import java.io.IOException;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;

public class GetIssueRequest extends JiraRequest {

    private final String issueIdOrKey;
    private String[] fields, expand, properties;

    public GetIssueRequest(JiraClient jiraClient, String issueIdOrKey) {
        super(jiraClient);
        this.issueIdOrKey = issueIdOrKey;
    }

    @Override
    public GetIssueResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(jiraClient.jiraHome(), issueIdOrKey, fields, expand, properties);
        try {
            final HttpRequest request = jiraClient.request().buildGetRequest(url);
            final HttpResponse response = request.execute();
            if (response.getStatusCode() != 200) {
                throw new HttpResponseException(response);
            }
            final Scanner s = new Scanner(response.getContent());
            s.useDelimiter("\\A");
            result = s.hasNext() ? s.next() : "";
            s.close();
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new AtlassianDataStoreException("The requested issue is not found, or the user does not have permission to view it.",
                        e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
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

    public static GetIssueResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Issue issue = mapper.readValue(json, new TypeReference<Issue>() {
            });
            return new GetIssueResponse(issue);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse issue from: \"" + json + "\"", e);
        }
    }

    protected GenericUrl buildUrl(final String jiraHome, final String issueIdOrKey, final String[] fields, final String[] expand,
            final String[] properties) {
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