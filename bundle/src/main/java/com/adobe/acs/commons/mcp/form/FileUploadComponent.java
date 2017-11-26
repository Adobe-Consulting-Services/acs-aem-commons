/*
 * Copyright 2017 Adobe.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.adobe.acs.commons.mcp.form;

import aQute.bnd.annotation.ProviderType;

/**
 * File upload component
 */
@ProviderType
public class FileUploadComponent extends FieldComponent {

    private static final String OPTION_MIME_TYPES = "mimeTypes";

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/fileupload");
        getComponentMetadata().put("text", "Upload " + getFieldDefinition().name());
        getComponentMetadata().put("autoStart", false);

        if (hasOption(OPTION_MIME_TYPES)) {
            getComponentMetadata().put(OPTION_MIME_TYPES, getOption(OPTION_MIME_TYPES).get());
        }
    }
}
