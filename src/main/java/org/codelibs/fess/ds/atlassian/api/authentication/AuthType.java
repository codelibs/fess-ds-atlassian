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
package org.codelibs.fess.ds.atlassian.api.authentication;

/**
 * Represents the authentication types for Atlassian API.
 */
public enum AuthType {
    /** Basic authentication. */
    BASIC("basic"),
    /** OAuth authentication. */
    OAUTH("oauth"),
    /** OAuth2 authentication. */
    OAUTH2("oauth2");

    private final String authType;

    AuthType(final String authType) {
        this.authType = authType;
    }

    /**
     * Returns the authentication type string.
     *
     * @return the authentication type string
     */
    public String getAuthType() {
        return authType;
    }
}
