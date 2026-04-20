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
package com.adobe.acs.commons.ccvar.impl;

import com.adobe.acs.commons.ccvar.ContentVariableProvider;
import com.adobe.acs.commons.ccvar.PropertyAggregatorService;
import org.apache.sling.api.SlingHttpServletRequest;
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

    private static final Logger LOG = LoggerFactory.getLogger(PropertyAggregatorServiceImpl.class);

    @Reference(policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MULTIPLE)
    private List<ContentVariableProvider> variableProviders;

    @Override
    public Map<String, Object> getProperties(final SlingHttpServletRequest request) {
        Map<String, Object> map = new HashMap<>();

        for (ContentVariableProvider variableProvider : variableProviders) {
            int sizeBefore = map.size();
            if (variableProvider.accepts(request)) {
                variableProvider.addProperties(map, request);
            } else {
                LOG.debug(variableProvider.getClass().getName() + " does not accept request for request at {}.", request.getPathInfo());
            }
            if (map.size() == sizeBefore) {
                LOG.debug(variableProvider.getClass().getName() + " either did not add any properties or replaced existing ones.");
            }
        }

        return map;
    }
}
