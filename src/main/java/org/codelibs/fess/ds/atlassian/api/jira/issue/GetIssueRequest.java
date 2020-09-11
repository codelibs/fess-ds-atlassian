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
import java.util.HashMap;
import java.util.Map;

import org.codelibs.curl.CurlException;
import org.codelibs.curl.CurlResponse;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.jsoup.internal.StringUtil;

import com.fasterxml.jackson.core.type.TypeReference;

public class GetIssueRequest extends Request {

    private final String issueIdOrKey;
    private String[] fields;
    private String[] expand;
    private String[] properties;

    public GetIssueRequest(final Authentication authentication, final String appHome, final String issueIdOrKey) {
        super(authentication, appHome);
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
        try (CurlResponse response = getCurlResponse(GET)) {
            if (response.getHttpStatusCode() != 200) {
                throw new CurlException("HTTP Status : " + response.getHttpStatusCode() + ", error : " + response.getContentAsString());
            }
            return parseResponse(response.getContentAsString());
        } catch (Exception e) {
            throw new AtlassianDataStoreException("Failed to access " + this, e);
        }
    }

    public static GetIssueResponse parseResponse(final String json) {
        if (StringUtil.isBlank(json)) {
            return new GetIssueResponse(null);
        }
        try {
            return new GetIssueResponse(mapper.readValue(json, new TypeReference<Issue>() {}));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse issue from: \"" + json + "\"", e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/issue/" + issueIdOrKey;
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (fields != null) {
            queryParams.put("fields", String.join(",", fields));
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        if (properties != null) {
            queryParams.put("properties", String.join(",", properties));
        }
        return queryParams;
    }

}
