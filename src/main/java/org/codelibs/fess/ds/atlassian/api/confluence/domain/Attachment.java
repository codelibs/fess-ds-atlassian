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

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Represents a file attachment in Confluence content.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {

    /** The title of the attachment. */
    protected String title;

    /** The media type of the attachment. */
    @JsonIgnore
    protected String mediaType;

    /** The download link for the attachment. */
    @JsonIgnore
    protected String downloadLink;

    /**
     * Default constructor for Attachment.
     */
    public Attachment() {
        // Default constructor
    }

    /**
     * Gets the attachment title.
     *
     * @return the attachment title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Gets the media type of the attachment.
     *
     * @return the media type
     */
    public String getMediaType() {
        return mediaType;
    }

    /**
     * Gets the download link for the attachment.
     *
     * @return the download link
     */
    public String getDownloadLink() {
        return downloadLink;
    }

    /**
     * Unpacks metadata from the API response.
     *
     * @param metadata the metadata from API response
     */
    @JsonProperty("metadata")
    public void unpackMetadata(final Map<String, Object> metadata) {
        this.mediaType = (String) metadata.get("mediaType");
    }

    /**
     * Unpacks links from the API response.
     *
     * @param links the links from API response
     */
    @JsonProperty("_links")
    public void unpackLinks(final Map<String, Object> links) {
        this.downloadLink = (String) links.get("download");
    }

}
