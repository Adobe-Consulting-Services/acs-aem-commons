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

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import com.day.cq.wcm.api.policies.ContentPolicy;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingBindings;
import org.apache.sling.xss.XSSAPI;

import javax.jcr.Session;
import javax.servlet.ServletRequest;

/**
 * Common methods for the injectors
 */
public class InjectorUtils {

    public static final String COM_DAY_CQ_WCM_TAGS_DEFINE_OBJECTS_TAG = "com.day.cq.wcm.tags.DefineObjectsTag";

    private InjectorUtils() {
        //static class
    }

    // --- public static stuff --
    public static Resource getResource(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) adaptable).getResource();
        }
        if (adaptable instanceof Resource) {
            return (Resource) adaptable;
        }

        return null;
    }

    public static ContentPolicy getContentPolicy(Object adaptable){
        ResourceResolver resourceResolver = getResourceResolver(adaptable);
        Resource resource = getResource(adaptable);

        if (resourceResolver != null && resource != null) {

            ContentPolicyManager manager = resourceResolver.adaptTo(ContentPolicyManager.class);

            if(manager == null){
                return  null;
            }

            final ContentPolicy policy;
            if(adaptable instanceof SlingHttpServletRequest){
                SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;
                return manager.getPolicy(resource, request);
            }else{
                return manager.getPolicy(resource);
            }
        }
        return null;
    }
    public static ResourceResolver getResourceResolver(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            return ((SlingHttpServletRequest) adaptable).getResourceResolver();
        }
        if (adaptable instanceof Resource) {
            return ((Resource) adaptable).getResourceResolver();
        }

        return null;
    }

    public static PageManager getPageManager(Object adaptable) {
        ResourceResolver resolver = getResourceResolver(adaptable);

        if (resolver != null) {
            return resolver.adaptTo(PageManager.class);
        }

        return null;
    }

    public static Designer getDesigner(Object adaptable) {
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
    public static ComponentContext getComponentContext(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = ((SlingHttpServletRequest) adaptable);

            return WCMUtils.getComponentContext(request);
        }
        // ComponentContext is not reachable from Resource

        return null;
    }

    public static Page getResourcePage(Object adaptable) {
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
    public static Page getCurrentPage(Object adaptable) {
        ComponentContext context = getComponentContext(adaptable);

        return (context != null) ? context.getPage() : null;
    }

    /**
     * Get the current design.
     *
     * @param adaptable a SlingHttpServletRequest
     * @return the current Design if the adaptable was a SlingHttpServletRequest, the default Design otherwise
     */
    public static Design getCurrentDesign(Object adaptable) {
        Page currentPage = getCurrentPage(adaptable);
        Designer designer = getDesigner(adaptable);

        if (currentPage != null && designer != null) {
            return designer.getDesign(currentPage);
        }

        return null;
    }

    public static Design getResourceDesign(Object adaptable) {
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
    public static Style getCurrentStyle(Object adaptable) {
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
    public static Session getSession(Object adaptable) {
        ResourceResolver resolver = getResourceResolver(adaptable);

        return resolver != null ? resolver.adaptTo(Session.class) : null;
    }

    /**
     * Get the XSS API.
     *
     * @param adaptable a SlingHttpServletRequest
     * @return a XSSAPI object configured for the current request, or null otherwise
     */
    public static XSSAPI getXssApi(Object adaptable) {
        if (adaptable instanceof SlingHttpServletRequest) {
            SlingHttpServletRequest request = (SlingHttpServletRequest) adaptable;

            return request.adaptTo(XSSAPI.class);
        }
        // otherwise will fetch generic XSSAPI from OSGiServiceInjector

        return null;
    }

}
