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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.codelibs.fess.app.service.FailureUrlService;
import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.helper.CrawlerStatsHelper;
import org.codelibs.fess.helper.SystemHelper;
import org.codelibs.fess.opensearch.config.exentity.CrawlingConfig;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.opensearch.config.exentity.FailureUrl;
import org.codelibs.fess.script.ScriptEngineFactory;
import org.codelibs.fess.script.javascript.JavaScriptEngine;
import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

public class JiraDataStoreTest extends UnitDsTestCase {

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
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        dataStore = new JiraDataStore();

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

    @Override
    public void tearDown(TestInfo testInfo) throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown(testInfo);
    }

    @Test
    public void test_script_cannot_read_credentials() throws Exception {
        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer.json(
                    "{\"issues\":[{\"id\":\"1\",\"key\":\"ABC-1\",\"fields\":{\"summary\":\"S\",\"updated\":\"2026-08-01T00:00:00.000+0000\"}}]}"));
            server.on("/rest/api/3/issue/1/comment", req -> MockAtlassianServer.json("{\"comments\":[]}"));

            final DataStoreParams paramMap = new DataStoreParams();
            paramMap.put("script_type", "javascript");
            paramMap.put("home", server.getBaseUrl());
            paramMap.put("deployment", "cloud");
            paramMap.put("auth_type", "basic");
            paramMap.put("basic.username", "user");
            paramMap.put("basic.password", "s3cr3t");

            final List<Map<String, Object>> stored = new ArrayList<>();
            final IndexUpdateCallback callback = new IndexUpdateCallback() {
                @Override
                public void store(final DataStoreParams params, final Map<String, Object> dataMap) {
                    stored.add(dataMap);
                }

                @Override
                public long getDocumentSize() {
                    return stored.size();
                }

                @Override
                public long getExecuteTime() {
                    return 0;
                }

                @Override
                public void commit() {
                    // no-op
                }
            };

            final Map<String, String> scriptMap = new HashMap<>();
            scriptMap.put("title", "issue.summary");
            scriptMap.put("leaked", "basic.password");

            new JiraDataStore().storeData(new DataConfig(), callback, paramMap, scriptMap, new HashMap<>());

            // What makes this test discriminate is the short-circuit at the top of
            // AbstractDataStore#convertValue, which runs before the script engine is ever
            // consulted:
            //
            //     if (paramMap.containsKey(template)) {
            //         return paramMap.get(template);
            //     }
            //
            // Before the fix the script's result map was seeded from paramMap, so the template
            // string "basic.password" was a literal key in that map and convertValue handed back
            // the raw credential without any engine being invoked at all - storing one document
            // whose "leaked" field was the password in clear text. The fix removes the credentials
            // from that map, so the template reaches the engine, which cannot resolve the unbound
            // "basic" variable.
            //
            // An engine that cannot evaluate a template now raises ScriptEngineException instead
            // of returning null - the bundled JavaScript engine and fess-script-groovy both made
            // that change so that a script which does not even compile stops being recorded as a
            // success. processIssue therefore discards the whole document rather than writing it
            // without the failed field, which is why nothing is stored at all here.
            Assertions.assertTrue(stored.isEmpty(), "a credential-bearing template must not produce a document");
            for (final Map<String, Object> document : stored) {
                Assertions.assertFalse(document.containsKey("leaked"), "credentials must not be reachable from the script");
                for (final Object value : document.values()) {
                    Assertions.assertNotEquals("s3cr3t", value, "the password must not appear anywhere in the document");
                }
            }
        }
    }

    @Test
    public void test_getExtractedTextFromAdf_singleParagraph() {
        final Map<String, Object> adf = new LinkedHashMap<>();
        adf.put("type", "doc");
        adf.put("version", 1);

        final Map<String, Object> textNode = new LinkedHashMap<>();
        textNode.put("type", "text");
        textNode.put("text", "Hello World");

        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(textNode));

        adf.put("content", List.of(paragraph));

        final String result = dataStore.getExtractedTextFromAdf(adf);
        assertEquals("Hello World", result);
    }

    @Test
    public void test_getExtractedTextFromAdf_multipleParagraphs() {
        final Map<String, Object> adf = new LinkedHashMap<>();
        adf.put("type", "doc");
        adf.put("version", 1);

        final Map<String, Object> text1 = new LinkedHashMap<>();
        text1.put("type", "text");
        text1.put("text", "First paragraph");

        final Map<String, Object> para1 = new LinkedHashMap<>();
        para1.put("type", "paragraph");
        para1.put("content", List.of(text1));

        final Map<String, Object> text2 = new LinkedHashMap<>();
        text2.put("type", "text");
        text2.put("text", "Second paragraph");

        final Map<String, Object> para2 = new LinkedHashMap<>();
        para2.put("type", "paragraph");
        para2.put("content", List.of(text2));

        adf.put("content", List.of(para1, para2));

        final String result = dataStore.getExtractedTextFromAdf(adf);
        assertEquals("First paragraph\nSecond paragraph", result);
    }

    @Test
    public void test_getExtractedTextFromAdf_withHeading() {
        final Map<String, Object> adf = new LinkedHashMap<>();
        adf.put("type", "doc");
        adf.put("version", 1);

        final Map<String, Object> headingText = new LinkedHashMap<>();
        headingText.put("type", "text");
        headingText.put("text", "Title");

        final Map<String, Object> heading = new LinkedHashMap<>();
        heading.put("type", "heading");
        heading.put("content", List.of(headingText));

        final Map<String, Object> paraText = new LinkedHashMap<>();
        paraText.put("type", "text");
        paraText.put("text", "Body text");

        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(paraText));

        adf.put("content", List.of(heading, paragraph));

        final String result = dataStore.getExtractedTextFromAdf(adf);
        assertEquals("Title\nBody text", result);
    }

    @Test
    public void test_getExtractedTextFromAdf_emptyDoc() {
        final Map<String, Object> adf = new LinkedHashMap<>();
        adf.put("type", "doc");
        adf.put("version", 1);
        adf.put("content", new ArrayList<>());

        final String result = dataStore.getExtractedTextFromAdf(adf);
        assertEquals("", result);
    }

    @Test
    public void test_getExtractedTextFromAdf_multipleTextNodesInParagraph() {
        final Map<String, Object> adf = new LinkedHashMap<>();
        adf.put("type", "doc");
        adf.put("version", 1);

        final Map<String, Object> text1 = new LinkedHashMap<>();
        text1.put("type", "text");
        text1.put("text", "Hello ");

        final Map<String, Object> text2 = new LinkedHashMap<>();
        text2.put("type", "text");
        text2.put("text", "World");

        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(text1, text2));

        adf.put("content", List.of(paragraph));

        final String result = dataStore.getExtractedTextFromAdf(adf);
        assertEquals("Hello World", result);
    }

    @Test
    public void test_getExtractedTextFromAdf_separates_table_cells() {
        final Map<String, Object> cellA = adfCell("Alpha");
        final Map<String, Object> cellB = adfCell("Beta");
        final Map<String, Object> row = new LinkedHashMap<>();
        row.put("type", "tableRow");
        row.put("content", List.of(cellA, cellB));
        final Map<String, Object> table = new LinkedHashMap<>();
        table.put("type", "table");
        table.put("content", List.of(row));
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(table));

        final String text = dataStore.getExtractedTextFromAdf(doc);
        Assertions.assertFalse(text.contains("AlphaBeta"), "cells must not run together: " + text);
        Assertions.assertTrue(text.contains("Alpha"), text);
        Assertions.assertTrue(text.contains("Beta"), text);
    }

    @Test
    public void test_getExtractedTextFromAdf_separates_list_items() {
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(adfListItem("First"), adfListItem("Second")));

        final String text = dataStore.getExtractedTextFromAdf(doc);
        Assertions.assertFalse(text.contains("FirstSecond"), "list items must not run together: " + text);
    }

    private static Map<String, Object> adfText(final String value) {
        final Map<String, Object> node = new LinkedHashMap<>();
        node.put("type", "text");
        node.put("text", value);
        return node;
    }

    private static Map<String, Object> adfCell(final String value) {
        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(adfText(value)));
        final Map<String, Object> cell = new LinkedHashMap<>();
        cell.put("type", "tableCell");
        cell.put("content", List.of(paragraph));
        return cell;
    }

    private static Map<String, Object> adfListItem(final String value) {
        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(adfText(value)));
        final Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "listItem");
        item.put("content", List.of(paragraph));
        return item;
    }

    @Test
    public void test_getExtractedTextFromAdf_separates_code_blocks() {
        final Map<String, Object> codeBlockA = new LinkedHashMap<>();
        codeBlockA.put("type", "codeBlock");
        codeBlockA.put("content", List.of(adfText("Alpha")));
        final Map<String, Object> codeBlockB = new LinkedHashMap<>();
        codeBlockB.put("type", "codeBlock");
        codeBlockB.put("content", List.of(adfText("Beta")));
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(codeBlockA, codeBlockB));

        final String text = dataStore.getExtractedTextFromAdf(doc);
        Assertions.assertFalse(text.contains("AlphaBeta"), "adjacent code blocks must not run together: " + text);
        Assertions.assertTrue(text.contains("Alpha"), text);
        Assertions.assertTrue(text.contains("Beta"), text);
    }

    @Test
    public void test_getExtractedTextFromAdf_includesMentionText() {
        final Map<String, Object> mentionAttrs = new LinkedHashMap<>();
        mentionAttrs.put("id", "abc-123");
        mentionAttrs.put("text", "@Jane Doe");
        final Map<String, Object> mention = new LinkedHashMap<>();
        mention.put("type", "mention");
        mention.put("attrs", mentionAttrs);
        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(adfText("Assigned to "), mention, adfText(" for review")));
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(paragraph));

        final String text = dataStore.getExtractedTextFromAdf(doc);
        Assertions.assertTrue(text.contains("@Jane Doe"), "mention text must be extracted: " + text);
        Assertions.assertFalse(text.contains("to@Jane"), "mention must not fuse with neighboring text: " + text);
        Assertions.assertFalse(text.contains("Doefor"), "mention must not fuse with neighboring text: " + text);
    }

    @Test
    public void test_getExtractedTextFromAdf_includesInlineCardUrl() {
        final Map<String, Object> inlineCardAttrs = new LinkedHashMap<>();
        inlineCardAttrs.put("url", "https://example.atlassian.net/browse/FOO-1");
        final Map<String, Object> inlineCard = new LinkedHashMap<>();
        inlineCard.put("type", "inlineCard");
        inlineCard.put("attrs", inlineCardAttrs);
        final Map<String, Object> paragraph = new LinkedHashMap<>();
        paragraph.put("type", "paragraph");
        paragraph.put("content", List.of(adfText("See "), inlineCard));
        final Map<String, Object> doc = new LinkedHashMap<>();
        doc.put("type", "doc");
        doc.put("version", 1);
        doc.put("content", List.of(paragraph));

        final String text = dataStore.getExtractedTextFromAdf(doc);
        Assertions.assertTrue(text.contains("https://example.atlassian.net/browse/FOO-1"), "inline card url must be extracted: " + text);
    }
}
