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

import org.codelibs.fess.ds.atlassian.api.Request;

public class GetIssueRequest extends Request {

    private String issueIdOrKey;
    private String[] fields, expand, properties;

    public GetIssueRequest(String issueIdOrKey) {
        this.issueIdOrKey = issueIdOrKey;
    }

    @Override
    public GetIssueResponse execute() {
        return new GetIssueResponse();
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

}