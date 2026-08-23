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
package org.codelibs.fess.ds.atlassian.api;

import org.junit.jupiter.api.TestInfo;

import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuth2Authentication;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AtlassianClientTest extends UnitDsTestCase {

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
    public void setUp(TestInfo testInfo) throws Exception {
        super.setUp(testInfo);
    }

    @Override
    public void tearDown(TestInfo testInfo) throws Exception {
        ComponentUtil.setFessConfig(null);
        super.tearDown(testInfo);
    }

    // Step 4 regression guard: the default (authorization_code) grant must still require
    // oauth2.access_token, exactly as before this feature was added.
    @Test
    public void test_oauth2_authorizationCode_missingAccessToken_throws() {
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("home", "https://example.com");
        paramMap.put(AUTH_TYPE_PARAM, "oauth2");
        paramMap.put("oauth2.client_id", "client-id");
        paramMap.put("oauth2.client_secret", "client-secret");

        Assertions.assertThrows(AtlassianDataStoreException.class, () -> new JiraClient(new DataConfig(), paramMap));
    }

    // Step 4: the client_credentials grant must not require oauth2.access_token / oauth2.refresh_token,
    // only oauth2.client_id / oauth2.client_secret.
    @Test
    public void test_oauth2_clientCredentials_withoutAccessToken_doesNotThrow() {
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("home", "https://example.com");
        paramMap.put(AUTH_TYPE_PARAM, "oauth2");
        paramMap.put("oauth2.grant_type", OAuth2Authentication.GRANT_CLIENT_CREDENTIALS);
        paramMap.put("oauth2.client_id", "client-id");
        paramMap.put("oauth2.client_secret", "client-secret");

        Assertions.assertDoesNotThrow(() -> new JiraClient(new DataConfig(), paramMap));
    }

    // Step 4: the client_credentials grant still requires client_id / client_secret.
    @Test
    public void test_oauth2_clientCredentials_missingClientSecret_throws() {
        final DataStoreParams paramMap = new DataStoreParams();
        paramMap.put("home", "https://example.com");
        paramMap.put(AUTH_TYPE_PARAM, "oauth2");
        paramMap.put("oauth2.grant_type", OAuth2Authentication.GRANT_CLIENT_CREDENTIALS);
        paramMap.put("oauth2.client_id", "client-id");

        Assertions.assertThrows(AtlassianDataStoreException.class, () -> new JiraClient(new DataConfig(), paramMap));
    }

    // Step 5, security: the write-back must edit the RAW handlerParameter string so an encrypted
    // value (oauth2.client_secret={cipher}...) is preserved verbatim, never rewritten in plain text.
    @Test
    public void test_updateTokenParameters_preservesEncryptedValueVerbatim() {
        final String handlerParameter = "oauth2.access_token=old-access\n" + "oauth2.refresh_token=old-refresh\n"
                + "oauth2.client_id=client-id\n" + "oauth2.client_secret={cipher}ABCDEF==\n" + "home=https://example.atlassian.net";

        final OAuth2Authentication.TokenUpdateResult tokenUpdateResult =
                new OAuth2Authentication.TokenUpdateResult("new-access", "new-refresh");

        final String updated =
                AtlassianClient.updateTokenParameters(handlerParameter, OAuth2Authentication.GRANT_AUTHORIZATION_CODE, tokenUpdateResult);

        Assertions.assertEquals("oauth2.access_token=new-access\n" + "oauth2.refresh_token=new-refresh\n" + "oauth2.client_id=client-id\n"
                + "oauth2.client_secret={cipher}ABCDEF==\n" + "home=https://example.atlassian.net", updated);
    }

    // Step 5: the client_credentials grant has no refresh token, so only the access-token line is
    // updated; no oauth2.refresh_token line is added.
    @Test
    public void test_updateTokenParameters_clientCredentials_updatesOnlyAccessToken() {
        final String handlerParameter =
                "oauth2.access_token=old-access\n" + "oauth2.client_id=client-id\n" + "oauth2.client_secret={cipher}XYZ==";

        final OAuth2Authentication.TokenUpdateResult tokenUpdateResult = new OAuth2Authentication.TokenUpdateResult("fresh-token", null);

        final String updated =
                AtlassianClient.updateTokenParameters(handlerParameter, OAuth2Authentication.GRANT_CLIENT_CREDENTIALS, tokenUpdateResult);

        Assertions.assertEquals("oauth2.access_token=fresh-token\n" + "oauth2.client_id=client-id\n" + "oauth2.client_secret={cipher}XYZ==",
                updated);
    }

    // Step 5: comments, blank lines, and line ordering must survive byte-identical.
    @Test
    public void test_updateTokenParameters_preservesBlankLinesAndComments() {
        final String handlerParameter =
                "# oauth2 config\n" + "\n" + "oauth2.access_token=old-access\n" + "\n" + "oauth2.client_id=client-id";

        final OAuth2Authentication.TokenUpdateResult tokenUpdateResult = new OAuth2Authentication.TokenUpdateResult("fresh-token", null);

        final String updated =
                AtlassianClient.updateTokenParameters(handlerParameter, OAuth2Authentication.GRANT_AUTHORIZATION_CODE, tokenUpdateResult);

        Assertions.assertEquals("# oauth2 config\n" + "\n" + "oauth2.access_token=fresh-token\n" + "\n" + "oauth2.client_id=client-id",
                updated);
    }

    // Regression (review finding on Task 10): under client_credentials, a stale oauth2.refresh_token
    // line -- left over from a config migrated off authorization_code -- must never be touched, even
    // if TokenUpdateResult happens to still carry a non-blank (stale) refresh token value. The
    // client_credentials response never returns a refresh_token, so OAuth2Authentication.refreshToken
    // carries the original constructed value forward unchanged; gating only on isNotBlank would rewrite
    // an encrypted line in plain text on every refresh cycle. The guard must be on grant type, not on
    // whether the token value happens to be blank.
    @Test
    public void test_updateTokenParameters_clientCredentials_neverTouchesStaleRefreshTokenLine() {
        final String handlerParameter = "oauth2.access_token=old-access\n" + "oauth2.refresh_token={cipher}SOMETHING==\n"
                + "oauth2.client_id=client-id\n" + "oauth2.client_secret={cipher}XYZ==";

        // Simulates the stale case: OAuth2Authentication still carries the old decrypted refresh
        // token forward because a client_credentials response never overwrites it.
        final OAuth2Authentication.TokenUpdateResult tokenUpdateResult =
                new OAuth2Authentication.TokenUpdateResult("fresh-token", "stale-decrypted-refresh-token");

        final String updated =
                AtlassianClient.updateTokenParameters(handlerParameter, OAuth2Authentication.GRANT_CLIENT_CREDENTIALS, tokenUpdateResult);

        Assertions.assertEquals("oauth2.access_token=fresh-token\n" + "oauth2.refresh_token={cipher}SOMETHING==\n"
                + "oauth2.client_id=client-id\n" + "oauth2.client_secret={cipher}XYZ==", updated);
    }

}
