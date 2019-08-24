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
package org.codelibs.fess.ds.atlassian.api.jira.issue;

import java.util.Map;

import org.codelibs.fess.ds.atlassian.api.Response;
import org.codelibs.fess.ds.atlassian.api.jira.JiraResponse;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;

public class GetIssueResponse extends JiraResponse {
    protected final Issue issue;

    public GetIssueResponse(final Issue issue) {
        this.issue = issue;
    }

    public Issue getIssue() {
        return issue;
    }

}