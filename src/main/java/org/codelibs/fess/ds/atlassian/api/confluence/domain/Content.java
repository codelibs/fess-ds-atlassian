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

/**
 * Represents content in Confluence (e.g., pages, blog posts).
 * Contains metadata and body content with version information.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Content {

    /** The unique identifier of the content. */
    protected String id;

    /** The type of content (e.g., page, blogpost). */
    protected String type;

    /** The title of the content. */
    protected String title;

    /** The space containing this content. */
    protected Space space;

    /** The body content in HTML format. */
    @JsonIgnore
    protected String body;

    /** The last modified timestamp. */
    @JsonIgnore
    protected Long lastModified;

    /**
     * Default constructor.
     */
    public Content() {
    }

    /**
     * Gets the content ID.
     *
     * @return the content ID
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the content type.
     *
     * @return the content type
     */
    public String getType() {
        return type;
    }

    /**
     * Gets the content title.
     *
     * @return the content title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the space containing this content.
     *
     * @return the space
     */
    public Space getSpace() {
        return space;
    }

    /**
     * Gets the content body.
     *
     * @return the content body in HTML format
     */
    public String getBody() {
        return body;
    }

    /**
     * Gets the last modified timestamp.
     *
     * @return the last modified timestamp in milliseconds
     */
    public Long getLastModified() {
        return lastModified;
    }

    /**
     * Unpacks the body content from the API response.
     *
     * @param body the body data from API response
     */
    @JsonProperty("body")
    public void unpackBody(final Map<String, Object> body) {
        @SuppressWarnings("unchecked")
        final Map<String, Object> view = (Map<String, Object>) body.get("view");
        final String value = (String) view.get("value");
        this.body = value;
    }

    /**
     * Unpacks version information from the API response.
     *
     * @param version the version data from API response
     * @throws ParseException if the date format cannot be parsed
     */
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
