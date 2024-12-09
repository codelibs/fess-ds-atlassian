/*
 * Copyright 2012-2024 CodeLibs Project and the Others.
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
package org.codelibs.fess.ds.atlassian.api.jira.domain;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class Fields {

    protected String summary;
    protected String updated;
    protected String description;
    protected Comments comment;

    public String getSummary() {
        return summary;
    }

    public String getUpdated() {
        return updated;
    }

    public String getDescription() {
        return description;
    }

    public Comments getComment() {
        return comment;
    }

    @Override
    public String toString() {
        return "Fields [summary=" + summary + ", updated=" + updated + ", description=" + description + "]";
    }

}
