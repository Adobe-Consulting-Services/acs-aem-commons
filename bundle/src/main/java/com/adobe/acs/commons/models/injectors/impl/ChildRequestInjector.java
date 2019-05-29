/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.ChildRequest;
import com.adobe.acs.commons.util.OverridePathSlingRequestWrapper;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Injector implementation for `@ChildRequest`
 * <p>
 * If the current adaptable is a `SlingHttpServletRequest`, injects a single
 * or list of `SlingHttpServletRequest` instance(s) referencing the child
 * resource path(s).
 * <p>
 * If the current adaptable is a `Resource`, injects a single or list of
 * `Resource` child resources similar to the `@ChildResource injector.
 */
@Component(
        service = {Injector.class},
        property = {
                Constants.SERVICE_RANKING + "=3000"
        }
)
public class ChildRequestInjector implements Injector {
    private static final Logger logger = LoggerFactory.getLogger(ChildRequestInjector.class);

    public String getName() {
        return "child-requests";
    }

    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
        // sanity check
        if (element.getAnnotation(ChildRequest.class) == null) {
            return null;
        }

        if (adaptable instanceof SlingHttpServletRequest) {
            logger.trace("Injecting '{}' from request", name);
            Class injectionType = null;
            if (declaredType instanceof Class) {
                injectionType = (Class) declaredType;
            } else if (this.isDeclaredTypeCollection(declaredType)) {
                injectionType = this.getActualType((ParameterizedType) declaredType);
            }
            if (injectionType != null) {
                SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;
                if (Resource.class.isAssignableFrom(injectionType)) {
                    logger.trace("Injected object type is resource, so injecting as resource rather than request");
                    return getValueForResource(request.getResource(), name, declaredType);
                } else {
                    return getValueForRequest(request, name, declaredType);
                }
            }
        } else if (adaptable instanceof Resource) {
            logger.trace("Injecting '{}' from resource (request not available)", name);
            return getValueForResource((Resource) adaptable, name, declaredType);
        }
        return null;
    }

    private Object getValueForRequest(SlingHttpServletRequest request, String name, Type declaredType) {
        Resource child = request.getResource().getChild(name);
        if (child != null) {
            if (declaredType instanceof Class) {
                return new OverridePathSlingRequestWrapper(request, child.getPath());
            } else if (this.isDeclaredTypeCollection(declaredType)) {
                List<SlingHttpServletRequest> childRequests = new ArrayList<>();
                Class type = this.getActualType((ParameterizedType) declaredType);
                if (type != null) {
                    Iterator<Resource> children = child.listChildren();
                    while (children.hasNext()) {
                        childRequests.add(new OverridePathSlingRequestWrapper(request, children.next().getPath()));
                    }
                }
                return childRequests;
            }
        }
        return null;
    }

    private Object getValueForResource(Resource resource, String name, Type declaredType) {
        Resource child = resource.getChild(name);
        if (child != null) {
            if (declaredType instanceof Class) {
                return child;
            } else if (this.isDeclaredTypeCollection(declaredType)) {
                List<Resource> childResources = new ArrayList<>();
                Class type = this.getActualType((ParameterizedType) declaredType);
                if (type != null) {
                    Iterator<Resource> children = child.listChildren();
                    while (children.hasNext()) {
                        childResources.add(children.next());
                    }
                }
                return childResources;
            }
        }
        return null;
    }

    private Class getActualType(ParameterizedType declaredType) {
        Type[] types = declaredType.getActualTypeArguments();
        return types != null && types.length > 0 ? (Class) types[0] : null;
    }

    private boolean isDeclaredTypeCollection(Type declaredType) {
        boolean isCollection = false;
        if (declaredType instanceof ParameterizedType) {
            ParameterizedType type = (ParameterizedType) declaredType;
            Class collectionType = (Class) type.getRawType();
            isCollection = collectionType.equals(Collection.class) || collectionType.equals(List.class);
        }

        return isCollection;
    }
}
