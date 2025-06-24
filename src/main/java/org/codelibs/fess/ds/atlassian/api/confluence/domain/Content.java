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
package org.codelibs.fess.ds.atlassian.api.confluence.domain;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Map;
import java.util.TimeZone;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    protected String id;
    protected String type;
    protected String title;
    protected Space space;

    @JsonIgnore
    protected String body;
    @JsonIgnore
    protected Long lastModified;

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public Space getSpace() {
        return space;
    }

    public String getBody() {
        return body;
    }

    public Long getLastModified() {
        return lastModified;
    }

    @JsonProperty("body")
    public void unpackBody(final Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> view = (Map<String, Object>) body.get("view");
        final String value = (String) view.get("value");
        this.body = value;
    }

    @JsonProperty("version")
    public void unpackVersion(final Map<String, Object> version) throws ParseException {
        final String when = (String) version.get("when");
        try {
            final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSX");
            format.setTimeZone(TimeZone.getTimeZone("UTC"));
            this.lastModified = format.parse(when).getTime();
        } catch (final ParseException e) {
            throw e; // TODO
        }
    }

}
