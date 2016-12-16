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

import com.adobe.cq.sightly.WCMUse;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class MultiFieldPanelWCMUse extends WCMUse {
    private static final Logger log = LoggerFactory.getLogger(MultiFieldPanelWCMUse.class);

    private List<Map<String, String>> values = Collections.emptyList();

    @Override
    public void activate() throws Exception {

        // handle name as a mandatory parameter
        String name = get("name", String.class);
        if (StringUtils.isBlank(name)) {
            log.info("Invalid property name");
            return;
        }

        // assume current resource as default
        Resource resource = get("location", Resource.class);
        if (resource == null) {
            resource = getResource();
        }

        values = MultiFieldPanelFunctions.getMultiFieldPanelValues(resource, name);
    }

    public List<Map<String, String>> getValues() {
        return values;
    }
}
