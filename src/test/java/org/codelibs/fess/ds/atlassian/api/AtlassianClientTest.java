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

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.TestInfo;

import org.codelibs.fess.util.ComponentUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.MockAtlassianServer;
import org.codelibs.fess.ds.atlassian.UnitDsTestCase;
import org.codelibs.fess.ds.atlassian.api.authentication.OAuth2Authentication;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.entity.DataStoreParams;
import org.codelibs.fess.opensearch.config.exbhv.DataConfigBhv;
import org.codelibs.fess.opensearch.config.exentity.DataConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.lastaflute.core.security.PrimaryCipher;

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

    // Security: oauth2.access_token and oauth2.refresh_token both match the default
    // app.encrypt.property.pattern (.*password|.*key|.*token|.*secret), so a data config saved
    // through the admin UI holds them as {cipher}... The write-back calls DataConfigBhv.update
    // directly, bypassing the ParameterUtil.encrypt that DataConfigService.store applies, so it
    // must re-encrypt the lines it replaces rather than overwrite them in plain text.
    @Test
    public void test_updateTokenParameters_keepsAnEncryptedTokenLineEncrypted() {
        final PrimaryCipher cipher = ComponentUtil.getPrimaryCipher();
        final String handlerParameter = "oauth2.access_token={cipher}" + cipher.encrypt("old-access") + "\n"
                + "oauth2.refresh_token={cipher}" + cipher.encrypt("old-refresh") + "\n" + "oauth2.client_id=client-id";

        final OAuth2Authentication.TokenUpdateResult tokenUpdateResult =
                new OAuth2Authentication.TokenUpdateResult("new-access", "new-refresh");

        final String updated =
                AtlassianClient.updateTokenParameters(handlerParameter, OAuth2Authentication.GRANT_AUTHORIZATION_CODE, tokenUpdateResult);

        final String[] lines = updated.split("\n", -1);
        Assertions.assertEquals(3, lines.length);
        Assertions.assertTrue(lines[0].startsWith("oauth2.access_token={cipher}"),
                "an encrypted access-token line must come back encrypted, not in plain text: " + lines[0]);
        Assertions.assertTrue(lines[1].startsWith("oauth2.refresh_token={cipher}"),
                "an encrypted refresh-token line must come back encrypted, not in plain text: " + lines[1]);
        Assertions.assertEquals("new-access", cipher.decrypt(lines[0].substring("oauth2.access_token={cipher}".length())));
        Assertions.assertEquals("new-refresh", cipher.decrypt(lines[1].substring("oauth2.refresh_token={cipher}".length())));
        Assertions.assertEquals("oauth2.client_id=client-id", lines[2]);
    }

    // Under client_credentials the write-back is skipped entirely: the token is derivable from
    // client_id/client_secret and lives 3600 seconds, so persisting it buys nothing. Without the
    // guard, the natural client_credentials setup -- which has no oauth2.access_token line,
    // because that grant does not need one -- would gain a new plaintext bearer-token line.
    @Test
    public void test_clientCredentials_refresh_appendsNoAccessTokenLine() throws Exception {
        final String handlerParameter = "oauth2.grant_type=client_credentials\n" + "oauth2.client_id=client-id\n"
                + "oauth2.client_secret={cipher}XYZ==\n" + "home=https://confluence.example.com";

        // Stub the behavior so the write-back would succeed if it were attempted: without this the
        // guard would be "proven" only by a ComponentNotFoundException, which says nothing about
        // what the write-back would have written.
        final AtomicInteger updates = new AtomicInteger();
        ComponentUtil.register(new DataConfigBhv() {
            @Override
            public void update(final DataConfig entity) {
                updates.incrementAndGet();
            }
        }, DataConfigBhv.class.getCanonicalName());

        try (MockAtlassianServer server = new MockAtlassianServer().start()) {
            server.on("/oauth/token", req -> MockAtlassianServer.json("{\"access_token\":\"fresh-token\",\"expires_in\":3600}"));

            final DataConfig dataConfig = new DataConfig();
            dataConfig.setHandlerParameter(handlerParameter);

            final DataStoreParams paramMap = new DataStoreParams();
            paramMap.put("home", "https://confluence.example.com");
            paramMap.put("deployment", "datacenter");
            paramMap.put(AUTH_TYPE_PARAM, "oauth2");
            paramMap.put("oauth2.grant_type", OAuth2Authentication.GRANT_CLIENT_CREDENTIALS);
            paramMap.put("oauth2.client_id", "client-id");
            paramMap.put("oauth2.client_secret", "client-secret");
            paramMap.put("oauth2.token_url", server.getBaseUrl() + "/oauth/token");

            try (JiraClient client = new JiraClient(dataConfig, paramMap)) {
                ((OAuth2Authentication) client.authentication).refreshAccessToken();
            }

            Assertions.assertEquals(handlerParameter, dataConfig.getHandlerParameter());
            Assertions.assertEquals(0, updates.get(), "client_credentials must not cost an OpenSearch write per refresh");
        }
    }

}
