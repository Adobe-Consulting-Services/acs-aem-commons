/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.TagProperty;
import com.adobe.acs.commons.util.impl.ReflectionUtil;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResourceResolver;
import static org.apache.commons.lang3.StringUtils.defaultIfEmpty;

@Component(
        service = {Injector.class},
        property = {
                Constants.SERVICE_RANKING + ":Integer=5520"
        }
)
public class TagPropertyInjector implements Injector {

    private static final Logger logger = LoggerFactory.getLogger(TagPropertyInjector.class);

    @NotNull
    @Override
    public String getName() {
        return TagProperty.SOURCE;
    }

    @Nullable
    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {

        if (element.isAnnotationPresent(TagProperty.class)) {

            // only inject directly. @ValueMapValue will handle the rest
            TagProperty annotation = element.getAnnotation(TagProperty.class);

            ResourceResolver resourceResolver = getResourceResolver(adaptable);

            if(resourceResolver == null) {
                logger.error("ResourceResolver is null, cannot inject tag property. Are you adapting from a resource or SlingHttpServletRequest?");
                return null;
            }

            TagManager tagManager = resourceResolver.adaptTo(TagManager.class);
            if(tagManager == null) {
                logger.error("TagManager is null, cannot inject tag property. Are you adapting from a resource or SlingHttpServletRequest?");
                return null;
            }
            String key =  defaultIfEmpty(annotation.value(), name);;
            final String[] tagKeys;
            if(annotation.inherit()){
                InheritanceValueMap inheritanceValueMap = new HierarchyNodeInheritanceValueMap(getResource(adaptable));
                tagKeys = inheritanceValueMap.getInherited(key, String[].class);
            }else{
                tagKeys = getResource(adaptable).getValueMap().get(key, String[].class);
            }
            if(tagKeys == null || tagKeys.length == 0){
                return null;
            }

            final Stream<Tag> tagStream = Arrays.stream(tagKeys).map(tagManager::resolve);

            if(ReflectionUtil.isSetType(declaredType) && ReflectionUtil.getGenericParameter(declaredType).equals(Tag.class)){
                return tagStream.collect(Collectors.toSet());
            }else if((ReflectionUtil.isListType(declaredType) || ReflectionUtil.isCollectionType(declaredType)) && ReflectionUtil.getGenericParameter(declaredType).equals(Tag.class)){
                return tagStream.collect(Collectors.toList());
            }else if(ReflectionUtil.isArray(declaredType) && ReflectionUtil.getClassOrGenericParam(declaredType).equals(Tag.class)) {
                // array
                return tagStream.toArray(Tag[]::new);
            }else{
                // single value, resolve first tag
                return tagStream.filter(Objects::nonNull).findFirst().orElse(null);
            }

        }

        return null;
    }
}
