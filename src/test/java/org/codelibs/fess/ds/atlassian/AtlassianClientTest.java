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
package org.codelibs.fess.ds.atlassian;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsResponse;
import org.codelibs.fess.ds.atlassian.api.confluence.space.GetSpacesResponse;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClientBuilder;
import org.codelibs.fess.ds.atlassian.api.jira.project.GetProjectsResponse;
import org.codelibs.fess.ds.atlassian.api.jira.search.SearchResponse;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastadi.ContainerTestCase;

public class AtlassianClientTest extends ContainerTestCase {

    public JiraClient jiraClient;
    public ConfluenceClient confluenceClient;

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();

        jiraClient = JiraClient.builder().oAuthToken("", accessToken -> {
            accessToken.consumerKey = "";
            accessToken.signer = JiraClientBuilder.getOAuthRsaSigner("");
            accessToken.transport = new ApacheHttpTransport();
            accessToken.verifier = "";
            accessToken.temporaryToken = "";
        }).build();

        confluenceClient = ConfluenceClient.builder().basicAuth("", "", "").build();
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_get_projects() throws Exception {
        System.out.println("-------- Test GetProjects --------");
        final GetProjectsResponse response = jiraClient.getProjects().execute();
        List<Map<String, Object>> projects = response.getProjects();
        for (Map<String, Object> project : projects) {
            System.out.println("name: " + project.get("name"));
        }
        System.out.println("-------- Test GetProjects --------");
    }

    @SuppressWarnings("unchecked")
    public void test_search() throws Exception {
        System.out.println("-------- Test Search --------");
        final SearchResponse response = jiraClient.search().fields("summary", "description", "comment", "updated")
                .execute();
        for (Map<String, Object> issue : response.getIssues()) {
            final Map<String, Object> fields = (Map<String, Object>) issue.get("fields");
            System.out.println("summary: " + fields.get("summary"));
            String desc = (String) fields.get("description");
            System.out.println("description: " + desc.substring(0, Math.min(11, desc.length())));
            List<Map<String, Object>> comments = (List<Map<String, Object>>) ((Map<String, Object>) fields
                    .get("comment")).get("comments");
            if (comments.size() > 0) {
                System.out.println("comment:");
                for (Map<String, Object> comment : comments) {
                    String body = ((String) comment.get("body")).split("\n")[0];
                    System.out.println(body.length() <= 10 ? body : (body.substring(0, 10) + "..."));
                }
            }
            System.out.println("updated: "
                    + new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX").parse((String) fields.get("updated")));
            System.out.println("----");
        }
        System.out.println("-------- Test Search --------");
    }

    @SuppressWarnings("unchecked")
    public void test_get_contents() throws Exception {
        System.out.println("-------- Test GetContents --------");
        final GetContentsResponse response = confluenceClient.getContents().expand("space", "version", "body.view")
                .execute();
        for (Map<String, Object> content : response.getContents()) {
            System.out.println("id: " + content.get("id"));
            System.out.println("type: " + content.get("type"));
            System.out.println("title: " + content.get("title"));
            Map<String, Object> version = (Map<String, Object>) content.get("version");
            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = format.parse((String) version.get("when"));
            System.out.println("lastUpdated: " + date);
            Map<String, Object> bodyObj = (Map<String, Object>) content.get("body");
            String body = (String) ((Map<String, Object>) bodyObj.get("view")).get("value");
            body = body.split("\n")[0];
            System.out.println(body.length() <= 10 ? body : (body.substring(0, 10) + "..."));
            System.out.println("----");
        }
        System.out.println("-------- Test GetContents --------");
    }

    public void test_get_spaces() throws Exception {
        System.out.println("-------- Test GetSpaces --------");
        final GetSpacesResponse response = confluenceClient.getSpaces().execute();
        for (Map<String, Object> space : response.getSpaces()) {
            System.out.println("name: " + space.get("name"));
        }
        System.out.println("-------- Test GetSpaces --------");
    }
}
