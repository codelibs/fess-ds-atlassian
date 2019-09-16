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
package org.codelibs.fess.ds.atlassian.api.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.codelibs.core.lang.StringUtil;

public class UrlUtil {

    private UrlUtil() {
        // do nothing
    }

    public static String encode(final CharSequence element) {
        if (element == null) {
            return null;
        }
        return URLEncoder.encode(element.toString(), StandardCharsets.UTF_8);
    }

    public static String decode(final CharSequence element) {
        if (element == null) {
            return null;
        }
        return URLDecoder.decode(element.toString(), StandardCharsets.UTF_8);
    }

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

}
