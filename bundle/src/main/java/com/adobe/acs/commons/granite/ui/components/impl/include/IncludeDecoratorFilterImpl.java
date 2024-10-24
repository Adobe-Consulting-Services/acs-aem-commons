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
package com.adobe.acs.commons.granite.ui.components.impl.include;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.collections4.MapUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.engine.EngineConstants;
import org.jetbrains.annotations.Nullable;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@Component(
        service = Filter.class,
        configurationPolicy = ConfigurationPolicy.OPTIONAL,
        property= {
                EngineConstants.SLING_FILTER_SCOPE+"="+ EngineConstants.FILTER_SCOPE_INCLUDE
        }
)
@Designate(ocd = IncludeDecoratorFilterImpl.Config.class)
public class IncludeDecoratorFilterImpl implements Filter {

    static final String RESOURCE_TYPE = "acs-commons/granite/ui/components/include";

    /**
     * Request attribute of the include namespace
     */
    static final String REQ_ATTR_NAMESPACE = "ACS_AEM_COMMONS_INCLUDE_NAMESPACE";
    /**
     * Parameters node name
     */
    static final String NN_PARAMETERS = "parameters";

    /**
     * Property name parameters
     */
    static final String PN_NAMESPACE = "namespace";

    /**
     * Prefix value parameters passed downwards
     */
    static final String PREFIX = "ACS_AEM_COMMONS_INCLUDE_PREFIX_";


    static final String REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE = "ACS_AEM_COMMONS_EXCLUDE_CHILDREN_RESOURCE_TYPE";

    private List<String> resourceTypesIgnoreChildren;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - IncludeDecoratorFilterImpl",
            description = "Used to perform the namespacing / parameterization in the include context")
    public @interface Config {
        String[] resourceTypesIgnoreChildren() default {
                "granite/ui/components/coral/foundation/form/multifield"
        };
    }

    @Activate
    @Modified
    public void init(Config config) {
        this.resourceTypesIgnoreChildren = Arrays.asList(config.resourceTypesIgnoreChildren());
    }


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {

        ValueMap parameters = ValueMap.EMPTY;

        if(!(servletRequest instanceof SlingHttpServletRequest)){
            chain.doFilter(servletRequest, servletResponse);
            return;
        }
        SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;

        Predicate<String> typeCheckFn = (resourceType) -> request.getResourceResolver().isResourceType(request.getResource(), resourceType);

        if(typeCheckFn.test(RESOURCE_TYPE)){
            Object ignoreResourceType = request.getAttribute(REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE);
            Object namespace = request.getAttribute(REQ_ATTR_NAMESPACE);
            //if children ignore is active, but we have a new include, we de-activate the ignore children.
            if(ignoreResourceType != null){
                request.removeAttribute(REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE);
                request.removeAttribute(REQ_ATTR_NAMESPACE);
            }
            performFilter(request, servletResponse, chain, parameters);
            // we are now out of the nested include context. re-activate the ignore children if it was active before.
            if(ignoreResourceType != null){
                request.setAttribute(REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE, ignoreResourceType);
                request.setAttribute(REQ_ATTR_NAMESPACE, namespace);
            }
        }else if(resourceTypesIgnoreChildren.stream().anyMatch(typeCheckFn)){
            boolean ignoreChildren = resourceTypesIgnoreChildren.stream().anyMatch(typeCheckFn);
            if(ignoreChildren){
                request.setAttribute(REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE, request.getResource().getResourceType());
            }
            chain.doFilter(servletRequest, servletResponse);
            if(ignoreChildren){
                request.removeAttribute(REQ_ATTR_IGNORE_CHILDREN_RESOURCE_TYPE);
            }
        }

    }

    private void performFilter(SlingHttpServletRequest request, ServletResponse servletResponse, FilterChain chain, ValueMap parameters) throws IOException, ServletException {
        @Nullable Resource parameterResource = request.getResource().getChild(NN_PARAMETERS);
        if(parameterResource != null){
            parameters = parameterResource.getValueMap();
        }

        ValueMap includeProperties = request.getResource().getValueMap();

        Object existingNamespace = request.getAttribute(REQ_ATTR_NAMESPACE);
        boolean hasExistingNamespace = existingNamespace != null;
        boolean hasNamespaceInInclude = includeProperties.containsKey(PN_NAMESPACE);

        if(MapUtils.isNotEmpty(parameters)){
            parameters.forEach((key, object) -> {
                request.setAttribute(PREFIX + key, object);
            });
        }

        if(hasNamespaceInInclude && hasExistingNamespace){
            request.setAttribute(REQ_ATTR_NAMESPACE, existingNamespace + "/" + includeProperties.get(PN_NAMESPACE, String.class));
        }else if(hasNamespaceInInclude){
            request.setAttribute(REQ_ATTR_NAMESPACE, includeProperties.get(PN_NAMESPACE, String.class));
        }

        chain.doFilter(request, servletResponse);

        if(MapUtils.isNotEmpty(parameters)){
            parameters.forEach((key, object) -> {
                request.removeAttribute(PREFIX + key);
            });
        }

        if(existingNamespace != null){
            request.setAttribute(REQ_ATTR_NAMESPACE, existingNamespace);
        }else{
            request.removeAttribute(REQ_ATTR_NAMESPACE);
        }
    }


    @Override
    public void destroy() {
        // no-op
    }
}
