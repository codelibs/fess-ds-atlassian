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
package org.codelibs.fess.ds.atlassian.api.confluence;

import com.google.api.client.http.HttpRequestFactory;

import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpaceRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesRequest;

public class ConfluenceClient {

    private final AtlassianClient client;

    public ConfluenceClient(final AtlassianClient client) {
        this.client = client;
    }

    public String confluenceHome() {
        return client.appHome();
    }

    public HttpRequestFactory request() {
        return client.request();
    }

    public GetSpacesRequest getSpaces() {
        return new GetSpacesRequest(this);
    }

    public GetSpaceRequest getSpace(String spaceKey) {
        return new GetSpaceRequest(this, spaceKey);
    }

    public GetContentsRequest getContents() {
        return new GetContentsRequest(this);
    }

    public GetContentRequest getContent(String contentId) {
        return new GetContentRequest(this, contentId);
    }

    public GetCommentsOfContentRequest getCommentsOfContent(String contentId) {
        return new GetCommentsOfContentRequest(this, contentId);
    }

    public GetAttachmentsOfContentRequest getAttachmentsOfContent(String contentId) {
        return new GetAttachmentsOfContentRequest(this, contentId);
    }

}