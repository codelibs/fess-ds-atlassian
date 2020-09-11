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
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Comments;
import org.jsoup.internal.StringUtil;

public class GetCommentsRequest extends Request {

    private final String issueIdOrKey;
    private Long startAt;
    private Integer maxResults;
    private String orderBy;
    private String[] expand;

    public GetCommentsRequest(final Authentication authentication, final String appHome, final String issueIdOrKey) {
        super(authentication, appHome);
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
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    public static GetCommentsResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetCommentsResponse(Collections.emptyList());
        }
        try {
            return new GetCommentsResponse(mapper.readValue(json, Comments.class).getComments());
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse comments from: \"" + json + "\"", e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/issue/" + issueIdOrKey + "/comment";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (startAt != null) {
            queryParams.put("startAt", startAt.toString());
        }
        if (maxResults != null) {
            queryParams.put("maxResults", maxResults.toString());
        }
        if (orderBy != null) {
            queryParams.put("orderBy", orderBy);
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

    @Override
    public String toString() {
        return "GetCommentsRequest [issueIdOrKey=" + issueIdOrKey + ", startAt=" + startAt + ", maxResults=" + maxResults + ", orderBy="
                + orderBy + ", expand=" + Arrays.toString(expand) + "]";
    }

}
