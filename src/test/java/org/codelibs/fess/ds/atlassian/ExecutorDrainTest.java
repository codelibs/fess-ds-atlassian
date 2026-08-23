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
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.codelibs.fess.crawler.filter.UrlFilter;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Content;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.atlassian.api.jira.domain.Issue;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;

/**
 * Guards the drain at the end of {@code storeData}.
 *
 * <p>Retry with backoff means one request can hold a worker for minutes -- and for as long as
 * the server asks when it supplies {@code Retry-After}. A single bounded
 * {@code awaitTermination} window would therefore let the {@code finally} block interrupt a
 * worker that is correctly waiting out a rate limit, losing that document. The drain must poll
 * until the pool has actually terminated.</p>
 */
public class ExecutorDrainTest extends UnitDsTestCase {

    @Override
    protected String prepareConfigFile() {
        return "test_app.xml";
    }

    @Override
    protected boolean isSuppressTestCaseTransaction() {
        return true;
    }

    @Override
    public void setUp(final TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
        // createConfigMap() looks up a UrlFilter component; this test never sets
        // include_pattern/exclude_pattern, so a permissive fake is enough.
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
                // no-op
            }

            @Override
            public void addExclude(final String urlPattern) {
                // no-op
            }

            @Override
            public void processUrl(final String url) {
                // no-op
            }

            @Override
            public void clear() {
                // no-op
            }
        }, UrlFilter.class.getCanonicalName());
    }

    /**
     * Stands in for a pool whose only worker is still waiting out a rate limit: the first two
     * polls report "not terminated". The worker is released on the second poll, so a drain that
     * keeps polling finishes on the third, while a drain that gives up after one poll reaches
     * {@code shutdownNow()} with the worker still blocked.
     */
    private static final class SlowDrainExecutor extends ThreadPoolExecutor {

        private final AtomicInteger polls = new AtomicInteger();

        private final CountDownLatch gate;

        SlowDrainExecutor(final CountDownLatch gate) {
            super(1, 1, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue<Runnable>(1), new ThreadPoolExecutor.CallerRunsPolicy());
            this.gate = gate;
        }

        @Override
        public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
            final int poll = polls.incrementAndGet();
            if (poll <= 2) {
                if (poll == 2) {
                    gate.countDown();
                }
                // Keep the suite fast: report "not yet" after 50 ms instead of the caller's window.
                super.awaitTermination(50L, TimeUnit.MILLISECONDS);
                return false;
            }
            return super.awaitTermination(timeout, unit);
        }
    }

    private static IndexUpdateCallback noopCallback() {
        return new IndexUpdateCallback() {
            @Override
            public void store(final DataStoreParams params, final Map<String, Object> dataMap) {
                // no-op: the per-document work is stubbed out in the data store subclasses below
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

    private static DataStoreParams params(final String home) {
        final DataStoreParams p = new DataStoreParams();
        p.put("home", home);
        p.put("deployment", "cloud");
        p.put("auth_type", "basic");
        p.put("basic.username", "user");
        p.put("basic.password", "pass");
        p.put("read_interval", "0");
        return p;
    }

    private static void awaitGate(final CountDownLatch gate, final AtomicInteger processed) {
        try {
            if (gate.await(5L, TimeUnit.SECONDS)) {
                processed.incrementAndGet();
            }
        } catch (final InterruptedException e) {
            // Interrupted by shutdownNow() before the "rate limit" cleared: the document is lost.
            Thread.currentThread().interrupt();
        }
    }

    @Test
    public void test_jira_drain_waits_for_a_worker_that_is_still_backing_off() throws Exception {
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger processed = new AtomicInteger();
        final SlowDrainExecutor executor = new SlowDrainExecutor(gate);

        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/rest/api/3/search/jql", req -> MockAtlassianServer
                    .json("{\"issues\":[{\"id\":\"1\",\"key\":\"ABC-1\",\"fields\":{\"summary\":\"S\"}}],\"isLast\":true}"));

            final JiraDataStore dataStore = new JiraDataStore() {
                @Override
                protected ExecutorService newFixedThreadPool(final int nThreads) {
                    return executor;
                }

                @Override
                protected void processIssue(final DataConfig dataConfig, final IndexUpdateCallback callback,
                        final Map<String, Object> configMap, final DataStoreParams paramMap, final Map<String, String> scriptMap,
                        final Map<String, Object> defaultDataMap, final JiraClient client, final Issue issue) {
                    awaitGate(gate, processed);
                }
            };

            dataStore.storeData(new DataConfig(), noopCallback(), params(server.getBaseUrl()), new HashMap<>(), new HashMap<>());
        }

        Assertions.assertEquals(1, processed.get(), "the drain must not interrupt a worker that is still waiting out a rate limit");
        Assertions.assertTrue(executor.isTerminated(), "the drain must return only once the pool has actually terminated");
    }

    @Test
    public void test_confluence_drain_waits_for_a_worker_that_is_still_backing_off() throws Exception {
        final CountDownLatch gate = new CountDownLatch(1);
        final AtomicInteger processed = new AtomicInteger();
        final SlowDrainExecutor executor = new SlowDrainExecutor(gate);

        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/wiki/rest/api/search",
                    req -> MockAtlassianServer.json("{\"results\":[{\"content\":{\"id\":\"1\",\"title\":\"P1\","
                            + "\"space\":{\"key\":\"SP\"},\"body\":{\"view\":{\"value\":\"body\"}},"
                            + "\"version\":{\"when\":\"2026-08-01T00:00:00.000Z\"}}}],\"_links\":{}}"));

            final ConfluenceDataStore dataStore = new ConfluenceDataStore() {
                @Override
                protected ExecutorService newFixedThreadPool(final int nThreads) {
                    return executor;
                }

                @Override
                protected void processContent(final DataConfig dataConfig, final IndexUpdateCallback callback,
                        final Map<String, Object> configMap, final DataStoreParams paramMap, final Map<String, String> scriptMap,
                        final Map<String, Object> defaultDataMap, final ConfluenceClient client, final Content content) {
                    awaitGate(gate, processed);
                }
            };

            dataStore.storeData(new DataConfig(), noopCallback(), params(server.getBaseUrl()), new HashMap<>(), new HashMap<>());
        }

        Assertions.assertEquals(1, processed.get(), "the drain must not interrupt a worker that is still waiting out a rate limit");
        Assertions.assertTrue(executor.isTerminated(), "the drain must return only once the pool has actually terminated");
    }
}
