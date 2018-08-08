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

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastadi.ContainerTestCase;

public class JiraDataStoreTest extends ContainerTestCase {
    public JiraDataStore dataStore;

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
        dataStore = new JiraDataStore();
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_storeData() {
        // doStoreDataTest();
    }

    protected void doStoreDataTest() {

        final DataConfig dataConfig = new DataConfig();
        final IndexUpdateCallback callback = new IndexUpdateCallback() {
            @Override
            public void store(Map<String, String> paramMap, Map<String, Object> dataMap) {
                System.out.println(dataMap);
            }

            @Override
            public long getExecuteTime() {
                return 0;
            }

            @Override
            public long getDocumentSize() {
                return 0;
            }

            @Override
            public void commit() {
            }
        };
        final Map<String, String> paramMap = new HashMap<>();
        paramMap.put("jira.home", "");
        paramMap.put("jira.oauth.consumer_key", "");
        paramMap.put("jira.oauth.private_key", "");
        paramMap.put("jira.oauth.secret", "");
        paramMap.put("jira.oauth.access_token", "");
        // paramMap.put("jira.issue.jql", "");
        final Map<String, String> scriptMap = new HashMap<>();
        final Map<String, Object> defaultDataMap = new HashMap<>();

        scriptMap.put("url", "issue.view_url");
        scriptMap.put("title", "issue.summary");
        scriptMap.put("content", "issue.description + issue.comments");   
        scriptMap.put("last_modified", "issue.last_modified");        

        dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

    }

}
