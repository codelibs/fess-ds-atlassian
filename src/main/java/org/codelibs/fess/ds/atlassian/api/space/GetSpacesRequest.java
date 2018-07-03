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
package org.codelibs.fess.ds.atlassian.api.space;

import org.codelibs.fess.ds.atlassian.api.Request;

public class GetSpacesRequest extends Request {

    private String spaceKey, type, status, label, favourite;
    private String[] expand;
    private int start, limit;

    @Override
    public GetSpacesResponse execute() {
        return new GetSpacesResponse();
    }

    public GetSpacesRequest spaceKey(String spaceKey) {
        this.spaceKey = spaceKey;
        return this;
    }

    public GetSpacesRequest type(String type) {
        this.type = type;
        return this;
    }

    public GetSpacesRequest status(String status) {
        this.status = status;
        return this;
    }

    public GetSpacesRequest label(String label) {
        this.label = label;
        return this;
    }

    public GetSpacesRequest favourite(String favourite) {
        this.favourite = favourite;
        return this;
    }

    public GetSpacesRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }

    public GetSpacesRequest start(int start) {
        this.start = start;
        return this;
    }

    public GetSpacesRequest limit(int limit) {
        this.limit = limit;
        return this;
    }

}