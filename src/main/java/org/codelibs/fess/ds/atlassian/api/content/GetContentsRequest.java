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
package org.codelibs.fess.ds.atlassian.api.content;

import org.codelibs.fess.ds.atlassian.api.JiraClient;
import org.codelibs.fess.ds.atlassian.api.Request;

public class GetContentsRequest extends Request {

    private String type, spaceKey, title, status, postingDay;
    private String[] expand;
    private int start, limit;

    public GetContentsRequest(JiraClient jiraClient) {
        super(jiraClient);
    }

    @Override
    public GetContentsResponse execute() {
        return new GetContentsResponse();
    }

    public GetContentsRequest type(String type) {
        this.type = type;
        return this;
    }

    public GetContentsRequest spaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetContentsRequest title(String title) {
        this.title = title;
        return this;
    }

    public GetContentsRequest status(String status) {
        this.status = status;
        return this;
    }

    public GetContentsRequest postingDay(String postingDay) {
        this.postingDay = postingDay;
        return this;
    }

    public GetContentsRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetContentsRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetContentsRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

}