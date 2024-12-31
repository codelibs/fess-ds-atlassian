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
package org.codelibs.fess.ds.atlassian.api;

import org.codelibs.fess.util.ComponentUtil;
import org.dbflute.utflute.lastaflute.LastaFluteTestCase;

public class AtlassianClientTest extends LastaFluteTestCase {

    protected static final String AUTH_TYPE_PARAM = "auth_type";
    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";

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
    }

    @Override
    public void tearDown() throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown();
    }

    public void test_production() {
        // doProductionTest();
    }

    protected void doProductionTest() {
    }
}
