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

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpResponse;
import org.codelibs.fess.ds.atlassian.api.JiraClient;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class GetIssueRequest extends Request {

    private String issueIdOrKey;
    private String[] fields, expand, properties;

    public GetIssueRequest(JiraClient jiraClient, String issueIdOrKey) {
        super(jiraClient);
        this.issueIdOrKey = issueIdOrKey;
    }

    @Override
    public GetIssueResponse execute() {
        try {
            final HttpRequest request = jiraClient.request().buildGetRequest(buildUrl(jiraClient.jiraHome(), issueIdOrKey));
            final HttpResponse response = request.execute();
            final Scanner s = new Scanner(response.getContent()).useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            final JSONObject jsonObj = new JSONObject(result);
            final Map<String, Object> issue = new HashMap<>();
            for (String key: jsonObj.keySet()) {
                issue.put(key, jsonObj.get(key));
            }
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

    protected GenericUrl buildUrl(final String jiraHome, final String issueIdOrKey) {
        return new GenericUrl(jiraHome + "/rest/api/latest/issue/" + issueIdOrKey);
    }

}