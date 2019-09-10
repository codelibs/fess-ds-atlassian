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
package org.codelibs.fess.ds.atlassian.api.jira.search;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.util.GenericData;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.jira.JiraRequest;

public class SearchRequest extends JiraRequest {

    private String jql;
    private Integer startAt;
    private Integer maxResults;
    private Boolean validateQuery;
    private String[] fields;
    private String[] expand;

    public SearchResponse execute() {
        return parseResponse(getHttpResponseAsString());
    }

    public SearchRequest(final HttpRequestFactory httpRequestFactory, final String appHome) {
        super(httpRequestFactory, appHome);
    }

    public static SearchResponse parseResponse(final String json) {
        final ObjectMapper mapper = new ObjectMapper();
        try {
            return mapper.readValue(json, SearchResponse.class);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse: \"" + json + "\"", e);
        }
    }

    public SearchRequest jql(final String jql) {
        this.jql = jql;
        return this;
    }

    public SearchRequest startAt(final int startAt) {
        this.startAt = startAt;
        return this;
    }

    public SearchRequest maxResults(final int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public SearchRequest validateQuery(final boolean validateQuery) {
        this.validateQuery = validateQuery;
        return this;
    }

    public SearchRequest fields(final String... fields) {
        this.fields = fields;
        return this;
    }

    public SearchRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    @Override
    public GenericUrl buildUrl() {
        return new GenericUrl(appHome() + "/rest/api/latest/search");
    }

    protected GenericData buildData() {
        final GenericData data = new GenericData();
        if (jql != null && !jql.isEmpty()) {
            data.put("jql", jql);
        }
        if (startAt != null) {
            data.put("startAt", startAt);
        }
        if (maxResults != null) {
            data.put("maxResults", maxResults);
        }
        if (validateQuery != null) {
            data.put("validateQuery", validateQuery);
        }
        if (fields != null) {
            data.put("fields", fields);
        }
        if (expand != null) {
            data.put("expand", String.join(",", expand));
        }
        return data;
    }

}
