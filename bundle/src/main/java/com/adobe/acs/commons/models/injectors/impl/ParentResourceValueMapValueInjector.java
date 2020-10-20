/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.models.injectors.impl;

import com.adobe.acs.commons.models.injectors.annotation.ParentResourceValueMapValue;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.foundation.forms.MergedValueMap;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Injects the parent component property
 */
@Component(property = Constants.SERVICE_RANKING + ":Integer=" + Integer.MAX_VALUE, service = Injector.class)
public class ParentResourceValueMapValueInjector implements Injector {

    @Override
    public String getName() {
        return ParentResourceValueMapValue.SOURCE;
    }

    @Override
    public Object getValue(Object adaptable, String name, Type type, AnnotatedElement annotatedElement, DisposalCallbackRegistry disposalCallbackRegistry) {
        Integer maxLevel = annotatedElement.getAnnotation(ParentResourceValueMapValue.class).maxLevel();
        if (adaptable instanceof Resource) {
            return getInheritedProperty((Resource)adaptable, name, type, maxLevel);

        } else if (adaptable instanceof SlingHttpServletRequest) {
            Resource adaptableResource = ((SlingHttpServletRequest) adaptable).getResource();
            return getInheritedProperty(adaptableResource, name, type, maxLevel);
        }
        return null;
    }

    /**
     * Returns the property by iterating through the parent component resources excluding the cq:Page resource
     * @param resource Resource
     * @param propertyName Property name to be retrieved
     * @param declaredType Declared class type
     * @param maxLevel Max level of parents to iterate
     * @return Object
     */
    private Object getInheritedProperty(Resource resource, String propertyName, Type declaredType, Integer maxLevel) {
        List<Resource> parentResources = new ArrayList<>();
        int count = 0;
        Resource parentResource = resource.getParent();
        do {
            if (parentResource != null && !parentResource.getResourceType().equals(NameConstants.NT_PAGE)) {
                parentResources.add(resource.getParent());
                count++;
            } else {
                break;
            }
            parentResource = parentResource.getParent();
        }
        while (count < maxLevel || maxLevel <= 0);
        return new MergedValueMap(parentResources).get(propertyName, (Class<?>) declaredType);
    }
}
