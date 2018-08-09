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
package org.codelibs.fess.ds.atlassian.api;

import com.google.api.client.http.HttpRequestFactory;

public class AtlassianClient {

    protected final String appHome;
    protected final HttpRequestFactory httpRequestFactory;

    public AtlassianClient(final String appHome, final HttpRequestFactory httpRequestFactory) {
        this.appHome = appHome;
        this.httpRequestFactory = httpRequestFactory;
    }

    public static AtlassianClientBuilder builder() {
        return new AtlassianClientBuilder();
    }

    public String appHome() {
        return appHome;
    }

    public HttpRequestFactory request() {
        return httpRequestFactory;
    }

}