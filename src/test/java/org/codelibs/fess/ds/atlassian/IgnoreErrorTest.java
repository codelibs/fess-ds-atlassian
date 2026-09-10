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
package org.codelibs.fess.ds.atlassian;

import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.exception.DataStoreCrawlingException;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.opensearch.config.exentity.CrawlingConfig;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.opensearch.config.exentity.FailureUrl;
import org.codelibs.fess.script.ScriptEngineFactory;
import org.codelibs.fess.script.javascript.JavaScriptEngine;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class IgnoreErrorTest extends UnitDsTestCase {

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);

        // storeData -> processIssue uses CrawlerStatsHelper (which in turn uses SystemHelper).
        // Register initialized instances so the full pipeline can run in the test.
        ComponentUtil.register(new SystemHelper(), "systemHelper");
        final CrawlerStatsHelper crawlerStatsHelper = new CrawlerStatsHelper();
        crawlerStatsHelper.init();
        ComponentUtil.register(crawlerStatsHelper, "crawlerStatsHelper");

        // On the error/abort path storeData records the failure via FailureUrlService. The real
        // implementation needs OpenSearch, so register a no-op stub to keep this a self-contained
        // unit test, under the class canonical name so ComponentUtil.getComponent(Class) resolves it.
        ComponentUtil.register(new FailureUrlService() {
            @Override
            public FailureUrl store(final CrawlingConfig crawlingConfig, final String errorName, final String url, final Throwable e) {
                return null;
            }
        }, FailureUrlService.class.getCanonicalName());

        // createConfigMap() looks up a UrlFilter component; register a permissive fake (this test
        // never sets include_pattern/exclude_pattern) under the class canonical name, the same key
        // ComponentUtil.getComponent(UrlFilter.class) resolves.
        ComponentUtil.register(new UrlFilter() {
            @Override
            public void init(final String sessionId) {
                // no-op
            }

            @Override
            public boolean match(final String url) {
                return true;
            }

            @Override
            public void addInclude(final String urlPattern) {
                // no-op: not used by this test
            }

            @Override
            public void addExclude(final String urlPattern) {
                // no-op: not used by this test
            }

            @Override
            public void processUrl(final String url) {
                // no-op: not used by this test
            }

            @Override
            public void clear() {
                // no-op
            }
        }, UrlFilter.class.getCanonicalName());

        // convertValue() evaluates script templates like "issue.summary" through the script engine named
        // by script_type. Groovy now lives in the fess-script-groovy plugin and is not on the test
        // classpath, so the tests ask for the JavaScript engine that ships in fess core.
        final ScriptEngineFactory scriptEngineFactory = new ScriptEngineFactory();
        ComponentUtil.register(scriptEngineFactory, "scriptEngineFactory");
        final JavaScriptEngine javaScriptEngine = new JavaScriptEngine();
        javaScriptEngine.init();
        javaScriptEngine.register();
    }

    private static IndexUpdateCallback throwingCallback() {
        return new IndexUpdateCallback() {
            @Override
            public void store(final DataStoreParams params, final Map<String, Object> dataMap) {
                throw new IllegalStateException("indexing blew up");
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
                // no-op
            }
        };
    }

    private static DataStoreParams params(final String home, final String ignoreError) {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", home);
        p.put("deployment", "cloud");
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        p.put("ignore_error", ignoreError);
        p.put("script_type", "javascript");
        return p;
    }

    private static String oneIssue() {
        return "{\"issues\":[{\"id\":\"1\",\"key\":\"ABC-1\",\"fields\":{\"summary\":\"S\"}}],\"isLast\":true}";
    }

    @Test
    public void test_ignore_error_true_keeps_crawling() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer.json(oneIssue()));
            server.on("/rest/api/3/issue/1/comment", req -> MockAtlassianServer.json("{\"comments\":[],\"total\":0}"));

            final Map<String, String> scriptMap = new HashMap<>();
            scriptMap.put("title", "issue.summary");

            new JiraDataStore().storeData(new DataConfig(), throwingCallback(), params(server.getBaseUrl(), "true"), scriptMap,
                    new HashMap<>());
            // Reaching here without an exception is the assertion: the failure was recorded and swallowed.
        }
    }

    @Test
    public void test_ignore_error_false_aborts_the_crawl() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer.json(oneIssue()));
            server.on("/rest/api/3/issue/1/comment", req -> MockAtlassianServer.json("{\"comments\":[],\"total\":0}"));

            final Map<String, String> scriptMap = new HashMap<>();
            scriptMap.put("title", "issue.summary");

            final DataStoreCrawlingException thrown = Assertions.assertThrows(DataStoreCrawlingException.class, () -> new JiraDataStore()
                    .storeData(new DataConfig(), throwingCallback(), params(server.getBaseUrl(), "false"), scriptMap, new HashMap<>()));
            Assertions.assertTrue(thrown.aborted(), "ignore_error=false must abort the crawl, not just report");
        }
    }
}
