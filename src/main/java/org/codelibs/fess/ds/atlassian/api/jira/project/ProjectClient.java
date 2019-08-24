package org.codelibs.fess.ds.atlassian.api.jira.project;

import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;

public class ProjectClient {
    protected final JiraClient client;

    public ProjectClient(final JiraClient client) {
        this.client = client;
    }

    public GetProjectRequest project(final String projectIdOrKey) { return new GetProjectRequest(client, projectIdOrKey); }

    public GetProjectsRequest projects() { return new GetProjectsRequest(client); }
}
