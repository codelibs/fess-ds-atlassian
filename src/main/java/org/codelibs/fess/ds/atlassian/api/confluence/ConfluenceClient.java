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
package org.codelibs.fess.ds.atlassian.api.confluence;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class ConfluenceClient extends AtlassianClient implements Closeable {

    private static final Logger logger = LoggerFactory.getLogger(ConfluenceClient.class);

    protected static final String DEFAULT_CONTENT_LIMIT = "25";

    // parameters for confluence
    protected static final String CONTENT_LIMIT_PARAM = "content_limit";

    protected final String confluenceHome;
    protected final Integer contentLimit;

    public ConfluenceClient(final Map<String, String> paramMap) {
        super(paramMap);
        confluenceHome = getHome(paramMap);
        contentLimit = getContentLimit(paramMap);
    }

    @Override
    public void close() {
        // TODO
    }

    public String getConfluenceHome() {
        return confluenceHome;
    }

    public Integer getContentLimit(final Map<String, String> paramMap) {
        return Integer.parseInt(paramMap.getOrDefault(CONTENT_LIMIT_PARAM, DEFAULT_CONTENT_LIMIT));
    }

    public GetSpacesRequest getSpaces() {
        return new GetSpacesRequest(authentication, getConfluenceHome());
    }

    public GetSpaceRequest getSpace(final String spaceKey) {
        return new GetSpaceRequest(authentication, getConfluenceHome(), spaceKey);
    }

    public GetContentsRequest getContents() {
        return new GetContentsRequest(authentication, getConfluenceHome());
    }

    public GetContentRequest getContent(final String contentId) {
        return new GetContentRequest(authentication, getConfluenceHome(), contentId);
    }

    public GetCommentsOfContentRequest getCommentsOfContent(final String contentId) {
        return new GetCommentsOfContentRequest(authentication, getConfluenceHome(), contentId);
    }

    public GetAttachmentsOfContentRequest getAttachmentsOfContent(final String contentId) {
        return new GetAttachmentsOfContentRequest(authentication, getConfluenceHome(), contentId);
    }

    public void getContents(final Consumer<Content> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetContentsResponse response = getContents().start(start).limit(contentLimit).expand("space", "version", "body.view").execute();
            final List<Content> contents = response.getContents();
            contents.forEach(consumer);
            if (contents.size() < contentLimit)
                break;
        }
    }

    public void getBlogContents(final Consumer<Content> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetContentsResponse response = getContents().start(start).limit(contentLimit).type("blogpost")
                    .expand("space", "version", "body.view").execute();
            final List<Content> contents = response.getContents();
            contents.forEach(consumer);
            if (contents.size() < contentLimit)
                break;
        }
    }

    public void getContentComments(final String id, final Consumer<Comment> consumer) {
        for (int start = 0;; start += contentLimit) {
            final GetCommentsOfContentResponse response = getCommentsOfContent(id).start(start).limit(contentLimit).expand("body.view").execute();
            final List<Comment> comments = response.getComments();
            comments.forEach(consumer);
            if (comments.size() < contentLimit)
                break;
        }
    }

}
