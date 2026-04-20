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

import com.adobe.acs.commons.models.via.annotations.ContentPolicyViaType;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.ViaProviderType;
import org.apache.sling.models.spi.ViaProvider;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;

@Component(service = ViaProvider.class)
public class ContentPolicyPropertiesViaProvider implements ViaProvider {

    public static final String VIA_COMPONENT = "component";
    public static final String VIA_RESOURCE_PAGE = "resourcePage";
    public static final String VIA_CURRENT_PAGE = "currentPage";

    private static final Logger logger = LoggerFactory.getLogger(ContentPolicyPropertiesViaProvider.class);


    @Override
    public Class<? extends ViaProviderType> getType() {
        return ContentPolicyViaType.class;
    }

    @Override
    public Object getAdaptable(Object original, String value) {


        try{
            ResourceResolver resourceResolver = getResourceResolver(original);
            final Resource resource;

            if(StringUtils.equals(value, VIA_CURRENT_PAGE)){
                resource = getCurrentPage(original).getContentResource();
            }else if(StringUtils.equals(value, VIA_RESOURCE_PAGE)){
                resource = getResourcePage(original).getContentResource();
            }else{
                resource = getResource(original);
            }

            if (resourceResolver != null && resource != null) {

                ContentPolicyManager manager = resourceResolver.adaptTo(ContentPolicyManager.class);

                if(manager == null){
                    return  null;
                }

                final ContentPolicy policy = manager.getPolicy(resource);
                if(policy != null){
                    return policy.getProperties();
                }
            }

        }catch(NullPointerException ex){
            logger.error("Error while getting content policy properties", ex);
        }

        return null;

    }
}
