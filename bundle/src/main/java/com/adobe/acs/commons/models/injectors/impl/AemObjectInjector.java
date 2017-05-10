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

import org.apache.sling.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;

import javax.jcr.Session;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

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
@Component
@Service
/*
 * SERVICE_RANKING of this service should be lower than the ranking of the OsgiServiceInjector (5000),
 * otherwise the generic XSSAPI service would be injected from the OSGi Service Registry instead of the
 * pre-configured from the current request.
 */
@Property(name = Constants.SERVICE_RANKING, intValue = 4500)
public final class AemObjectInjector implements Injector {

    private static final String COM_DAY_CQ_WCM_TAGS_DEFINE_OBJECTS_TAG = "com.day.cq.wcm.tags.DefineObjectsTag";

    @Override
    public String getName() {
        return "define-objects";
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

    // --- private stuff --
    private Resource getResource(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) adaptable).getResource();
        }
        if (adaptable instanceof Resource) {
            return (Resource) adaptable;
        }

        return null;
    }

    private ResourceResolver getResourceResolver(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) adaptable).getResourceResolver();
        }
        if (adaptable instanceof Resource) {
            return ((Resource) adaptable).getResourceResolver();
        }

        return null;
    }

    private PageManager getPageManager(Object adaptable) {
        ResourceResolver resolver = getResourceResolver(adaptable);

        if (resolver != null) {
            return resolver.adaptTo(PageManager.class);
        }

        return null;
    }

    private Designer getDesigner(Object adaptable) {
        ResourceResolver resolver = getResourceResolver(adaptable);

        if (resolver != null) {
            return resolver.adaptTo(Designer.class);
        }

        return null;
    }

    /**
     * Get the current component context.
     * 
     * @param adaptable a SlingHttpServletRequest
     * @return the ComponentContext if the adaptable was a SlingHttpServletRequest, or null otherwise
     */
    private ComponentContext getComponentContext(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = ((SlingHttpServletRequest) adaptable);

            return WCMUtils.getComponentContext(request);
        }
        // ComponentContext is not reachable from Resource

        return null;
    }

    private Page getResourcePage(Object adaptable) {
        PageManager pageManager = getPageManager(adaptable);
        Resource resource = getResource(adaptable);

        if (pageManager != null && resource != null) {
            return pageManager.getContainingPage(resource);
        }

        return null;
    }

    /**
     * Get the current page.
     *
     * @param adaptable a SlingHttpServletRequest
     * @return the current Page if the adaptable was a SlingHttpServletRequest, null otherwise
     */
    private Page getCurrentPage(Object adaptable) {
        ComponentContext context = getComponentContext(adaptable);

        return (context != null) ? context.getPage() : null;
    }

    /**
     * Get the current design.
     *
     * @param adaptable a SlingHttpServletRequest
     * @return the current Design if the adaptable was a SlingHttpServletRequest, the default Design otherwise
     */
    private Design getCurrentDesign(Object adaptable) {
        Page currentPage = getCurrentPage(adaptable);
        Designer designer = getDesigner(adaptable);

        if (currentPage != null && designer != null) {
            return designer.getDesign(currentPage);
        }

        return null;
    }

    private Design getResourceDesign(Object adaptable) {
        Page resourcePage = getResourcePage(adaptable);
        Designer designer = getDesigner(adaptable);

        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;

            if (resourcePage != null && designer != null) {
                String resourceDesignKey = COM_DAY_CQ_WCM_TAGS_DEFINE_OBJECTS_TAG + resourcePage.getPath();
                Object cachedResourceDesign = request.getAttribute(resourceDesignKey);

                if (cachedResourceDesign != null) {
                    return (Design) cachedResourceDesign;
                } else {
                    Design resourceDesign = designer.getDesign(resourcePage);
                    request.setAttribute(resourceDesignKey, resourceDesign);

                    return resourceDesign;
                }
            }
        }

        if (adaptable instanceof Resource) {
            return designer != null ? designer.getDesign(resourcePage) : null;
        }

        return null;
    }

    /**
     * Get the current style.
     * 
     * @param adaptable a SlingHttpServletRequest
     * @return the current Style if the adaptable was a SlingHttpServletRequest, null otherwise
     */
    private Style getCurrentStyle(Object adaptable) {
        Design currentDesign = getCurrentDesign(adaptable);
        ComponentContext componentContext = getComponentContext(adaptable);

        if (currentDesign != null && componentContext != null) {
            return currentDesign.getStyle(componentContext.getCell());
        }

        return null;
    }

    /**
     * Get the session.
     *
     * @param adaptable Either a SlingHttpServletRequest or a Resource
     * @return the current Session
     */
    private Session getSession(Object adaptable) {
        ResourceResolver resolver = getResourceResolver(adaptable);

        return resolver != null ? resolver.adaptTo(Session.class) : null;
    }

    /**
     * Get the XSS API.
     *
     * @param adaptable a SlingHttpServletRequest
     * @return a XSSAPI object configured for the current request, or null otherwise
     */
    private XSSAPI getXssApi(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;

            return request.adaptTo(XSSAPI.class);
        }
        // otherwise will fetch generic XSSAPI from OSGiServiceInjector

        return null;
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
