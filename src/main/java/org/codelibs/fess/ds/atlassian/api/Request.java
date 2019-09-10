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
import com.google.api.client.http.HttpContent;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestFactory;
import com.google.api.client.http.HttpResponse;
import com.google.api.client.http.HttpResponseException;
import com.google.api.client.http.json.JsonHttpContent;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.GenericData;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class Request {

    protected final static String GET_REQUEST = "GET";
    protected final static String PUT_REQUEST = "PUT";
    protected final static String POST_REQUEST = "POST";
    protected final static String DELETE_REQUEST = "DELETE";

    private static final Logger logger = LoggerFactory.getLogger(Request.class);

    protected final HttpRequestFactory httpRequestFactory;
    protected final String appHome;

    public Request(final HttpRequestFactory httpRequestFactory, final String appHome) {
        this.httpRequestFactory = httpRequestFactory;
        this.appHome = appHome;
    }

    public String appHome() {
        return appHome;
    }

    public abstract GenericUrl buildUrl();

    public GenericData buildData() {
        return null;
    }

    public String getHttpResponseAsString(final String requestMethod) {
        final GenericUrl url = buildUrl();
        try {
            synchronized (httpRequestFactory) {
                HttpRequest request;
                switch (requestMethod) {
                    case GET_REQUEST: {
                        request = httpRequestFactory.buildGetRequest(url);
                        break;
                    }
                    case PUT_REQUEST: {
                        final HttpContent content = new JsonHttpContent(new JacksonFactory(), buildData());
                        request = httpRequestFactory.buildPutRequest(url, content);
                        break;
                    }
                    case POST_REQUEST: {
                        final HttpContent content = new JsonHttpContent(new JacksonFactory(), buildData());
                        request = httpRequestFactory.buildPostRequest(url, content);
                        break;
                    }
                    case DELETE_REQUEST: {
                        request = httpRequestFactory.buildDeleteRequest(url);
                        break;
                    }
                    default: {
                        throw new AtlassianDataStoreException("unknown request method : " + requestMethod);
                    }
                }
                final HttpResponse response = request.execute();
                if (response.getStatusCode() != 200) {
                    throw new HttpResponseException(response);
                }
                return getContentAsString(response);
            }
        } catch (final HttpResponseException e) {
            throw new AtlassianDataStoreException("HTTP Status :" + e.getStatusCode(), e);
        }  catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to request : " + url, e);
        }
    }

    protected String getContentAsString(final HttpResponse response) {
        try (final Scanner s = new Scanner(response.getContent())) {
            s.useDelimiter("\\A");
            final String result = s.hasNext() ? s.next() : "";
            return result;
        } catch (final IOException e) {
            throw new AtlassianDataStoreException("Failed to get response content.", e);
        }
    }

}