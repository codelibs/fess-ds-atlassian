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
package org.codelibs.fess.ds.atlassian.api.jira.project;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;

import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;

public class GetProjectsRequest extends JiraRequest {

    private String[] expand;
    private Integer recent;

    public GetProjectsRequest(JiraClient jiraClient) {
        super(jiraClient);
    }

    @Override
    public GetProjectsResponse execute() {
        try {
            final HttpRequest request = jiraClient.request()
                    .buildGetRequest(buildUrl(jiraClient.jiraHome(), expand, recent));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final ObjectMapper mapper = new ObjectMapper();
            final List<Map<String, Object>> projects = mapper.readValue(result,
                    new TypeReference<List<Map<String, Object>>>() {
                    });
            return new GetProjectsResponse(projects);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public GetProjectsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetProjectsRequest recent(int recent) {
        this.recent = recent;
        return this;
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