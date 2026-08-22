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
package org.codelibs.fess.ds.atlassian.api;

import java.net.URI;
import java.util.Locale;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codelibs.core.lang.StringUtil;
import org.codelibs.fess.ds.atlassian.AtlassianDataStoreException;

/**
 * Represents the Atlassian deployment type.
 */
public enum Deployment {
    /** Atlassian Cloud. */
    CLOUD,
    /** Atlassian Data Center or the end-of-life Server edition. */
    DATA_CENTER;

    private static final Logger logger = LogManager.getLogger(Deployment.class);

    private static final String CLOUD_HOST_SUFFIX = ".atlassian.net";

    /**
     * Detects the deployment type from the home URL.
     * A host ending with {@code .atlassian.net} is treated as Cloud; anything else is Data Center.
     *
     * @param home the instance home URL, may be null or blank
     * @return the detected deployment type, never null
     */
    public static Deployment detect(final String home) {
        if (StringUtil.isBlank(home)) {
            return DATA_CENTER;
        }
        try {
            final String host = new URI(home.trim()).getHost();
            if (host != null && host.toLowerCase(Locale.ROOT).endsWith(CLOUD_HOST_SUFFIX)) {
                return CLOUD;
            }
        } catch (final Exception e) {
            logger.debug("Failed to parse home url: {}", home, e);
        }
        return DATA_CENTER;
    }

    /**
     * Resolves the deployment type from an explicit parameter value, falling back to detection.
     *
     * @param value the explicit value ({@code cloud} / {@code datacenter} / {@code data_center} / {@code dc}), may be blank
     * @param home the instance home URL used for detection when {@code value} is blank
     * @return the resolved deployment type
     * @throws AtlassianDataStoreException if {@code value} is non-blank and unrecognized
     */
    public static Deployment of(final String value, final String home) {
        if (StringUtil.isBlank(value)) {
            final Deployment detected = detect(home);
            logger.info("Detected Atlassian deployment: {} (from home={})", detected, home);
            return detected;
        }
        switch (value.trim().toLowerCase(Locale.ROOT)) {
        case "cloud":
            return CLOUD;
        case "datacenter", "data_center", "dc":
            return DATA_CENTER;
        default:
            throw new AtlassianDataStoreException("Invalid deployment value: \"" + value + "\". Expected \"cloud\" or \"datacenter\".");
        }
    }
}
