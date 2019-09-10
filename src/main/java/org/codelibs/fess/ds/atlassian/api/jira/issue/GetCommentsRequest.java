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
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comments;

public class GetCommentsRequest extends JiraRequest {

    private final String issueIdOrKey;
    private Long startAt;
    private Integer maxResults;
    private String orderBy;
    private String[] expand;

    public GetCommentsRequest(final HttpRequestFactory httpRequestFactory, final String appHome, String issueIdOrKey) {
        super(httpRequestFactory, appHome);
        this.issueIdOrKey = issueIdOrKey;
    }

    @Override
    public GetCommentsResponse execute() {
        String result = "";
        final GenericUrl url = buildUrl(appHome(), issueIdOrKey, startAt, maxResults, orderBy, expand);
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
            if (e.getStatusCode() == 404) {
                throw new AtlassianDataStoreException(
                        "The issue with the given id/key does not exist or if the currently authenticated user does not have permission to view it: "
                                + issueIdOrKey,
                        e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
        return fromJson(result);
    }

    public GetCommentsRequest startAt(long startAt) {
        this.startAt = startAt;
        return this;
    }

    public GetCommentsRequest maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public GetCommentsRequest orderBy(String orderBy) {
        this.orderBy = orderBy;
        return this;
    }

    public GetCommentsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public static GetCommentsResponse fromJson(String json) {
        final ObjectMapper mapper = new ObjectMapper();
        final List<Comment> comments = new ArrayList<>();
        try {
            comments.addAll(mapper.readValue(json, Comments.class).getComments());
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: \"" + json + "\"", e);
        }
        return new GetCommentsResponse(comments);
    }

    protected GenericUrl buildUrl(final String jiraHome, final String issueIdOrKey, final Long startAt, final Integer maxResults,
                                  final String orderBy, final String[] expand) {
        final GenericUrl url = new GenericUrl(jiraHome + "/rest/api/latest/issue/" + issueIdOrKey + "/comment");
        if (startAt != null) {
            url.put("startAt", startAt);
        }
        if (maxResults != null) {
            url.put("maxResults", maxResults);
        }
        if (orderBy != null) {
            url.put("orderBy", orderBy);
        }
        if (expand != null) {
            url.put("expand", String.join(",", expand));
        }
        return url;
    }

}