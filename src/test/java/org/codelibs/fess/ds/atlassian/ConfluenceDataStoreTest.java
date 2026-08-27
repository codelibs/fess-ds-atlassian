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

import org.junit.jupiter.api.TestInfo;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.container.StandardCrawlerContainer;
import org.codelibs.fess.crawler.extractor.ExtractorFactory;
import org.codelibs.fess.crawler.extractor.impl.TikaExtractor;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.crawler.helper.ContentLengthHelper;
import org.codelibs.fess.crawler.helper.impl.MimeTypeHelperImpl;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.content.GetContentsRequest;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.opensearch.config.exentity.CrawlingConfig;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.opensearch.config.exentity.FailureUrl;
import org.codelibs.fess.script.ScriptEngineFactory;
import org.codelibs.fess.script.groovy.GroovyEngine;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConfluenceDataStoreTest extends UnitDsTestCase {
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
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        dataStore = new ConfluenceDataStore();

        // storeData -> processContent uses CrawlerStatsHelper (which in turn uses SystemHelper).
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

        // processContent extracts text via ComponentUtil.getExtractorFactory(). Wire a real
        // ExtractorFactory + TikaExtractor through a StandardCrawlerContainer so the @Resource
        // CrawlerContainer field injection happens, matching fess-crawler's own ExtractorFactoryTest.
        final StandardCrawlerContainer crawlerContainer = new StandardCrawlerContainer().singleton("tikaExtractor", TikaExtractor.class)
                .singleton("mimeTypeHelper", MimeTypeHelperImpl.class)
                .singleton("contentLengthHelper", ContentLengthHelper.class)
                .singleton("extractorFactory", ExtractorFactory.class);
        ComponentUtil.register(crawlerContainer.getComponent("extractorFactory"), "extractorFactory");

        // convertValue() evaluates script templates like "content.title" through the default
        // (groovy) script engine.
        final ScriptEngineFactory scriptEngineFactory = new ScriptEngineFactory();
        ComponentUtil.register(scriptEngineFactory, "scriptEngineFactory");
        final GroovyEngine groovyEngine = new GroovyEngine();
        groovyEngine.init();
        groovyEngine.register();
    }

    @Override
    public void tearDown(TestInfo testInfo) throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown(testInfo);
    }

    @Test
    public void test_storeData_fetches_each_page_once() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/wiki/rest/api/search",
                    req -> MockAtlassianServer.json("{\"results\":[{\"content\":{"
                            + "\"id\":\"1\",\"type\":\"blogpost\",\"title\":\"Blog-1\",\"space\":{\"key\":\"SP\"},"
                            + "\"body\":{\"view\":{\"value\":\"body\"}},\"version\":{\"when\":\"2026-08-01T00:00:00.000Z\"}}}],"
                            + "\"_links\":{}}"));

            final DataStoreParams paramMap = new DataStoreParams();
            paramMap.put("home", server.getBaseUrl());
            paramMap.put("deployment", "cloud");
            paramMap.put("auth_type", "basic");
            paramMap.put("basic.username", "user");
            paramMap.put("basic.password", "pass");

            final List<Map<String, Object>> stored = new ArrayList<>();
            final IndexUpdateCallback callback = new IndexUpdateCallback() {
                @Override
                public void store(final DataStoreParams params, final Map<String, Object> dataMap) {
                    stored.add(dataMap);
                }

                @Override
                public long getExecuteTime() {
                    return 0;
                }

                @Override
                public long getDocumentSize() {
                    return stored.size();
                }

                @Override
                public void commit() {
                    // no-op
                }
            };

            final Map<String, String> scriptMap = new HashMap<>();
            scriptMap.put("title", "content.title");

            new ConfluenceDataStore().storeData(new DataConfig(), callback, paramMap, scriptMap, new HashMap<>());

            Assertions.assertEquals(1, stored.size(), "a blogpost must not be indexed twice");

            // getContentComments() legitimately issues its own request to this same
            // "/rest/api/search" endpoint (Confluence's search API is unified across content
            // types via CQL), so the raw total is 2: one to list content, one to fetch content
            // id=1's comments. Isolate the content-listing requests (whose CQL does not target
            // comments) to prove that call happens once, not once for "page" and again for
            // "blogpost".
            final long contentSearchRequests =
                    server.getRequests().stream().filter(r -> !r.query().getOrDefault("cql", "").contains("type=\"comment\"")).count();
            Assertions.assertEquals(1, contentSearchRequests, "the content search endpoint must be called once, not once per content type");
        }
    }

    @Test
    public void test_content_view_url_is_cloud_style_on_cloud() throws Exception {
        assertContentViewUrl("cloud", "https://example.atlassian.net", "https://example.atlassian.net/wiki/spaces/SP/pages/1");
    }

    @Test
    public void test_content_view_url_is_viewpage_action_on_datacenter() throws Exception {
        assertContentViewUrl("datacenter", "https://wiki.example.com", "https://wiki.example.com/pages/viewpage.action?pageId=1");
    }

    private void assertContentViewUrl(final String deployment, final String home, final String expected) {
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("home", home);
        paramMap.put("deployment", deployment);
        paramMap.put("auth_type", "basic");
        paramMap.put("basic.username", "user");
        paramMap.put("basic.password", "pass");

        final Content content =
                GetContentsRequest.parseResponse("{\"results\":[{\"content\":{\"id\":\"1\",\"title\":\"T\",\"space\":{\"key\":\"SP\"}}}]}")
                        .getContents()
                        .get(0);

        try (ConfluenceClient client = new ConfluenceClient(new DataConfig(), paramMap)) {
            Assertions.assertEquals(expected, new ConfluenceDataStore().getContentViewUrl(content, client));
        }
    }

}
