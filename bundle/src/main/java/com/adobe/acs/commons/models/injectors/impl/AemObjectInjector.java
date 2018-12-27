/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.AemObject;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getComponentContext;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getCurrentDesign;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getCurrentPage;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getCurrentStyle;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getDesigner;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getPageManager;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResource;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResourceDesign;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResourcePage;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getResourceResolver;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getSession;
import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.getXssApi;

/**
 * Sling Models Injector which injects the Adobe AEM objects defined in
 * <a href="http://bit.ly/1gmlmfE">&lt;cq:defineObjects/&gt;</a>.
 * <p>
 * the following objects can be injected:
 * <ul>
 * <li> resource the current resource
 * <li> resourceResolver the current resource resolver
 *
 * <li> componentContext component context of this request
 *
 * <li> pageManager page manager
 * <li> currentPage containing page addressed by the request
 * <li> resourcePage containing page of the addressed resource
 *
 * <li> designer the designer
 * <li> currentDesign design of the addressed resource
 * <li> resourceDesign design of the addressed resource
 *
 * <li> currentStyle style addressed by the request
 * <li> session the current session
 * <li> xssApi cross site scripting provider for the current request
 * </ul>
 *
 * Note: This Injector requires at least org.apache.sling.models.impl version 1.0.2
 *
 */
@Component(service=Injector.class, property= {Constants.SERVICE_RANKING +":Integer=4500"})
/*
 * SERVICE_RANKING of this service should be lower than the ranking of the OsgiServiceInjector (5000),
 * otherwise the generic XSSAPI service would be injected from the OSGi Service Registry instead of the
 * pre-configured from the current request.
 */
public final class AemObjectInjector implements Injector {


    @Override
    public String getName() {
        return AemObject.SOURCE;
    }

    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element,
            DisposalCallbackRegistry callbackRegistry) {

        // sanity check
        if (!(adaptable instanceof Resource || adaptable instanceof SlingHttpServletRequest)) {
            return null;
        }

        ObjectType nameEnum = ObjectType.fromString(name);
        if (nameEnum == null) {
            return null;
        }

        switch (nameEnum) {
        case RESOURCE:
            return getResource(adaptable);
        case RESOURCE_RESOLVER:
            return getResourceResolver(adaptable);
        case COMPONENT_CONTEXT:
            return getComponentContext(adaptable);
        case PAGE_MANAGER:
            return getPageManager(adaptable);
        case CURRENT_PAGE:
            return getCurrentPage(adaptable);
        case RESOURCE_PAGE:
            return getResourcePage(adaptable);
        case DESIGNER:
            return getDesigner(adaptable);
        case CURRENT_DESIGN:
            return getCurrentDesign(adaptable);
        case RESOURCE_DESIGN:
            return getResourceDesign(adaptable);
        case CURRENT_STYLE:
            return getCurrentStyle(adaptable);
        case SESSION:
            return getSession(adaptable);
        case XSS_API:
            return getXssApi(adaptable);
        default:
            return null;
        }
    }

    // --- inner classes ---

    /**
     * Enumeration which encapsulated the available objects.
     */
    private enum ObjectType {

        RESOURCE("resource"),
        RESOURCE_RESOLVER("resourceResolver"),
        COMPONENT_CONTEXT("componentContext"),
        PAGE_MANAGER("pageManager"),
        CURRENT_PAGE("currentPage"),
        RESOURCE_PAGE("resourcePage"),
        DESIGNER("designer"),
        CURRENT_DESIGN("currentDesign"),
        RESOURCE_DESIGN("resourceDesign"),
        CURRENT_STYLE("currentStyle"),
        SESSION("session"),
        XSS_API("xssApi");

        private String text;

        ObjectType(String text) {
            this.text = text;
        }

        public static ObjectType fromString(String text) {
            if (text != null) {
                for (ObjectType b : ObjectType.values()) {
                    if (text.equalsIgnoreCase(b.text)) {
                        return b;
                    }
                }
            }

            return null;
        }
    }
}
