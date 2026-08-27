/*
 * Copyright 2012-2025 CodeLibs Project and the Others.
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
package org.codelibs.fess.ds.atlassian.api.endpoint;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.api.Deployment;

/**
 * Strategy interface for resolving Atlassian API endpoints.
 */
public interface EndpointStrategy {
    /**
     * Returns the home URL.
     *
     * @return the home URL
     */
    String getHomeUrl();

    /**
     * Returns the API URL.
     *
     * @return the API URL
     */
    String getApiUrl();

    /**
     * Returns the deployment type this strategy resolves endpoints for.
     *
     * @return the deployment type
     */
    Deployment getDeployment();

    /**
     * Returns the JIRA REST API base path for this deployment.
     * Cloud uses {@code /rest/api/3}; Data Center only provides {@code /rest/api/2}.
     *
     * @return the JIRA API base path, starting with a slash
     */
    default String getJiraApiBase() {
        return getDeployment() == Deployment.CLOUD ? "/rest/api/3" : "/rest/api/2";
    }

    /**
     * Builds the browser-facing URL of a Confluence content item.
     *
     * @param contentId the content id
     * @param spaceKey the space key, may be null or blank
     * @return the view URL
     */
    default String getContentViewUrl(final String contentId, final String spaceKey) {
        if (getDeployment() == Deployment.CLOUD && StringUtil.isNotBlank(spaceKey)) {
            return getHomeUrl() + "/spaces/" + spaceKey + "/pages/" + contentId;
        }
        return getHomeUrl() + "/pages/viewpage.action?pageId=" + contentId;
    }

    /**
     * Builds the browser-facing URL of a JIRA issue.
     *
     * @param issueKey the issue key
     * @return the view URL
     */
    default String getIssueViewUrl(final String issueKey) {
        return getHomeUrl() + "/browse/" + issueKey;
    }
}
