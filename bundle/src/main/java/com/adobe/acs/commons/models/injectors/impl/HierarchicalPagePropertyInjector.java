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
import org.apache.sling.models.annotations.Source;
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
                Constants.SERVICE_RANKING + ":Integer=5502"
        },
        service = Injector.class
)
public class HierarchicalPagePropertyInjector implements Injector {

    /**
     * Source value used for injector
     *
     * @see Source
     */
    public static final String SOURCE = "hierarchical-page-property";

    private static final Logger LOG = LoggerFactory.getLogger(HierarchicalPagePropertyInjector.class);

    @Override
    public String getName() {
        return SOURCE;
    }

    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
                           DisposalCallbackRegistry callbackRegistry) {

        if (!(element.isAnnotationPresent(HierarchicalPageProperty.class) || element.isAnnotationPresent(PageProperty.class))) {
            //skipping javax.Inject for performance reasons. Only supports direct injection.
            return null;
        }

        Resource currentResource = getResource(adaptable);
        if (currentResource != null) {
            Resource adaptableRes = lookUpFromPage(adaptable, currentResource, element);
            if (adaptableRes != null) {
                if (useInheritance(element)) {
                    return ReflectionUtil.convertValueMapValue(adaptableRes.getValueMap(), name, declaredType);
                } else {
                    InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(adaptableRes);
                    return ReflectionUtil.convertValueMapValue(inheritanceValueMap, name, declaredType);
                }
            }

        }
        return null;

    }

    private Resource lookUpFromPage(Object adaptable, Resource currentResource, AnnotatedElement element) {

        boolean useCurrentPage = useCurrentPage(element);
        Page containingPage;

        if(useCurrentPage){
            containingPage = getCurrentPage(adaptable);
            if(containingPage == null){
                LOG.error("Could not find current page for resource: {}. Only SlingHttpServletRequest is supported as adaptable", getResource(adaptable).getPath());
            }
        }else{
            containingPage = getResourcePage(currentResource);
        }

        if(containingPage != null && element.isAnnotationPresent(HierarchicalPageProperty.class)) {
            HierarchicalPageProperty annotation = element.getAnnotation(HierarchicalPageProperty.class);
            containingPage = containingPage.getAbsoluteParent(annotation.traverseFromAbsoluteParent());
        }

        return containingPage != null ? containingPage.getContentResource() : null;
    }


    private boolean useCurrentPage(AnnotatedElement element){

        if(element.isAnnotationPresent(HierarchicalPageProperty.class)){
            return element.getAnnotation(HierarchicalPageProperty.class).useCurrentPage();
        }else if (element.isAnnotationPresent(PageProperty.class)){
            return element.getAnnotation(PageProperty.class).useCurrentPage();
        }
        return false;

    }

    private boolean useInheritance(AnnotatedElement element){

        return element.isAnnotationPresent(PageProperty.class) || !element.getAnnotation(HierarchicalPageProperty.class).inherit();

    }


}
