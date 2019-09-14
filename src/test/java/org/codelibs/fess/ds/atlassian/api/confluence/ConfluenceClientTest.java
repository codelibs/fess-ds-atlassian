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

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ConfluenceClientTest extends AtlassianClientTest {
    protected final String confluenceHome = "";

    protected void doProductionTest() {
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put(AUTH_TYPE_PARAM, "oauth");
        paramMap.put(CONSUMER_KEY_PARAM, "");
        paramMap.put(PRIVATE_KEY_PARAM, "");
        paramMap.put(SECRET_PARAM, "");
        paramMap.put(ACCESS_TOKEN_PARAM, "");
        final ConfluenceClient confluenceClient = new ConfluenceClient(paramMap);
        doGetContentsTest(confluenceClient);
        doGetCommentsOfContentTest(confluenceClient);
        doGetAttachmentsOfContentTest(confluenceClient);
        doGetSpacesTest(confluenceClient);
    }

    protected void doGetContentsTest(final ConfluenceClient confluenceClient) {
        final List<Content> contents = confluenceClient.getContents().expand("body.view", "version").execute().getContents();
        if (!contents.isEmpty()) {
            final Content content = contents.get(0);
            assertTrue("not contains \"title\"", content.getTitle() != null);

            assertTrue("not contains \"body\"", content.getBody() != null);
            assertTrue("not contains \"lastModified\"", content.getLastModified() != null);
        }
    }

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
        for (int i = 0; i < contents.size(); i++) {
            final Content content = contents.get(i);
            assertEquals(content.getTitle(), "Title-" + i);
            assertEquals(content.getBody(), "Body-" + i);
            // TODO
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            try {
                assertEquals(content.getLastModified(), (Long)format.parse("2018-08-01T12:34:56.789Z").getTime());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    protected void doGetCommentsOfContentTest(final ConfluenceClient confluenceClient) {
        final List<Content> contents = confluenceClient.getContents().execute().getContents();
        if (!contents.isEmpty()) {
            final String id = contents.get(0).getId();
            final GetCommentsOfContentResponse response =
                    confluenceClient.getCommentsOfContent(id).depth("all").expand("body.view").execute();
            for (final Comment comment : response.getComments()) {
                assertTrue("not contains \"title\"", comment.getTitle() != null);
                assertTrue("not contains \"value\" in \"body.view\"", comment.getBody() != null);
            }
        }
    }

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
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);
            assertEquals("Title-" + i, comment.getTitle());
            assertEquals("<p>Comment-" + i + "</p>", comment.getBody());
        }
    }

    protected void doGetAttachmentsOfContentTest(final ConfluenceClient confluenceClient) {
        final List<Content> contents = confluenceClient.getContents().execute().getContents();
        if (!contents.isEmpty()) {
            final String id =  contents.get(0).getId();
            final GetAttachmentsOfContentResponse response = confluenceClient.getAttachmentsOfContent(id).execute();
            for (final Attachment attachment : response.getAttachments()) {
                assertTrue("not contains \"title\"", attachment.getTitle() != null);
                assertTrue("not contains \"mediaType\" in \"metadata\"", attachment.getMediaType() != null);
                assertTrue("not contains \"download\" in \"_links\"", attachment.getDownloadLink() != null);
            }
        }
    }

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
        final Attachment attachment = attachments.get(0);
        assertEquals("title.txt", attachment.getTitle());
        assertEquals("text/plain", attachment.getMediaType());
        assertEquals("/download", attachment.getDownloadLink());
    }

    protected void doGetSpacesTest(final ConfluenceClient confluenceClient) {
        final GetSpacesResponse response = confluenceClient.getSpaces().expand("description").execute();
        for (final Space space : response.getSpaces()) {
            assertTrue("not contains \"name\"", space.getName() != null);
            assertTrue("not contains \"description\"", space.getDescription() != null);
        }
    }

    public void test_getSpaces_parseResponse() {
        String json = "{" + //
                "  \"results\": [" + //
                "    { \"name\": \"Space-0\" }," + //
                "    { \"name\": \"Space-1\" }" + //
                "  ]" + //
                "}";
        final GetSpacesResponse response = GetSpacesRequest.parseResponse(json);
        final List<Space> spaces = response.getSpaces();
        for (int i = 0; i < spaces.size(); i++) {
            final Space space = spaces.get(i);
            assertEquals(space.getName(), "Space-" + i);
        }
    }

    protected void doGetSpaceTest(final ConfluenceClient confluenceClient) {
        // TODO
    }
    public void test_getSpace_parseResponse() {
        // TODO
    }
}
