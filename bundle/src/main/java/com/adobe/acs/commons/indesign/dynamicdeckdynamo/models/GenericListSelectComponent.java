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

import com.adobe.acs.commons.genericlists.GenericList;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Custom SelectComponent to Support Select list from GenericList
 */
public class GenericListSelectComponent extends SelectComponent {

    private static final Logger LOGGER = LoggerFactory.getLogger(GenericListSelectComponent.class);
    public static final String GENERIC_LIST_PATH = "genericListPath";


    /**
     * Override to support options for select list from Generic Lists
     *
     * @return
     */
    @Override
    public Map<String, String> getOptions() {

        Map<String, String> options = new LinkedHashMap<>();
        if (null != getHelper()
                && null != getHelper().getRequest()
                && null != getHelper().getRequest().getResourceResolver()) {

            ResourceResolver resourceResolver = getHelper().getRequest().getResourceResolver();
            PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

            if (pageManager == null) {
                LOGGER.debug("Page manager is null, hence exiting the process and returning empty map");
                return Collections.emptyMap();
            }

            if (!hasOption(GENERIC_LIST_PATH)) {
                LOGGER.debug("Generic list path is null, hence exiting the process and returning empty map");
                return Collections.emptyMap();
            }
            Optional<String> listPath = getOption(GENERIC_LIST_PATH);
            if (!listPath.isPresent()) {
                LOGGER.debug("Generic list path under getOption is null, hence exiting the select options process and returning empty map");
                return Collections.emptyMap();
            }
            Page genericListPage = pageManager.getPage(listPath.get());

            if (genericListPage == null) {
                LOGGER.debug("Generic List Page is null, hence exiting the select options process and returning empty map");
                return Collections.emptyMap();
            }

            GenericList itemList = genericListPage.adaptTo(GenericList.class);
            if (itemList == null) {
                return Collections.emptyMap();
            }

            options.put(StringUtils.EMPTY, "Select the Option");
            itemList.getItems().forEach(item -> options.put(item.getValue(), item.getTitle()));
        } else {
            LOGGER.error("Resource resolver is null while getting the generic list");
        }
        return options;

    }
}
