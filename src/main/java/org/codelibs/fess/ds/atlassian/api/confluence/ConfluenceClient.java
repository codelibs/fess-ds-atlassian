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
package org.codelibs.fess.ds.atlassian.api.confluence;

import java.io.Closeable;
import java.util.List;
import java.util.function.Consumer;

import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpaceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesRequest;
import org.codelibs.fess.entity.DataStoreParams;

/**
 * Confluence API client for accessing Confluence content, spaces, and comments.
 * Provides high-level methods for interacting with Confluence REST API.
 */
public class ConfluenceClient extends AtlassianClient implements Closeable {

    /** Default limit for content requests. */
    protected static final String DEFAULT_CONTENT_LIMIT = "25";

    // parameters for confluence
    /** Parameter key for content limit configuration. */
    protected static final String CONTENT_LIMIT_PARAM = "content_limit";

    /** The Confluence instance home URL. */
    protected final String confluenceHome;

    /** The maximum number of content items to retrieve per request. */
    protected final Integer contentLimit;

    /**
     * Constructs a new Confluence client with the specified parameters.
     *
     * @param paramMap the configuration parameters
     */
    public ConfluenceClient(final DataStoreParams paramMap) {
        super(paramMap);
        confluenceHome = getHome(paramMap);
        contentLimit = getContentLimit(paramMap);
    }

    @Override
    public void close() {
        // TODO
    }

    /**
     * Gets the Confluence home URL.
     *
     * @return the Confluence home URL
     */
    public String getConfluenceHome() {
        return confluenceHome;
    }

    /**
     * Gets the content limit from parameters.
     *
     * @param paramMap the parameter map
     * @return the content limit
     */
    public Integer getContentLimit(final DataStoreParams paramMap) {
        return Integer.parseInt(paramMap.getAsString(CONTENT_LIMIT_PARAM, DEFAULT_CONTENT_LIMIT));
    }

    /**
     * Creates a request to get all spaces.
     *
     * @return a GetSpacesRequest instance
     */
    public GetSpacesRequest spaces() {
        return createRequest(new GetSpacesRequest());
    }

    /**
     * Creates a request to get a specific space.
     *
     * @param spaceKey the space key
     * @return a GetSpaceRequest instance
     */
    public GetSpaceRequest space(final String spaceKey) {
        return createRequest(new GetSpaceRequest(spaceKey));
    }

    /**
     * Creates a request to get content.
     *
     * @return a GetContentsRequest instance
     */
    public GetContentsRequest contents() {
        return createRequest(new GetContentsRequest());
    }

    /**
     * Creates a request to get specific content.
     *
     * @param contentId the content ID
     * @return a GetContentRequest instance
     */
    public GetContentRequest content(final String contentId) {
        return createRequest(new GetContentRequest(contentId));
    }

    /**
     * Creates a request to get comments of specific content.
     *
     * @param contentId the content ID
     * @return a GetCommentsOfContentRequest instance
     */
    public GetCommentsOfContentRequest commentsOfContent(final String contentId) {
        return createRequest(new GetCommentsOfContentRequest(contentId));
    }

    /**
     * Creates a request to get attachments of specific content.
     *
     * @param contentId the content ID
     * @return a GetAttachmentsOfContentRequest instance
     */
    public GetAttachmentsOfContentRequest attachmentsOfContent(final String contentId) {
        return createRequest(new GetAttachmentsOfContentRequest(contentId));
    }

    @Override
    protected String getAppHome() {
        return confluenceHome;
    }

    /**
     * Retrieves all content pages using pagination and passes them to the consumer.
     *
     * @param consumer the consumer to process each content item
     */
    public void getContents(final Consumer<Content> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetContentsResponse response =
                    contents().start(start).limit(contentLimit).expand("space", "version", "body.view").execute();
            final List<Content> contents = response.getContents();
            contents.forEach(consumer);
            if (contents.size() < contentLimit) {
                break;
            }
        }
    }

    /**
     * Retrieves all blog content using pagination and passes them to the consumer.
     *
     * @param consumer the consumer to process each blog content item
     */
    public void getBlogContents(final Consumer<Content> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetContentsResponse response =
                    contents().start(start).limit(contentLimit).type("blogpost").expand("space", "version", "body.view").execute();
            final List<Content> contents = response.getContents();
            contents.forEach(consumer);
            if (contents.size() < contentLimit) {
                break;
            }
        }
    }

    /**
     * Retrieves all comments for specific content using pagination and passes them to the consumer.
     *
     * @param id the content ID
     * @param consumer the consumer to process each comment
     */
    public void getContentComments(final String id, final Consumer<Comment> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetCommentsOfContentResponse response =
                    commentsOfContent(id).start(start).limit(contentLimit).expand("body.view").execute();
            final List<Comment> comments = response.getComments();
            comments.forEach(consumer);
            if (comments.size() < contentLimit) {
                break;
            }
        }
    }

}
