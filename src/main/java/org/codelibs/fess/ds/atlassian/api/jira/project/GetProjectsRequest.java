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
package org.codelibs.fess.ds.atlassian.api.jira.project;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Project;

public class GetProjectsRequest extends JiraRequest {

    private String[] expand;
    private Integer recent;

    public GetProjectsRequest(final HttpRequestFactory httpRequestFactory, final String appHome) {
        super(httpRequestFactory, appHome);
    }

    public GetProjectsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetProjectsRequest recent(int recent) {
        this.recent = recent;
        return this;
    }

    public GetProjectsResponse execute() {
        return parseResponse(getHttpResponseAsString());
    }

    public static GetProjectsResponse parseResponse(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        final List<Project> projects = new ArrayList<>();
        try {
            projects.addAll(mapper.readValue(json, new TypeReference<List<Project>>() {
            }));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse projects from: \"" + json + "\"", e);
        }
        return new GetProjectsResponse(projects);
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/project");
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        if (recent != null) {
            url.put("recent", recent);
        }
        return url;
    }

}