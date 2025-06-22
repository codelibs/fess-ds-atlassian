/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;

public class ConfluenceDataStoreTest extends LastaFluteTestCase {
    public ConfluenceDataStore dataStore;

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
        dataStore = new ConfluenceDataStore();
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
            public void store(DataStoreParams paramMap, Map<String, Object> dataMap) {
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
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("confluence.home", "");
        paramMap.put("confluence.oauth.consumer_key", "");
        paramMap.put("confluence.oauth.private_key", "");
        paramMap.put("confluence.oauth.secret", "");
        paramMap.put("confluence.oauth.access_token", "");
        final Map<String, String> scriptMap = new HashMap<>();
        final Map<String, Object> defaultDataMap = new HashMap<>();

        scriptMap.put("url", "content.view_url");
        scriptMap.put("title", "content.title");
        scriptMap.put("content", "content.body + content.comments");
        scriptMap.put("last_modified", "content.last_modified");

        dataStore.storeData(dataConfig, callback, paramMap, scriptMap, defaultDataMap);

    }

}
