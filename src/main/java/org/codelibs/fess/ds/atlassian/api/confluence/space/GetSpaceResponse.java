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
package org.codelibs.fess.ds.atlassian.api.confluence.space;

import org.codelibs.fess.ds.atlassian.api.confluence.domain.Space;

/**
 * Response containing a Confluence space.
 */
public class GetSpaceResponse {

    /** The space returned by the API. */
    protected Space space;

    /**
     * Constructs a response with the given space.
     *
     * @param space the space
     */
    public GetSpaceResponse(final Space space) {
        this.space = space;
    }

    /**
     * Returns the space.
     *
     * @return the space
     */
    public Space getSpace() {
        return space;
    }

}
