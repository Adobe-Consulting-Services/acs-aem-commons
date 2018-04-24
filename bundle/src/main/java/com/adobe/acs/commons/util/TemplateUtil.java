/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.util;

import javax.annotation.CheckForNull;

import org.apache.sling.api.resource.ValueMap;

import tldgen.Function;
import aQute.bnd.annotation.ProviderType;

import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;

/**
 * Utility functions for working with CQ Templates.
 */
@ProviderType
public final class TemplateUtil {

    private TemplateUtil() {
    }

    /**
     * Determine if the page is of a particular template type. This method
     * is null safe and works properly in the publish and author environments.
     * 
     * @param page the page to check
     * @param templatePath the path of the template to check
     * @return true if the page is of the defined template
     */
    @Function
    public static boolean hasTemplate(@CheckForNull final Page page, @CheckForNull final String templatePath) {
        if (page == null) {
            return false;
        }
        return hasTemplate(page.getProperties(), templatePath);
    }

    @SuppressWarnings("squid:S1144")
    private static boolean hasTemplate(@CheckForNull final ValueMap valueMap, @CheckForNull final String templatePath) {
        if (valueMap != null && templatePath != null) {
            String path = valueMap.get(NameConstants.NN_TEMPLATE, String.class);
            if (templatePath.equals(path)) {
                return true;
            }
        }
        return false;
    }

}
