/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
 * %%
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
 * #L%
 */
package com.adobe.acs.commons.widgets;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.script.Bindings;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Usage init:
 *  - data-sly-use.multiField="${ 'com.adobe.acs.commons.widgets.MultiFieldPanelWCMUse' @ name='myComponentPropertyName' }"
 *  - data-sly-use.multiField="${ 'com.adobe.acs.commons.widgets.MultiFieldPanelWCMUse' @ location=currentPage.contentResource, name='myPagePropertyName' }"
 *  - data-sly-use.multiField="${ 'com.adobe.acs.commons.widgets.MultiFieldPanelWCMUse' @ location=currentPage.parent.contentResource, name='myParentPagePropertyName' }"
 * Usage iteration
 *  - data-sly-list.value="${ multiField.values }"
 */
public class MultiFieldPanelWCMUse implements Use {
    private static final Logger log = LoggerFactory.getLogger(MultiFieldPanelWCMUse.class);

    private List<Map<String, String>> values = Collections.emptyList();

    public List<Map<String, String>> getValues() {
        return Collections.unmodifiableList(values);
    }

    @Override
    public void init(Bindings bindings) {
        Resource resource = (Resource) bindings.get(SlingBindings.RESOURCE);

        Object location = bindings.get("location");
        if (location != null) {
            if (location instanceof Resource) {
                resource = (Resource) location;
            } else {
                if (location instanceof String) {
                    resource = resource.getResourceResolver().getResource((String) location);
                }
            }
        }

        String name = (String) bindings.get("name");
        if (StringUtils.isBlank(name)) {
            log.info("Invalid property name");
            return;
        }

        values = getMultiFieldPanelValues(resource, name);
    }

    /* This is broken out into its own method to allow for easier unit testing */
    protected List<Map<String, String>> getMultiFieldPanelValues(Resource resource, final String name) {
        return MultiFieldPanelFunctions.getMultiFieldPanelValues(resource, name);
    }
}