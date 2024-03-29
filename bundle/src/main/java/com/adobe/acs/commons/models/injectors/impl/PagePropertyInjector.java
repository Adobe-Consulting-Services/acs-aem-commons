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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.HierarchicalPageProperty;
import com.adobe.acs.commons.models.injectors.annotation.PageProperty;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;

@Component(
        property = {
                Constants.SERVICE_RANKING + ":Integer=5501"
        },
        service = Injector.class
)
public class PagePropertyInjector implements Injector {

    private static final Logger LOG = LoggerFactory.getLogger(ContentPolicyValueInjector.class);

    @Override
    public String getName() {
        return PageProperty.SOURCE;
    }

    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
                           DisposalCallbackRegistry callbackRegistry) {

        if (!element.isAnnotationPresent(PageProperty.class)) {
            //skipping javax. Don't want to cause confusing behaviour for existing components.
            // Only supports direct injection to make it explicit.
            return null;
        }

        PageProperty pageProperty = element.getAnnotation(PageProperty.class);

        // current page ( SlingHttpServletRequest adaptable only)
        if(pageProperty.useCurrentPage()){
            Page currentPage = getCurrentPage(adaptable);
            if(currentPage != null){
                Resource contentResource = currentPage.getContentResource();
                ValueMap valueMap = contentResource.getValueMap();
                return ReflectionUtil.convertValueMapValue(valueMap, name, declaredType);
            }
            LOG.error("Could not find current page for resource: {}. Only SlingHttpServletRequest is supported as adaptable", getResource(adaptable).getPath());

            return null;
        }

        // resource page
        Resource currentResource = getResource(adaptable);
        if (currentResource != null) {
            Resource adaptableRes = lookUpFromPage(currentResource);
            if (adaptableRes != null) {
                ValueMap valueMap = adaptableRes.getValueMap();
                return ReflectionUtil.convertValueMapValue(valueMap, name, declaredType);
            }

        }
        return null;

    }

    private Resource lookUpFromPage(Resource currentResource) {
        Page containingPage = getResourcePage(currentResource);
        return containingPage != null ? containingPage.getContentResource() : null;
    }


}
