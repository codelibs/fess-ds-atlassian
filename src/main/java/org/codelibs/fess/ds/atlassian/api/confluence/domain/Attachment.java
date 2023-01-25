/*
 * Copyright 2012-2023 CodeLibs Project and the Others.
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

@JsonIgnoreProperties(ignoreUnknown = true)
public class Attachment {

    protected String title;
    @JsonIgnore
    protected String mediaType;
    @JsonIgnore
    protected String downloadLink;

    public String getTitle() {
        return title;
    }

    public String getMediaType() {
        return mediaType;
    }

    public String getDownloadLink() {
        return downloadLink;
    }

    @JsonProperty("metadata")
    public void unpackMetadata(final Map<String, Object> metadata) {
        this.mediaType = (String) metadata.get("mediaType");
    }

    @JsonProperty("_links")
    public void unpackLinks(final Map<String, Object> links) {
        this.downloadLink = (String) links.get("download");
    }

}
