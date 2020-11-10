/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.models;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckConfigurationService;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils.DynamicDeckUtils;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * This custom select is used to populate the available collection for the current user.
 */
public class CollectionSelectComponent extends SelectComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(CollectionSelectComponent.class);

    @Override
    public Map<String, String> getOptions() {
        Map<String, String> options = new LinkedHashMap<>();
        if (null != getHelper()
                && null != getHelper().getRequest()
                && null != getHelper().getRequest().getResourceResolver()) {

            ResourceResolver resourceResolver = getHelper().getRequest().getResourceResolver();
            DynamicDeckConfigurationService configurationService = getHelper().getService(DynamicDeckConfigurationService.class);

            if (null == configurationService) {
                LOGGER.error("Configuration service is null, hence exiting the process and returning empty map");
                return Collections.emptyMap();
            }

            Map<String, String> collectionMap =
                    DynamicDeckUtils.getCollectionsListForLoggedInUser(configurationService.getCollectionQuery(),
                            resourceResolver);
            options.put(StringUtils.EMPTY, "Select the Collection");
            collectionMap.forEach((key, value) -> options.put(value, key));
        } else {
            LOGGER.error("Resource resolver is null while getting the collection list");
        }
        return options;
    }
}
