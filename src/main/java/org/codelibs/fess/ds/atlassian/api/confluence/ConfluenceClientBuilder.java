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
package org.codelibs.fess.ds.atlassian.api.confluence;

import com.google.api.client.auth.oauth.OAuthGetAccessToken;
import com.google.api.client.http.BasicAuthentication;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.javanet.NetHttpTransport;

public class ConfluenceClientBuilder {
    private String confluenceHome;
    private OAuthGetAccessToken oAuthGetAccessToken;
    private BasicAuthentication basicAuthentication;

    ConfluenceClientBuilder() {
    }

    public ConfluenceClientBuilder basicAuth(final String confluenceHome, final String userName,
            final String password) {
        this.confluenceHome = confluenceHome;
        basicAuthentication = new BasicAuthentication(userName, password);
        return this;
    }

    public ConfluenceClient build() {
        final HttpRequestFactory httpRequestFactory;
        if (basicAuthentication != null) {
            httpRequestFactory = new NetHttpTransport().createRequestFactory(basicAuthentication);
        } else if (oAuthGetAccessToken != null) {
            httpRequestFactory = new NetHttpTransport().createRequestFactory(oAuthGetAccessToken.createParameters());
        } else {
            httpRequestFactory = new NetHttpTransport().createRequestFactory();
        }
        return new ConfluenceClient(confluenceHome, httpRequestFactory);
    }

}
