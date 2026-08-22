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

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import org.codelibs.fess.ds.atlassian.api.AtlassianClientTest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Attachment;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesResponse;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfluenceClientTest extends AtlassianClientTest {
    protected final String confluenceHome = "";

    @Test
    public void test_getContents_parseResponse() {
        final String json = "{" + //
                "  \"results\": [{" + //
                "      \"title\": \"Title-0\"," + //
                "      \"body\": { \"view\": { \"value\": \"Body-0\" } }," + //
                "      \"version\": { \"when\": \"2018-08-01T12:34:56.789Z\" }" + //
                "    }," + //
                "    {" + //
                "      \"title\": \"Title-1\"," + //
                "      \"body\": { \"view\": { \"value\": \"Body-1\" } }," + //
                "      \"version\": { \"when\": \"2018-08-01T12:34:56.789Z\" }" + //
                "    }" + //
                "  ]" + //
                "}";
        final GetContentsResponse response = GetContentsRequest.parseResponse(json);
        final List<Content> contents = response.getContents();
        Assertions.assertEquals(2, contents.size());
        for (int i = 0; i < contents.size(); i++) {
            final Content content = contents.get(i);
            Assertions.assertEquals("Title-" + i, content.getTitle());
            Assertions.assertEquals("Body-" + i, content.getBody());
            // TODO
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                Assertions.assertEquals((Long) format.parse("2018-08-01T12:34:56.789Z").getTime(), content.getLastModified());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void test_getCommentsOfContent_parseResponse() {
        String json = "{" + //
                "  \"results\": [" + //
                "    {" + //
                "      \"title\": \"Title-0\"," + //
                "      \"body\": { \"view\": { \"value\": \"<p>Comment-0</p>\" } }" + //
                "    }," + //
                "    {" + //
                "      \"title\": \"Title-1\"," + //
                "      \"body\": { \"view\": { \"value\": \"<p>Comment-1</p>\" } }" + //
                "    }" + //
                "  ]" + //
                "}";
        final GetCommentsOfContentResponse response = GetCommentsOfContentRequest.parseResponse(json);
        final List<Comment> comments = response.getComments();
        Assertions.assertEquals(2, comments.size());
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);
            assertEquals("Title-" + i, comment.getTitle());
            assertEquals("<p>Comment-" + i + "</p>", comment.getBody());
        }
    }

    @Test
    public void test_getAttachmentsOfContent_parseResponse() {
        String json = "{" + //
                "  \"results\": [" + //
                "    {" + //
                "      \"title\": \"title.txt\"," + //
                "      \"metadata\": { \"mediaType\": \"text/plain\" }," + //
                "      \"_links\": {" + //
                "        \"download\": \"/download\"" + //
                "      }" + //
                "    }" + //
                "  ]" + //
                "}";
        final GetAttachmentsOfContentResponse response = GetAttachmentsOfContentRequest.parseResponse(json);
        final List<Attachment> attachments = response.getAttachments();
        Assertions.assertEquals(1, attachments.size());
        final Attachment attachment = attachments.get(0);
        assertEquals("title.txt", attachment.getTitle());
        assertEquals("text/plain", attachment.getMediaType());
        assertEquals("/download", attachment.getDownloadLink());
    }

    @Test
    public void test_getSpaces_parseResponse() {
        final String json = "{" + //
                "  \"results\": [" + //
                "    { \"name\": \"Space-0\" }," + //
                "    { \"name\": \"Space-1\" }" + //
                "  ]" + //
                "}";
        final GetSpacesResponse response = GetSpacesRequest.parseResponse(json);
        final List<Space> spaces = response.getSpaces();
        Assertions.assertEquals(2, spaces.size());
        for (int i = 0; i < spaces.size(); i++) {
            final Space space = spaces.get(i);
            Assertions.assertEquals("Space-" + i, space.getName());
        }
    }

}
