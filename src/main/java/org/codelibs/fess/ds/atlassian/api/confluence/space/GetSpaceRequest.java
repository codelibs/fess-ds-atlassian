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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;
import org.codelibs.fess.ds.atlassian.api.Request;
import org.codelibs.fess.ds.atlassian.api.authentication.Authentication;
import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

public class GetSpaceRequest extends Request {

    private final String spaceKey;
    private String[] expand;

    public GetSpaceRequest(final Authentication authentication, final String appHome, final String spaceKey) {
        super(authentication, appHome);
        this.spaceKey = spaceKey;
    }

    public GetSpaceRequest expand(final String... expand) {
        this.expand = expand;
        return this;
    }

    public GetSpaceResponse execute() {
        return parseResponse(getCurlResponse(GET).getContentAsString());
    }

    public static GetSpaceResponse parseResponse(final String json) {
        try {
            return new GetSpaceResponse(mapper.readValue(json, Space.class));
        } catch (IOException e) {
            throw new AtlassianDataStoreException("Failed to parse space from: " + json, e);
        }
    }

    @Override
    public String getURL() {
        return appHome + "/rest/api/latest/space/" + spaceKey;
    }

    @Override
    public Map<String, String> getQueryParamMap() {
        final Map<String, String> queryParams = new HashMap<>();
        if (expand != null) {
            queryParams.put("expand", String.join(",", expand));
        }
        return queryParams;
    }

}
