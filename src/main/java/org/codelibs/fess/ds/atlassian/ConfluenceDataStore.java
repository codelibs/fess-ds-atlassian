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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.jira.JiraClient;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceDataStore extends AbstractDataStore {
    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    protected static final String CONFLUENCE_HOME_PARAM = "confluence.home";

    protected static final String CONFLUENCE_CONSUMER_KEY_PARAM = "confluence.oauth.consumer_key";
    protected static final String CONFLUENCE_PRIVATE_KEY_PARAM = "confluence.oauth.private_key";
    protected static final String CONFLUENCE_SECRET_PARAM = "confluence.oauth.secret";
    protected static final String CONFLUENCE_ACCESS_TOKEN_PARAM = "confluence.oauth.access_token";

    protected static final String CONFLUENCE_USERNAME_PARAM = "confluence.basicauth.username";
    protected static final String CONFLUENCE_PASSWORD_PARAM = "confluence.basicauth.password";

    protected static final int LIMIT = 25;

    protected String getName() {
        return "ConfluenceDataStore";
    }

    @Override
    protected void storeData(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();

        final String confluenceHome = getConfluenceHome(paramMap);

        final String userName = getUserName(paramMap);
        final String password = getPassword(paramMap);

        final String consumerKey = getConsumerKey(paramMap);
        final String privateKey = getPrivateKey(paramMap);
        final String verifier = getSecret(paramMap);
        final String temporaryToken = getAccessToken(paramMap);

        final long readInterval = getReadInterval(paramMap);

        boolean basic = false;
        if (confluenceHome.isEmpty()) {
            logger.warn("parameter \"" + CONFLUENCE_HOME_PARAM + "\" is required");
            return;
        } else if (!userName.isEmpty() && !password.isEmpty()) {
            basic = true;
        } else if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || temporaryToken.isEmpty()) {
            logger.warn("parameter \"" + CONFLUENCE_USERNAME_PARAM + "\" and \"" + CONFLUENCE_PASSWORD_PARAM + "\" or \""
                    + CONFLUENCE_CONSUMER_KEY_PARAM + "\", \"" + CONFLUENCE_PRIVATE_KEY_PARAM + "\", \"" + CONFLUENCE_SECRET_PARAM
                    + "\" and \"" + CONFLUENCE_ACCESS_TOKEN_PARAM + "\" are required");
            return;
        }

        final ConfluenceClient client =
                basic ? new ConfluenceClient(AtlassianClient.builder().basicAuth(confluenceHome, userName, password).build())
                        : new ConfluenceClient(AtlassianClient.builder().oAuthToken(confluenceHome, accessToken -> {
                            accessToken.consumerKey = consumerKey;
                            accessToken.signer = AtlassianClientBuilder.getOAuthRsaSigner(privateKey);
                            accessToken.transport = new ApacheHttpTransport();
                            accessToken.verifier = verifier;
                            accessToken.temporaryToken = temporaryToken;
                        }).build());

        for (int start = 0;; start += LIMIT) {
            // get contents
            List<Map<String, Object>> contents =
                    client.getContents().start(start).limit(LIMIT).expand("space", "version", "body.view").execute().getContents();

            if (contents.size() == 0)
                break;

            // store contents
            for (Map<String, Object> content : contents) {
                processContent(dataConfig, callback, paramMap, scriptMap, defaultDataMap, readInterval, confluenceHome, content);
            }
        }

        for (int start = 0;; start += LIMIT) {
            // get blog contents
            List<Map<String, Object>> blogContents = client.getContents().start(start).limit(LIMIT).type("blogpost")
                    .expand("space", "version", "body.view").execute().getContents();

            if (blogContents.size() == 0)
                break;

            // store blog contents
            for (Map<String, Object> content : blogContents) {
                processContent(dataConfig, callback, paramMap, scriptMap, defaultDataMap, readInterval, confluenceHome, content);
            }
        }

    }

    @SuppressWarnings("unchecked")
    protected void processContent(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap, final long readInterval,
            final String confluenceHome, final Map<String, Object> content) {
        final FessConfig fessConfig = ComponentUtil.getFessConfig();
        final Map<String, Object> dataMap = new HashMap<>();
        dataMap.putAll(defaultDataMap);

        try {
            final String id = (String) content.get("id");
            final String type = (String) content.get("type");

            final Map<String, Object> spaceObj = (Map<String, Object>) content.get("space");
            final String spaceKey = (String) spaceObj.get("key");

            dataMap.put(fessConfig.getIndexFieldUrl(),
                    confluenceHome + "/spaces/" + spaceKey + "/" + (type == "blogspot" ? "blog" : "page") + "/" + id);
            dataMap.put(fessConfig.getIndexFieldTitle(), content.getOrDefault("title", ""));

            final Map<String, Object> bodyObj = (Map<String, Object>) content.get("body");
            String body = (String) ((Map<String, Object>) bodyObj.get("view")).get("value");
            dataMap.put(fessConfig.getIndexFieldContent(), body);

            Map<String, Object> version = (Map<String, Object>) content.get("version");
            try {
                SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
                format.setTimeZone(TimeZone.getTimeZone("UTC"));
                Date lastModified = format.parse((String) version.get("when"));
                dataMap.put(fessConfig.getIndexFieldLastModified(), lastModified);
            } catch (final ParseException e) {
                logger.warn("Parse Exception", e);
            }
            callback.store(paramMap, dataMap);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : " + dataMap, e);
        }
    }

    protected String getConfluenceHome(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_HOME_PARAM)) {
            return paramMap.get(CONFLUENCE_HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getUserName(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_USERNAME_PARAM)) {
            return paramMap.get(CONFLUENCE_USERNAME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPassword(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_PASSWORD_PARAM)) {
            return paramMap.get(CONFLUENCE_PASSWORD_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getConsumerKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_CONSUMER_KEY_PARAM)) {
            return paramMap.get(CONFLUENCE_CONSUMER_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPrivateKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_PRIVATE_KEY_PARAM)) {
            return paramMap.get(CONFLUENCE_PRIVATE_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getSecret(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_SECRET_PARAM)) {
            return paramMap.get(CONFLUENCE_SECRET_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAccessToken(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONFLUENCE_ACCESS_TOKEN_PARAM)) {
            return paramMap.get(CONFLUENCE_ACCESS_TOKEN_PARAM);
        }
        return StringUtil.EMPTY;
    }

}
