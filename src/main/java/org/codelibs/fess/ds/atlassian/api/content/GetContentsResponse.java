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
package org.codelibs.fess.ds.atlassian.api.content;

import java.util.List;
import java.util.Map;

import org.codelibs.fess.ds.atlassian.api.Response;

public class GetContentsResponse extends Response {

    protected final List<Map<String, Object>> contents;

    public GetContentsResponse(List<Map<String, Object>> contents) {
        this.contents = contents;
    }

    public List<Map<String, Object>> getContents() {
        return contents;
    }

}