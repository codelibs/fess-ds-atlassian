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
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;

public class SearchRequest extends Request {

    private String jql;
    private Integer startAt;
    private Integer maxResults;
    private Boolean validateQuery;
    private String[] fields;
    private String[] expand;
    public SearchRequest(final Authentication authentication, final String appHome) {
        super(authentication, appHome);
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

    public static SearchResponse parseResponse(final String json) {
        try {
            return mapper.readValue(json, SearchResponse.class);
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to parse: \"" + json + "\"", e);
        }
    }


    public SearchResponse execute() {
        return parseResponse(getCurlResponse(GET).getContentAsString());
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/search";
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (jql != null && !jql.isEmpty()) {
            queryParams.put("jql", jql);
        }
        if (startAt != null) {
            queryParams.put("startAt", startAt.toString());
        }
        if (maxResults != null) {
            queryParams.put("maxResults", maxResults.toString());
        }
        if (validateQuery != null) {
            queryParams.put("validateQuery", validateQuery.toString());
        }
        if (fields != null) {
            queryParams.put("fields", String.join(",", fields));
        }
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

}
