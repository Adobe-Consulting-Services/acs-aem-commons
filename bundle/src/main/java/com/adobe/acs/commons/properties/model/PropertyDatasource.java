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
package com.adobe.acs.commons.properties.model;

import java.util.Map;

import javax.annotation.PostConstruct;
import javax.inject.Inject;

import com.adobe.acs.commons.properties.PropertyAggregatorService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Model(adaptables = SlingHttpServletRequest.class)
public class PropertyDatasource {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertyDatasource.class);

    @Self
    private SlingHttpServletRequest request;

    @Inject
    private PropertyAggregatorService propertyAggregatorService;

    private Map<String, Object> properties;
    private ObjectMapper objectMapper = new ObjectMapper();

    @PostConstruct
    protected void init() {
        Resource componentResource = request.getRequestPathInfo().getSuffixResource();
        if (componentResource != null) {
            Page currentPage = request.getResourceResolver().adaptTo(PageManager.class)
                    .getContainingPage(componentResource);
            if (currentPage != null) {
                properties = propertyAggregatorService.getProperties(currentPage);
            }
        }
    }

    public String getJson() {
        if (properties != null) {
            try {
                return StringEscapeUtils.escapeHtml4(objectMapper.writeValueAsString(properties));
            } catch (JsonProcessingException e) {
                LOGGER.error("Error serializing JSON from properties");
            }
        }
        return "{}";
    }
}
