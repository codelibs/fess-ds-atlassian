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
package org.codelibs.fess.ds.atlassian.api.util;

import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;

/**
 * Utility class for URL encoding, decoding, and query parameter manipulation.
 */
public class UrlUtil {
    private static final Logger logger = LogManager.getLogger(UrlUtil.class);

    /**
     * Private constructor to prevent instantiation.
     */
    private UrlUtil() {
        // do nothing
    }

    /**
     * URL-encodes the given character sequence using UTF-8 encoding.
     *
     * @param element the character sequence to encode
     * @return the URL-encoded string, or null if input is null
     */
    public static String encode(final CharSequence element) {
        if (element == null) {
            return null;
        }
        return URLEncoder.encode(element.toString(), StandardCharsets.UTF_8);
    }

    /**
     * URL-decodes the given character sequence using UTF-8 encoding.
     *
     * @param element the character sequence to decode
     * @return the URL-decoded string, or null if input is null
     */
    public static String decode(final CharSequence element) {
        if (element == null) {
            return null;
        }
        return URLDecoder.decode(element.toString(), StandardCharsets.UTF_8);
    }

    /**
     * Builds a query parameter string from a map of parameters.
     *
     * @param params the parameter map (key-value pairs)
     * @return the query parameter string (without leading '?'), or empty string if params is null
     */
    public static String buildQueryParameters(final Map<String, String> params) {
        if (params == null) {
            return StringUtil.EMPTY;
        }

        final StringBuilder parametersBuf = new StringBuilder();
        boolean first = true;
        for (final Map.Entry<String, String> entry : params.entrySet()) {
            if (first) {
                first = false;
            } else {
                parametersBuf.append('&');
            }
            parametersBuf.append(entry.getKey());
            final String value = encode(entry.getValue());
            if (value != null) {
                parametersBuf.append('=').append(value);
            }
        }
        return parametersBuf.toString();
    }

    /**
     * Normalizes the given URL by removing trailing slashes.
     *
     * @param urlString the URL string to normalize
     * @return the normalized URL string
     */
    public static String normalizeUrl(final String urlString) {
        try {
            final URI uri = URI.create(urlString).normalize();
            final String normalized = uri.toString();
            return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
        } catch (IllegalArgumentException e) {
            logger.warn("Failed to normalize URL.", e);
            return urlString.endsWith("/") ? urlString.substring(0, urlString.length() - 1) : urlString;
        }
    }
}
