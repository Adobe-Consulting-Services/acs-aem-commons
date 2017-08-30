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
import com.adobe.acs.commons.mcp.impl.AbstractResourceImpl;
import com.adobe.acs.commons.mcp.util.StringUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * File upload component
 */
@ProviderType
public class FileUploadComponent extends FieldComponent {
    private static final Logger log = LoggerFactory.getLogger(FileUploadComponent.class);

    @Override
    public void init() {
        setResourceType("granite/ui/components/coral/foundation/form/fileupload");
        getComponentMetadata().put("text", "Upload!! " + getFieldDefinition().name());
        getComponentMetadata().put("autoStart", false);

        if (hasOption("mimeTypes")) {
            getComponentMetadata().put("mimeTypes", getOption("mimeTypes").get());
        }
    }
}
