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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comments;

public class GetCommentsRequest extends JiraRequest {

    private final String issueIdOrKey;
    private Long startAt;
    private Integer maxResults;
    private String orderBy;
    private String[] expand;

    public GetCommentsRequest(final HttpRequestFactory httpRequestFactory, final String appHome, final String issueIdOrKey) {
        super(httpRequestFactory, appHome);
        this.issueIdOrKey = issueIdOrKey;
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

    public GetCommentsResponse execute() {
        return parseResponse(getHttpResponseAsString());
    }

    public static GetCommentsResponse parseResponse(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        final List<Comment> comments = new ArrayList<>();
        try {
            comments.addAll(mapper.readValue(json, Comments.class).getComments());
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: \"" + json + "\"", e);
        }
        return new GetCommentsResponse(comments);
    }

    @Override
    public GenericUrl buildUrl() {
        final GenericUrl url = new GenericUrl(appHome() + "/rest/api/latest/issue/" + issueIdOrKey + "/comment");
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
