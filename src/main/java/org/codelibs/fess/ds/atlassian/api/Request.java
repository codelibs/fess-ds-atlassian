/*
 * Copyright 2012-2019 CodeLibs Project and the Others.
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

import java.io.IOException;
import java.util.Scanner;

import com.google.api.client.http.GenericUrl;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;

public abstract class Request {

    protected final HttpRequestFactory httpRequestFactory;
    protected final String appHome;

    public Request(final HttpRequestFactory httpRequestFactory, final String appHome) {
        this.httpRequestFactory = httpRequestFactory;
        this.appHome = appHome;
    }

    public HttpRequestFactory request() {
        return httpRequestFactory;
    }

    public String appHome() {
        return appHome;
    }

    public abstract GenericUrl buildUrl();

    public String getHttpResponseAsString() {
        final GenericUrl url = buildUrl();
        try {
            final HttpRequest request = request().buildGetRequest(url);
            final HttpResponse response = request.execute();
            if (response.getStatusCode() != 200) {
                throw new HttpResponseException(response);
            }
            final Scanner s = new Scanner(response.getContent());
            s.useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            s.close();
            return result;
        } catch (HttpResponseException e) {
            if (e.getStatusCode() == 404) {
                throw new AtlassianDataStoreException("The requested issue is not found, or the user does not have permission to view it.",
                        e);
            } else {
                throw new AtlassianDataStoreException("Content is not found: " + e.getStatusCode(), e);
            }
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to request: " + url, e);
        }
    }


}