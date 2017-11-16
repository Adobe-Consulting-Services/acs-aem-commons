/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.util.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Pattern;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.ArrayUtils;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.api.components.ComponentManager;

/**
 * Filter which can accept/reject requests based on the selectors, extensions, and/or suffixes.
 * Allowed selector, extensions, and suffix patterns are defined on the component resource using the property names
 * defined in the constants in this class.
 */
@org.apache.felix.scr.annotations.Component(policy = ConfigurationPolicy.REQUIRE)
@SlingFilter(scope = SlingFilterScope.REQUEST, order = Integer.MIN_VALUE, generateComponent = false)
@SuppressWarnings("squid:S3776")
public class UrlFilter implements Filter {

    /**
     * Regular expression for the allowed extensions. Property with this name must be single-valued.
     */
    protected static final String PN_ALLOWED_EXTENSION_PATTERN = "allowedExtensionPattern";

    /**
     * List of values which will be allowed extensions.
     * If this property is an empty array, no extensions will be allowed.
     */
    protected static final String PN_ALLOWED_EXTENSIONS = "allowedExtensions";

    /**
     * Regular expression for the allowed selectors. Property with this name must be single-valued.
     */
    protected static final String PN_ALLOWED_SELECTOR_PATTERN = "allowedSelectorPattern";

    /**
     * List of values which will be allowed selectors.
     * If this property is an empty array, no selectors will be allowed.
     */
    protected static final String PN_ALLOWED_SELECTORS = "allowedSelectors";

    /**
     * Regular expression for the allowed suffixes. Property with this name must be single-valued.
     */
    protected static final String PN_ALLOWED_SUFFIX_PATTERN = "allowedSuffixPattern";

    /**
     * List of values which will be allowed suffixes.
     * If this property is an empty array, no suffixes will be allowed.
     */
    protected static final String PN_ALLOWED_SUFFIXES = "allowedSuffixes";

    private static final Collection<String> PROPERTY_NAMES = Arrays.asList(PN_ALLOWED_SUFFIXES, PN_ALLOWED_EXTENSIONS,
            PN_ALLOWED_SELECTORS, PN_ALLOWED_SUFFIX_PATTERN, PN_ALLOWED_SELECTOR_PATTERN, PN_ALLOWED_EXTENSION_PATTERN);

    private static final Logger log = LoggerFactory.getLogger(UrlFilter.class);

    public void destroy() {
        // nothing to do
    }

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        if (request instanceof SlingHttpServletRequest && response instanceof SlingHttpServletResponse) {
            SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

            RequestPathInfo pathInfo = slingRequest.getRequestPathInfo();

            Component definitionComponent = findUrlFilterDefinitionComponent(slingRequest.getResource(),
                    slingRequest.getResourceResolver().adaptTo(ComponentManager.class));

            if (definitionComponent != null) {
                String definitionPath = definitionComponent.getPath();
                log.debug("found url filter definition resource at {}", definitionPath);
                ValueMap properties = definitionComponent.getProperties();
                if (properties != null) {
                    if (checkSelector(pathInfo, properties) && checkSuffix(pathInfo, properties)
                            && checkExtension(pathInfo, properties)) {
                        log.debug("url filter definition resource at {} passed for request {}.",
                                definitionPath, slingRequest.getRequestPathInfo());
                    } else {
                        log.info("url filter definition resource at {} FAILED for request {}.",
                                definitionPath, slingRequest.getRequestPathInfo());
                        slingResponse.sendError(403);
                        return;
                    }
                }
            }

        }

        chain.doFilter(request, response);

    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // nothing to do
    }

    protected boolean checkExtension(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getExtension(), PN_ALLOWED_EXTENSIONS, PN_ALLOWED_EXTENSION_PATTERN, properties);
    }

    protected boolean checkSelector(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getSelectorString(), PN_ALLOWED_SELECTORS, PN_ALLOWED_SELECTOR_PATTERN, properties);
    }

    private boolean check(String value, String allowedArrayPropertyName, String allowedPatternPropertyName, ValueMap properties) {
        if (value == null) {
            // no value is always allowed
            return true;
        }
        String[] allowedValues = properties.get(allowedArrayPropertyName, String[].class);
        if (allowedValues != null) {
            if (allowedValues.length == 0) {
                log.debug("{} was empty, therefore not allowing any value.", allowedArrayPropertyName);
                return false;
            } else if (!ArrayUtils.contains(allowedValues, value)) {
                log.debug("{} did not contain our string {}. checking the pattern.", allowedArrayPropertyName, value);
                String allowedPattern = properties.get(allowedPatternPropertyName, String.class);
                if (allowedPattern == null || !Pattern.matches(allowedPattern, value)) {
                    log.debug("allowedPattern ({}) did not match our string {}", allowedPattern, value);
                    return false;
                } else {
                    log.debug("allowedPattern ({}) did match our string {}", allowedPattern, value);
                    return true;
                }
            } else {
                return true;
            }
        } else {
            String allowedPattern = properties.get(allowedPatternPropertyName, String.class);
            if (allowedPattern != null && !Pattern.matches(allowedPattern, value)) {
                log.debug("allowedPattern ({}) did not match our string {}", allowedPattern, value);
                return false;
            } else {
                return true;
            }
        }
    }

    protected boolean checkSuffix(RequestPathInfo pathInfo, ValueMap properties) {
        return check(pathInfo.getSuffix(), PN_ALLOWED_SUFFIXES, PN_ALLOWED_SUFFIX_PATTERN, properties);
    }

    private Component findUrlFilterDefinitionComponent(Resource resource, ComponentManager componentManager) {
        if (resource == null) {
            return null;
        }
        
        Resource contentResource = resource.getChild("jcr:content");
        if (contentResource != null) {
            resource = contentResource;
        }

        Component component = componentManager.getComponentOfResource(resource);

        return findUrlFilterDefinitionComponent(component);
    }

    private Component findUrlFilterDefinitionComponent(Component component) {
        if (component == null) {
            return null;
        }
        
        ValueMap properties = component.getProperties();
        // Collections.disjoint returns true if the collections
        // have nothing in common, so when it is false, use the current resource
        if (!Collections.disjoint(properties.keySet(), PROPERTY_NAMES)) {
            return component;
        } else {
            // otherwise, look at the resource type resource's super type
            return findUrlFilterDefinitionComponent(component.getSuperComponent());
        }
    }

}
