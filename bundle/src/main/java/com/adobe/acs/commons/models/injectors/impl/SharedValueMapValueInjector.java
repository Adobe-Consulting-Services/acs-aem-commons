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
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.adobe.acs.commons.wcm.PageRootProvider;
import com.adobe.acs.commons.wcm.properties.shared.SharedComponentProperties;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;

@Component
@Service
@Property(name = Constants.SERVICE_RANKING, intValue = 4500)
public class SharedValueMapValueInjector implements Injector {
    @Reference
    private PageRootProvider pageRootProvider;

    @Override
    public String getName() {
        return SharedValueMapValue.SOURCE;
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(SharedValueMapValue.class) == null) {
            return null;
        }

        Resource resource = getResource(adaptable);

        if (resource != null) {
            String rootPagePath = pageRootProvider.getRootPagePath(resource.getPath());
            if (StringUtils.isNotBlank(rootPagePath)) {
                ValueMap valueMap = null;
                switch (element.getAnnotation(SharedValueMapValue.class).type()) {
                    case MERGED:
                        valueMap = getMergedProperties(rootPagePath, resource);
                        break;
                    case SHARED:
                        valueMap = getSharedProperties(rootPagePath, resource);
                        break;
                    case GLOBAL:
                        valueMap = getGlobalProperties(rootPagePath, resource);
                        break;
                    default:
                        break;
                }
                if (valueMap != null) {
                    return ReflectionUtil.convertValueMapValue(valueMap, name, declaredType);
                }
            }
        }

        return null;
    }

    /**
     * Get shared properties ValueMap the current resource.
     */
    protected ValueMap getSharedProperties(String rootPagePath, Resource resource) {
        String sharedPropsPath = rootPagePath + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_SHARED_COMPONENT_PROPERTIES + "/" + resource.getResourceType();
        Resource sharedPropsResource = resource.getResourceResolver().getResource(sharedPropsPath);
        return sharedPropsResource != null ? sharedPropsResource.getValueMap() : ValueMap.EMPTY;
    }

    /**
     * Get global properties ValueMap for the current resource.
     */
    protected ValueMap getGlobalProperties(String rootPagePath, Resource resource) {
        String globalPropsPath = rootPagePath + "/" + JcrConstants.JCR_CONTENT + "/" + SharedComponentProperties.NN_GLOBAL_COMPONENT_PROPERTIES;
        Resource globalPropsResource = resource.getResourceResolver().getResource(globalPropsPath);
        return globalPropsResource != null ? globalPropsResource.getValueMap() : ValueMap.EMPTY;
    }

    /**
     * Get merged properties ValueMap for the current resource.
     */
    protected ValueMap getMergedProperties(String rootPagePath, Resource resource) {
        Map<String, Object> mergedProperties = new HashMap<>();

        mergedProperties.putAll(getGlobalProperties(rootPagePath, resource));
        mergedProperties.putAll(getSharedProperties(rootPagePath, resource));
        mergedProperties.putAll(resource.getValueMap());

        return new ValueMapDecorator(mergedProperties);
    }

}
