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
package org.codelibs.fess.ds.atlassian.api.confluence.content.child;

import java.util.List;

import org.codelibs.fess.ds.atlassian.api.confluence.domain.Attachment;

/**
 * Response containing a list of attachments from Confluence content.
 */
public class GetAttachmentsOfContentResponse {

    /** The list of attachments returned by the API. */
    protected List<Attachment> attachments;

    /**
     * Constructs a response with the given list of attachments.
     *
     * @param attachments the list of attachments
     */
    public GetAttachmentsOfContentResponse(final List<Attachment> attachments) {
        this.attachments = attachments;
    }

    /**
     * Returns the list of attachments.
     *
     * @return the list of attachments
     */
    public List<Attachment> getAttachments() {
        return attachments;
    }

}
