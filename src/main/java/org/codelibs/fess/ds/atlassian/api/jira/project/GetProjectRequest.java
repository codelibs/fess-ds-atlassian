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
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Project;

public class GetProjectRequest extends JiraRequest {

    private final String projectIdOrKey;
    private String[] expand;

    public GetProjectRequest(JiraClient jiraClient, String projectIdOrKey) {
        super(jiraClient);
        this.projectIdOrKey = projectIdOrKey;
    }

    @Override
    public GetProjectResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(jiraClient.jiraHome(), projectIdOrKey, expand);
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
                throw new AtlassianDataStoreException(
                        "The project is not found, or the calling user does not have permission to view it: " + projectIdOrKey, e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
    }

    public GetProjectRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public static GetProjectResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final Project project = mapper.readValue(json, Project.class);
            return new GetProjectResponse(project);
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse project from: \"" + json + "\"", e);
        }
    }

    protected GenericUrl buildUrl(final String jiraHome, final String projectIdOrKey, final String[] expand) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/project/" + projectIdOrKey);
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}