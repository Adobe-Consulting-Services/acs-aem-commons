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

import com.adobe.granite.asset.api.AssetManager;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import com.day.cq.commons.Externalizer;
import com.day.cq.search.QueryBuilder;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.api.policies.ContentPolicyManager;
import org.apache.sling.xss.XSSAPI;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.api.designer.Design;
import com.day.cq.wcm.api.designer.Designer;
import com.day.cq.wcm.api.designer.Style;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.Constants;
import com.adobe.acs.commons.models.injectors.annotation.AemObject;

import javax.jcr.Session;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Locale;

import static com.adobe.acs.commons.models.injectors.impl.InjectorUtils.*;
import static com.adobe.acs.commons.util.impl.ReflectionUtil.getClassOrGenericParam;

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
@Component(service = Injector.class)
/*
 * SERVICE_RANKING of this service should be lower than the ranking of the OsgiServiceInjector (5000),
 * otherwise the generic XSSAPI service would be injected from the OSGi Service Registry instead of the
 * pre-configured from the current request.
 */
public final class AemObjectInjector implements Injector {

    @Reference
    private XSSAPI genericXxsApi;

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


        final Class<?> clazz = getClassOrGenericParam(declaredType);
        final ObjectType typeEnum = ObjectType.fromClassAndName(clazz, name);
        if (typeEnum == null) {
            return null;
        }

        switch (typeEnum) {
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
            return resolveXssApi(adaptable);
        case LOCALE:
            return resolveLocale(adaptable);
        case TAG_MANAGER:
            return adaptFromResourceResolver(adaptable, TagManager.class);
        case ASSET_MANAGER:
            return adaptFromResourceResolver(adaptable, AssetManager.class);
        case ASSET_MANAGER_OLD:
            return adaptFromResourceResolver(adaptable, com.day.cq.dam.api.AssetManager.class);
        case QUERY_BUILDER:
            return adaptFromResourceResolver(adaptable, QueryBuilder.class);
        case CONTENT_POLICY_MANAGER:
            return adaptFromResourceResolver(adaptable,ContentPolicyManager.class);
        case EXTERNALIZER:
            return adaptFromResourceResolver(adaptable, Externalizer.class);
        default:
            return null;
        }
    }


    private Object resolveLocale(Object adaptable) {
        final Page page = getResourcePage(adaptable);
        if (page != null) {
            return page.getLanguage(false);
        }else{
            if (adaptable instanceof SlingHttpServletRequest) {
                return ((SlingHttpServletRequest) adaptable).getLocale();
            }
        }
        return null;
    }

    private Object resolveXssApi(Object adaptable) {
        XSSAPI specificApi = getXssApi(adaptable);
        if(specificApi != null){
            return specificApi;
        }else{
            return genericXxsApi;
        }
    }

    // --- inner classes ---

    /**
     * Enumeration which encapsulated the available objects.
     */
    private enum ObjectType {

        RESOURCE,
        RESOURCE_RESOLVER,
        COMPONENT_CONTEXT,
        PAGE_MANAGER,
        CURRENT_PAGE,
        RESOURCE_PAGE,
        DESIGNER,
        CURRENT_DESIGN,
        RESOURCE_DESIGN,
        CURRENT_STYLE,
        SESSION,
        LOCALE,
        TAG_MANAGER,
        QUERY_BUILDER,
        CONTENT_POLICY_MANAGER,
        ASSET_MANAGER,
        ASSET_MANAGER_OLD,
        EXTERNALIZER,
        XSS_API;

        private static final String RESOURCE_PAGE_STRING = "resourcePage";
        private static final String RESOURCE_DESIGN_STRING = "resourceDesign";

        public static ObjectType fromClassAndName(Class<?> classOrGenericParam, String name) {

            if (classOrGenericParam.isAssignableFrom(Resource.class)) {
                return ObjectType.RESOURCE;
            } else if (classOrGenericParam.isAssignableFrom(ResourceResolver.class)) {
                return ObjectType.RESOURCE_RESOLVER;
            } else if (classOrGenericParam.isAssignableFrom(ComponentContext.class)) {
                return ObjectType.COMPONENT_CONTEXT;
            } else if (classOrGenericParam.isAssignableFrom(TagManager.class)) {
                return ObjectType.TAG_MANAGER;
            } else if (classOrGenericParam.isAssignableFrom(PageManager.class)) {
                return ObjectType.PAGE_MANAGER;
            } else if (classOrGenericParam.isAssignableFrom(Page.class)) {
                return resolvePageFromName(name);
            } else if (classOrGenericParam.isAssignableFrom(Designer.class)) {
                return ObjectType.DESIGNER;
            } else if (classOrGenericParam.isAssignableFrom(Design.class)) {
                return resolveDesignFromName(name);
            } else if (classOrGenericParam.isAssignableFrom(Style.class)) {
                return ObjectType.CURRENT_STYLE;
            } else if (classOrGenericParam.isAssignableFrom(Session.class)) {
                return ObjectType.SESSION;
            } else if (classOrGenericParam.isAssignableFrom(XSSAPI.class)) {
                return ObjectType.XSS_API;
            } else if(classOrGenericParam.isAssignableFrom(Locale.class)){
                return ObjectType.LOCALE;
            } else if(classOrGenericParam.isAssignableFrom(AssetManager.class)){
                return ObjectType.ASSET_MANAGER;
            } else if(classOrGenericParam.isAssignableFrom(com.day.cq.dam.api.AssetManager.class)){
                return ObjectType.ASSET_MANAGER_OLD;
            } else if(classOrGenericParam.isAssignableFrom(QueryBuilder.class)){
                return ObjectType.QUERY_BUILDER;
            } else if(classOrGenericParam.isAssignableFrom(ContentPolicyManager.class)){
                return ObjectType.CONTENT_POLICY_MANAGER;
            } else if(classOrGenericParam.isAssignableFrom(Externalizer.class)){
                return ObjectType.EXTERNALIZER;
            }

            return null;
        }

        private static ObjectType resolveDesignFromName(String name) {
            if (name.equalsIgnoreCase(RESOURCE_DESIGN_STRING)) {
                return ObjectType.RESOURCE_DESIGN;
            } else {
                return ObjectType.CURRENT_DESIGN;
            }
        }

        private static ObjectType resolvePageFromName(String name) {
            if(name.equalsIgnoreCase(RESOURCE_PAGE_STRING)){
                return ObjectType.RESOURCE_PAGE;
            }else{
                return ObjectType.CURRENT_PAGE;
            }
        }
    }
}
