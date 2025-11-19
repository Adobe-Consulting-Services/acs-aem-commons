/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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
package com.adobe.acs.commons.granite.ui.components.impl;

import java.util.Collections;
import java.util.Map;

import org.apache.sling.api.SlingHttpServletRequest;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.acs.commons.granite.ui.components.ELVariableProvider;
import com.adobe.acs.commons.wcm.PageRootProvider;

@Component(service = ELVariableProvider.class)
public class ContextualPageRootELVariableProvider implements ELVariableProvider {
    
    @Reference
    PageRootProvider pageRootProvider;

    @Override
    public Map<String, Object> getVariables(SlingHttpServletRequest request) {
        String contentPath = request.getRequestPathInfo().getSuffix();

        String rootPath = pageRootProvider.getRootPagePath(contentPath);
        return Collections.singletonMap("acsCommonsPageRoot", rootPath);
    }
}