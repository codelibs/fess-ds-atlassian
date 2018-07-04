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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceClient;
import org.codelibs.fess.ds.atlassian.api.confluence.ConfluenceRequest;

public class GetSpaceRequest extends ConfluenceRequest {

    private String spaceKey;
    private String[] expand;

    public GetSpaceRequest(ConfluenceClient confluenceClient, String spaceKey) {
        super(confluenceClient);
        this.spaceKey = spaceKey;
    }

    @Override
    public GetSpaceResponse execute() {
        return new GetSpaceResponse();
    }

    public GetSpaceRequest expand(String... expand) {
        this.expand = expand;
        return this;
    }
}