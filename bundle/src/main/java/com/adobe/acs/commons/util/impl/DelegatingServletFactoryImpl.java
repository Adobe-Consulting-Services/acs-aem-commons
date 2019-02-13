/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@Component(
        label = "ACS AEM Commons - Delegating Servlet",
        description = "Delegating Servlet enabling the unobtrusive delegate of Resource Types.",
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        immediate = false)
@Properties({
        @Property(
                label = "Source Resource Types",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.resourceTypes",
                cardinality = Integer.MAX_VALUE,
                value = {""}),
        @Property(
                label = "Source Selectors",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.selectors",
                cardinality = Integer.MAX_VALUE,
                value = {""}),
        @Property(
                label = "Source Extensions",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.extensions",
                cardinality = Integer.MAX_VALUE,
                value = {"html"}),
        @Property(
                label = "Source HTTP Methods",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.methods",
                cardinality = Integer.MAX_VALUE,
                value = {"GET"}
        ),
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Target type: {prop.target-resource-type}")
})
@Service(Servlet.class)
public final class DelegatingServletFactoryImpl extends SlingAllMethodsServlet {
    protected static final Logger log = LoggerFactory.getLogger(DelegatingServletFactoryImpl.class);
    private static final String REQUEST_ATTR_DELEGATION_HISTORY = DelegatingServletFactoryImpl.class.getName() + "_History";


    private static final String DEFAULT_TARGET_RESOURCE_TYPE = "";
    private String targetResourceType = DEFAULT_TARGET_RESOURCE_TYPE;
    @Property(label = "Target Resource Type",
            description = "The resource type to proxy requests to.",
            value = DEFAULT_TARGET_RESOURCE_TYPE)
    public static final String PROP_TARGET_RESOURCE_TYPE = "prop.target-resource-type";

    /** Safe HTTP Methods **/

    public void doGeneric(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doHead(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doOptions(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doTrace(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    /** Un-Safe HTTP Methods **/

    public void doDelete(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    public void doPut(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        this.delegate(request, response);
    }

    /**
     * Delegates request through to the target resource type
     *
     * @param request
     * @param response
     */
    private void delegate(final SlingHttpServletRequest request, final SlingHttpServletResponse response) throws ServletException {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();

        if(this.isCyclic(request, targetResourceType)) {
            log.error("Delegation Servlet creating a cycle for Target Resource Type: {}", targetResourceType);
            throw new ServletException("Cyclic delegation detected for " + targetResourceType);
        }

        if(StringUtils.isNotBlank(targetResourceType)) {
            log.debug("Delegating Request resource type with: {}", targetResourceType);
            options.setForceResourceType(targetResourceType);
        } else {
            log.warn("Delegating Servlet's \"Target Resource Type\" is blank or null");
        }

        try {
            this.setDelegationHistory(request, targetResourceType);
            request.getRequestDispatcher(request.getResource(), options).forward(request, response);
        } catch (ServletException e) {
            log.error("Could not properly re-route request to delegate resource type: {}", targetResourceType);
        } catch (IOException e) {
            log.error("Could not properly re-route request to delegate resource type: {}", targetResourceType);
        }
    }


    /**
     * Determines if the Request is or will be cyclic
     *
     * @param request
     * @param targetResourceType
     * @return true is Request will cause a and infinite delegation cycle
     */
    private boolean isCyclic(final SlingHttpServletRequest request, final String targetResourceType) {
        if(StringUtils.isBlank(targetResourceType)) {
            log.debug("Delegating Servlet's \"Target Resource Type\" is blank or null");
            return true;
        }

        final Set<String> history = this.getDelegationHistory(request);
        if(history.contains(targetResourceType)) {
            log.debug("Delegating Servlet's \"Target Resource Type\" is been forwarded to previously");
            return true;
        }

        return false;
    }

    /**
     * Retrieves the delegation history from the Request
     *
     * @param request
     * @return the delegation history set (of resource types previously targeted by this Servlet)
     */
    @SuppressWarnings("unchecked")
    private Set<String> getDelegationHistory(final SlingHttpServletRequest request) {
        Set<String> history = new HashSet<String>();
        final Object tmp = request.getAttribute(REQUEST_ATTR_DELEGATION_HISTORY);
        if(history.getClass().isInstance(tmp)) {
            return (Set<String>)tmp;
        } else {
            return history;
        }
    }

    /**
     * Sets the targetResourceType as part of the delegation history and adds update history set to the Request
     *
     * @param request
     * @param targetResourceType
     */
    private void setDelegationHistory(final SlingHttpServletRequest request, final String targetResourceType) {
        final Set<String> history = this.getDelegationHistory(request);
        history.add(targetResourceType);
        request.setAttribute(REQUEST_ATTR_DELEGATION_HISTORY, history);
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        targetResourceType = PropertiesUtil.toString(config.get(PROP_TARGET_RESOURCE_TYPE), "");

        if(StringUtils.isBlank(targetResourceType)) {
            throw new IllegalArgumentException("Target Resource Type must NOT be blank");
        }

        log.debug("Target Resource Type: {}", targetResourceType);
    }
}