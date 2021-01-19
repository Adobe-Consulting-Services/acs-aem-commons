/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
package com.adobe.acs.commons.properties.impl;

import com.adobe.acs.commons.properties.ContentVariableProvider;
import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.sling.api.resource.Resource;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(service = PropertyAggregatorService.class,
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
public class PropertyAggregatorServiceImpl implements PropertyAggregatorService {

    private static final Logger log = LoggerFactory.getLogger(PropertyAggregatorServiceImpl.class);

    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    private List<ContentVariableProvider> variableProviders;

    @Override
    public Map<String, Object> getProperties(final Resource resource) {
        Map<String, Object> map = new HashMap<>();

        if (resource == null) {
            return map;
        }
        PageManager pageManager = resource.getResourceResolver().adaptTo(PageManager.class);
        if (pageManager == null) {
            return map;
        }
        Page currentPage = pageManager.getContainingPage(resource);

        if (currentPage == null) {
            log.warn("No containing page found for resource at {}", resource.getPath());
            return map;
        }

        for (ContentVariableProvider variableProvider : variableProviders) {
            int sizeBefore = map.size();
            if (variableProvider.accepts(currentPage)) {
                variableProvider.addProperties(map, currentPage);
            } else {
                log.debug(variableProvider.getClass().getName() + " does not accept request for page at {}.", currentPage.getPath());
            }
            if (map.size() == sizeBefore) {
                log.debug(variableProvider.getClass().getName() + " did not add any properties.");
            }
        }

        return map;
    }

    @Override
    public Map<String, Object> getProperties(final Page page) {
        return getProperties(page.getContentResource());
    }
}
