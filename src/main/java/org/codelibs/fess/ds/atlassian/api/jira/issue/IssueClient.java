package org.codelibs.fess.ds.atlassian.api.jira.issue;

import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;

public class IssueClient {
    protected final JiraClient client;

    public IssueClient(final JiraClient client) {
        this.client = client;
    }

    public GetCommentsRequest comments(final String idOrKey) {
        return new GetCommentsRequest(client, idOrKey);
    }

    public GetIssueRequest issue(final String user) {
        return new GetIssueRequest(client, user);
    }
}
