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
package org.codelibs.fess.ds.atlassian;

import com.google.api.client.http.apache.ApacheHttpTransport;
import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetAttachmentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.content.child.GetCommentsOfContentResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Comment;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesResponse;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class ConfluenceClientTest extends AtlassianClientTest {

    protected void doProductionTest() {
        final ConfluenceClient confluenceClient = new ConfluenceClient(AtlassianClient.builder().oAuthToken("", accessToken -> {
            accessToken.consumerKey = "";
            accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner("");
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = "";
            accessToken.temporaryToken = "";
        }).build());
        doGetContentsTest(confluenceClient);
        doGetCommentsOfContentTest(confluenceClient);
        doGetAttachmentsOfContentTest(confluenceClient);
        doGetSpacesTest(confluenceClient);
    }

    @SuppressWarnings("unchecked")
    protected void doGetContentsTest(final ConfluenceClient confluenceClient) {
        final List<Content> contents = confluenceClient.getContents().expand("body.view", "version").execute().getContents();
        if (!contents.isEmpty()) {
            final Content content = contents.get(0);
            assertTrue("not contains \"title\"", content.getTitle() != null);

            assertTrue("not contains \"body\"", content.getBody() != null);
            assertTrue("not contains \"lastModified\"", content.getLastModified() != null);
        }
    }

    @SuppressWarnings("unchecked")
    public void test_getContents_fromJson() {
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
        final GetContentsResponse response = GetContentsRequest.fromJson(json);
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

    @SuppressWarnings("unchecked")
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

    @SuppressWarnings("unchecked")
    public void test_getCommentsOfContent_fromJson() {
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
        final GetCommentsOfContentResponse response = GetCommentsOfContentRequest.fromJson(json);
        final List<Comment> comments = response.getComments();
        for (int i = 0; i < comments.size(); i++) {
            final Comment comment = comments.get(i);
            assertEquals("Title-" + i, comment.getTitle());
            assertEquals("<p>Comment-" + i + "</p>", comment.getBody());
        }
    }

    @SuppressWarnings("unchecked")
    protected void doGetAttachmentsOfContentTest(final ConfluenceClient confluenceClient) {
        final List<Content> contents = confluenceClient.getContents().execute().getContents();
        if (!contents.isEmpty()) {
            final String id =  contents.get(0).getId();
            final GetAttachmentsOfContentResponse response = confluenceClient.getAttachmentsOfContent(id).execute();
            for (final Map<String, Object> attachment : response.getAttachments()) {
                assertTrue("not contains \"title\"", attachment.containsKey("title"));
                assertTrue("not contains \"metadata\"", attachment.containsKey("metadata"));
                final Map<String, Object> metadata = (Map<String, Object>) attachment.get("metadata");
                assertTrue("not contains \"mediaType\" in \"metadata\"", metadata.containsKey("mediaType"));
                assertTrue("not contains \"_links\"", attachment.containsKey("_links"));
                final Map<String, Object> links = (Map<String, Object>) attachment.get("_links");
                assertTrue("not contains \"download\" in \"_links\"", links.containsKey("download"));
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void test_getAttachmentsOfContent_fromJson() {
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
        final GetAttachmentsOfContentResponse response = GetAttachmentsOfContentRequest.fromJson(json);
        final List<Map<String, Object>> attachments = response.getAttachments();
        final Map<String, Object> attachment = attachments.get(0);
        assertEquals(attachment.get("title"), "title.txt");
        final Map<String, Object> metadata = (Map<String, Object>) attachment.get("metadata");
        assertEquals(metadata.get("mediaType"), "text/plain");
        final Map<String, Object> links = (Map<String, Object>) attachment.get("_links");
        assertEquals(links.get("download"), "/download");
    }

    protected void doGetSpacesTest(final ConfluenceClient confluenceClient) {
        final GetSpacesResponse response = confluenceClient.getSpaces().expand("description").execute();
        for (final Map<String, Object> space : response.getSpaces()) {
            assertTrue("not contains \"name\"", space.containsKey("name"));
            assertTrue("not contains \"description\"", space.containsKey("description"));
        }
    }

    public void test_getSpaces_fromJson() {
        String json = "{" + //
                "  \"results\": [" + //
                "    { \"name\": \"Space-0\" }," + //
                "    { \"name\": \"Space-1\" }" + //
                "  ]" + //
                "}";
        final GetSpacesResponse response = GetSpacesRequest.fromJson(json);
        final List<Map<String, Object>> spaces = response.getSpaces();
        for (int i = 0; i < spaces.size(); i++) {
            final Map<String, Object> space = spaces.get(i);
            assertEquals(space.get("name"), "Space-" + i);
        }
    }
}