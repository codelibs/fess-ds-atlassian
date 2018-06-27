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
package org.codelibs.fess.ds.atlassian.api.search;

import org.codelibs.fess.ds.atlassian.api.Request;

public class SearchRequest extends Request {

    private String jql;
    private int startAt, maxResults;
    private String[] fields;

    @Override
    public SearchResponse execute() {
        return new SearchResponse();
    }

    public SearchRequest jql(String jql) {
        this.jql = jql;
        return this;
    }

    public SearchRequest startAt(int startAt) {
        this.startAt = startAt;
        return this;
    }

    public SearchRequest maxResults(int maxResults) {
        this.maxResults = maxResults;
        return this;
    }

    public SearchRequest fields(String... fields) {
        this.fields = fields;
        return this;
    }

}