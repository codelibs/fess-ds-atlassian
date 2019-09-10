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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;

public class GetIssueRequest extends JiraRequest {

    private final String issueIdOrKey;
    private String[] fields;
    private String[] expand;
    private String[] properties;

    public GetIssueRequest(final HttpRequestFactory httpRequestFactory, final String appHome, final String issueIdOrKey) {
        super(httpRequestFactory, appHome);
        this.issueIdOrKey = issueIdOrKey;
    }

    public GetIssueRequest fields(final String... fields) {
        this.fields = fields;
        return this;
    }

    public GetIssueRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetIssueRequest properties(final String... properties) {
        this.properties = properties;
        return this;
    }

    public GetIssueResponse execute() {
        return parseResponse(getHttpResponseAsString(GET_REQUEST));
    }

    public static GetIssueResponse parseResponse(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Issue issue = mapper.readValue(json, new TypeReference<Issue>() {
            });
            return new GetIssueResponse(issue);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse issue from: \"" + json + "\"", e);
        }
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/issue/" + issueIdOrKey);
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
