/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2024 Adobe
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
package com.adobe.acs.commons.models.via.impl;


import com.adobe.acs.commons.models.via.annotations.PageContentResourceViaType;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

@Component(service = ViaProvider.class)
public class PageContentResourceViaProvider implements ViaProvider {

    public static final String VIA_CURRENT_PAGE = "currentPage";

    private static final Logger logger = LoggerFactory.getLogger(PageContentResourceViaProvider.class);

    @Override
    public Class<? extends ViaProviderType> getType() {
        return PageContentResourceViaType.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {


        try{
            final Resource resource;

            if(StringUtils.equals(value, VIA_CURRENT_PAGE)){
                resource = getCurrentPage(original).getContentResource();

                if(resource == null){
                    logger.error("Could not find current page for resource: {}. Only SlingHttpServletRequest is supported as adaptable", getResource(original).getPath());
                }
            }else{
                resource = getResourcePage(original).getContentResource();
            }

            return resource;

        }catch(NullPointerException ex){
            logger.error("Error while getting content policy properties", ex);
        }

        return null;

    }
}
