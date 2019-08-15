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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

import com.google.api.client.http.apache.ApacheHttpTransport;

import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.crawler.exception.CrawlingAccessException;
import org.codelibs.fess.crawler.extractor.Extractor;
import org.codelibs.fess.crawler.extractor.impl.HtmlExtractor;
import org.codelibs.fess.ds.AbstractDataStore;
import org.codelibs.fess.ds.atlassian.api.AtlassianClient;
import org.codelibs.fess.ds.atlassian.api.AtlassianClientBuilder;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.callback.IndexUpdateCallback;
import org.codelibs.fess.es.config.exentity.DataConfig;
import org.codelibs.fess.mylasta.direction.FessConfig;
import org.codelibs.fess.util.ComponentUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfluenceDataStore extends AbstractDataStore {
    private static final Logger logger = LoggerFactory.getLogger(JiraDataStore.class);

    // parameters
    protected static final String HOME_PARAM = "home";

    protected static final String CONSUMER_KEY_PARAM = "oauth.consumer_key";
    protected static final String PRIVATE_KEY_PARAM = "oauth.private_key";
    protected static final String SECRET_PARAM = "oauth.secret";
    protected static final String ACCESS_TOKEN_PARAM = "oauth.access_token";

    protected static final String USERNAME_PARAM = "basicauth.username";
    protected static final String PASSWORD_PARAM = "basicauth.password";

    // scripts
    protected static final String CONTENT = "content";
    protected static final String CONTENT_TITLE = "title";
    protected static final String CONTENT_BODY = "body";
    protected static final String CONTENT_COMMENTS = "comments";
    protected static final String CONTENT_LAST_MODIFIED = "last_modified";
    protected static final String CONTENT_VIEW_URL = "view_url";

    protected static final int CONTENT_LIMIT = 25;

    protected Extractor extractor;

    protected String getName() {
        return this.getClass().getSimpleName();
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
            logger.warn("parameter \"" + HOME_PARAM + "\" is required");
            return;
        } else if (!userName.isEmpty() && !password.isEmpty()) {
            basic = true;
        } else if (consumerKey.isEmpty() || privateKey.isEmpty() || verifier.isEmpty() || temporaryToken.isEmpty()) {
            logger.warn("parameter \"" + USERNAME_PARAM + "\" and \"" + PASSWORD_PARAM + "\" or \"" + CONSUMER_KEY_PARAM + "\", \""
                    + PRIVATE_KEY_PARAM + "\", \"" + SECRET_PARAM + "\" and \"" + ACCESS_TOKEN_PARAM + "\" are required");
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

        extractor = new HtmlExtractor();

        for (int start = 0;; start += CONTENT_LIMIT) {
            // get contents
            final List<Map<String, Object>> contents =
                    client.getContents().start(start).limit(CONTENT_LIMIT).expand("space", "version", "body.view").execute().getContents();

            // store contents
            for (final Map<String, Object> content : contents) {
                processContent(dataConfig, callback, paramMap, scriptMap, defaultDataMap, fessConfig, client, readInterval, confluenceHome,
                        content);
            }

            if (contents.size() < CONTENT_LIMIT)
                break;
        }

        for (int start = 0;; start += CONTENT_LIMIT) {
            // get blog contents
            final List<Map<String, Object>> blogContents = client.getContents().start(start).limit(CONTENT_LIMIT).type("blogpost")
                    .expand("space", "version", "body.view").execute().getContents();

            // store blog contents
            for (final Map<String, Object> content : blogContents) {
                processContent(dataConfig, callback, paramMap, scriptMap, defaultDataMap, fessConfig, client, readInterval, confluenceHome,
                        content);
            }

            if (blogContents.size() < CONTENT_LIMIT)
                break;
        }

    }

    protected void processContent(final DataConfig dataConfig, final IndexUpdateCallback callback, final Map<String, String> paramMap,
            final Map<String, String> scriptMap, final Map<String, Object> defaultDataMap, final FessConfig fessConfig,
            final ConfluenceClient client, final long readInterval, final String confluenceHome, final Map<String, Object> content) {
        final Map<String, Object> dataMap = new HashMap<>();
        dataMap.putAll(defaultDataMap);
        final Map<String, Object> resultMap = new LinkedHashMap<>();
        resultMap.putAll(paramMap);
        final Map<String, Object> contentMap = new HashMap<>();

        try {
            contentMap.put(CONTENT_TITLE, getContentTitle(content));
            contentMap.put(CONTENT_BODY, getContentBody(content));
            contentMap.put(CONTENT_COMMENTS, getContentComments(content, client));
            contentMap.put(CONTENT_LAST_MODIFIED, getContentLastModified(content));
            contentMap.put(CONTENT_VIEW_URL, getContentViewUrl(content, confluenceHome));
            resultMap.put(CONTENT, contentMap);

            for (final Map.Entry<String, String> entry : scriptMap.entrySet()) {
                final Object convertValue = convertValue(entry.getValue(), resultMap);
                if (convertValue != null) {
                    dataMap.put(entry.getKey(), convertValue);
                }
            }
            callback.store(paramMap, dataMap);
        } catch (final CrawlingAccessException e) {
            logger.warn("Crawling Access Exception at : " + dataMap, e);
        }
    }

    protected String getContentTitle(final Map<String, Object> content) {
        return (String) content.getOrDefault("title", "");
    }

    @SuppressWarnings("unchecked")
    protected String getContentBody(final Map<String, Object> content) {
        final Map<String, Object> body = (Map<String, Object>) content.get("body");
        final Map<String, Object> view = (Map<String, Object>) body.get("view");
        final String value = (String) view.get("value");
        return getExtractedText(value);
    }

    @SuppressWarnings("unchecked")
    protected String getContentComments(final Map<String, Object> content, final ConfluenceClient client) {
        final StringBuilder sb = new StringBuilder();
        final String id = (String) content.get("id");

        for (int start = 0;; start += CONTENT_LIMIT) {
            final List<Map<String, Object>> comments =
                    client.getCommentsOfContent(id).start(start).limit(CONTENT_LIMIT).expand("body.view").execute().getComments();

            for (final Map<String, Object> comment : comments) {
                final Map<String, Object> body = (Map<String, Object>) comment.get("body");
                final Map<String, Object> view = (Map<String, Object>) body.get("view");
                final String value = (String) view.get("value");
                sb.append("\n\n");
                sb.append(getExtractedText(value));
            }

            if (comments.size() < CONTENT_LIMIT)
                break;
        }
        return sb.toString();
    }

    protected String getExtractedText(final String text) {
        final InputStream in = new ByteArrayInputStream(text.getBytes());
        return extractor.getText(in, null).getContent();
    }

    @SuppressWarnings("unchecked")
    protected Date getContentLastModified(final Map<String, Object> content) {
        final Map<String, Object> version = (Map<String, Object>) content.get("version");
        final String when = (String) version.get("when");
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            return format.parse(when);
        } catch (final ParseException e) {
            logger.warn("Fail to parse: " + when, e);
        }
        return null;
    }

    protected String getContentViewUrl(final Map<String, Object> content, final String confluenceHome) {
        final String id = (String) content.get("id");
        final String type = (String) content.get("type");
        @SuppressWarnings("unchecked")
        final Map<String, Object> space = (Map<String, Object>) content.get("space");
        final String spaceKey = (String) space.get("key");
        return confluenceHome + "/spaces/" + spaceKey + "/" + (type.equals("blogpost") ? "blog" : "page") + "/" + id;
    }

    protected String getConfluenceHome(Map<String, String> paramMap) {
        if (paramMap.containsKey(HOME_PARAM)) {
            return paramMap.get(HOME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getUserName(Map<String, String> paramMap) {
        if (paramMap.containsKey(USERNAME_PARAM)) {
            return paramMap.get(USERNAME_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPassword(Map<String, String> paramMap) {
        if (paramMap.containsKey(PASSWORD_PARAM)) {
            return paramMap.get(PASSWORD_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getConsumerKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(CONSUMER_KEY_PARAM)) {
            return paramMap.get(CONSUMER_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getPrivateKey(Map<String, String> paramMap) {
        if (paramMap.containsKey(PRIVATE_KEY_PARAM)) {
            return paramMap.get(PRIVATE_KEY_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getSecret(Map<String, String> paramMap) {
        if (paramMap.containsKey(SECRET_PARAM)) {
            return paramMap.get(SECRET_PARAM);
        }
        return StringUtil.EMPTY;
    }

    protected String getAccessToken(Map<String, String> paramMap) {
        if (paramMap.containsKey(ACCESS_TOKEN_PARAM)) {
            return paramMap.get(ACCESS_TOKEN_PARAM);
        }
        return StringUtil.EMPTY;
    }

}
