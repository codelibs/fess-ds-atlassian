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

    @Override
    public GetProjectsResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(appHome(), expand, recent);
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
        } catch (HttpResponseException e) {
            throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
    }

    public GetProjectsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetProjectsRequest recent(int recent) {
        this.recent = recent;
        return this;
    }

    public static GetProjectsResponse fromJson(String json) {
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

    protected GenericUrl buildUrl(final String jiraHome, final String[] expand, final Integer recent) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/project");
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        if (recent != null) {
            url.put("recent", recent);
        }
        return url;
    }

}