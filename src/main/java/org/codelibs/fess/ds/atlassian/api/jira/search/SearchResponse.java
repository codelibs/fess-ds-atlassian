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
package org.codelibs.fess.ds.atlassian.api.jira.search;

import java.util.List;

import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Response containing search results from JIRA.
 * Contains the total number of matching issues and the issues themselves.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class SearchResponse {

    /** Number of total hits. **/
    protected Long total;

    /** The offset the endpoint actually served, echoed back from the request. */
    protected Integer startAt;

    /** The list of issues returned by the search. */
    protected List<Issue> issues;

    /** Continuation token returned by the enhanced JQL search endpoint. */
    protected String nextPageToken;

    /** Whether this is the last page, as reported by the enhanced JQL search endpoint. */
    protected Boolean isLast;

    /**
     * Default constructor for SearchResponse.
     */
    public SearchResponse() {
        // Default constructor
    }

    /**
     * Return number of total hits.
     *
     * @return number of total hits
     */
    public Long getTotal() {
        return total;
    }

    /**
     * Returns the offset the endpoint reported serving.
     *
     * @return the offset, or null when the endpoint did not echo one
     */
    public Integer getStartAt() {
        return startAt;
    }

    /**
     * Returns the list of issues.
     *
     * @return the list of issues
     */
    public List<Issue> getIssues() {
        return issues;
    }

    /**
     * Returns the continuation token for the next page.
     *
     * @return the token, or null when the endpoint did not return one
     */
    public String getNextPageToken() {
        return nextPageToken;
    }

    /**
     * Returns whether the endpoint reported this as the last page.
     *
     * @return the flag, or null when the endpoint did not return one
     */
    public Boolean getIsLast() {
        return isLast;
    }

    /**
     * Creates a SearchResponse with the given list of issues.
     *
     * @param issues the list of issues
     * @return a new SearchResponse instance
     */
    public static SearchResponse create(final List<Issue> issues) {
        final SearchResponse response = new SearchResponse();
        response.issues = issues;
        return response;
    }

}
