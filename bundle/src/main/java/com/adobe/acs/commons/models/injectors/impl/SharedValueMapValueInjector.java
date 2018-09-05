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

import com.adobe.acs.commons.models.injectors.annotation.SharedValueMapValue;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Array;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 4500)
public class SharedValueMapValueInjector implements Injector {
    @Reference
    private PageRootProvider pageRootProvider;

    @Override
    public String getName() {
        return "shared-component-properties-valuemap";
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(SharedValueMapValue.class) == null) {
            return null;
        }

        // sanity check
        if (!(adaptable instanceof Resource || adaptable instanceof SlingHttpServletRequest)) {
            return null;
        }

        Resource resource = null;
        if (adaptable instanceof SlingHttpServletRequest) {
            resource = ((SlingHttpServletRequest) adaptable).getResource();
        } else if (adaptable instanceof Resource) {
            resource = (Resource) adaptable;
        }

        if (resource != null) {
            Page pageRoot = pageRootProvider.getRootPage(resource);
            if (pageRoot != null) {
                ValueMap valueMap = null;
                switch (element.getAnnotation(SharedValueMapValue.class).type()) {
                    case MERGED:
                        valueMap = getMergedProperties(pageRoot, resource);
                        break;
                    case SHARED:
                        valueMap = getSharedProperties(pageRoot, resource);
                        break;
                    case GLOBAL:
                        valueMap = getGlobalProperties(pageRoot, resource);
                        break;
                    default:
                        break;
                }
                if (valueMap != null) {
                    return getValueMapValue(valueMap, name, declaredType);
                }
            }
        }

        return null;
    }

    /**
     * Get shared properties ValueMap the current resource.
     */
    protected ValueMap getSharedProperties(Page pageRoot, Resource resource) {
        String sharedPropsPath = pageRoot.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES + "/" + resource.getResourceType();
        Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
        return sharedPropsResource != null ? sharedPropsResource.getValueMap() : ValueMapDecorator.EMPTY;
    }

    /**
     * Get global properties ValueMap for the current resource.
     */
    protected ValueMap getGlobalProperties(Page pageRoot, Resource resource) {
        String globalPropsPath = pageRoot.getPath() + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
        Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
        return globalPropsResource != null ? globalPropsResource.getValueMap() : ValueMapDecorator.EMPTY;
    }

    /**
     * Get merged properties ValueMap for the current resource.
     */
    protected ValueMap getMergedProperties(Page pageRoot, Resource resource) {
        Map<String, Object> mergedProperties = new HashMap<String, Object>();

        mergedProperties.putAll(getGlobalProperties(pageRoot, resource));
        mergedProperties.putAll(getSharedProperties(pageRoot, resource));
        mergedProperties.putAll(resource.getValueMap());

        return new ValueMapDecorator(mergedProperties);
    }

    /**
     * Get the property value from the value map.
     *
     * This function has special logic to handle parameterized types
     * such as List<?> which can be addapted to from array properties.
     */
    protected Object getValueMapValue(ValueMap valueMap, String name, Type type) {
        if (type instanceof Class) {
            return valueMap.get(name, (Class) type);
        } else if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            if (parameterizedType.getActualTypeArguments().length == 1) {
                Class collectionType = (Class) parameterizedType.getRawType();
                if (collectionType.equals(Collection.class) || collectionType.equals(List.class)) {
                    Class itemType = (Class) parameterizedType.getActualTypeArguments()[0];
                    Object valuesArray = valueMap.get(name, Array.newInstance(itemType, 0).getClass());
                    if (valuesArray != null) {
                        return Arrays.asList((Object[]) valuesArray);
                    }
                }
            }
        }
        return null;
    }
}
