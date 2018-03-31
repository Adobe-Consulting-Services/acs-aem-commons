/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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
package com.adobe.acs.commons.http.headers.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class to handle standard logic for registering a Dispatcher TTL header filter.
 */
public abstract class AbstractDispatcherCacheHeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AbstractDispatcherCacheHeaderFilter.class);

    public static final String PROP_FILTER_PATTERN = "filter.pattern";

    protected static final String SERVER_AGENT_NAME = "Server-Agent";

    protected static final String DISPATCHER_AGENT_HEADER_VALUE = "Communique-Dispatcher";

    private List<ServiceRegistration> filterRegistrations = new ArrayList<ServiceRegistration>();

    /**
     * Get the value to place in the Cache-Control header.
     * 
     * @return the value of the Cache-Control header
     */
    protected abstract String getHeaderName();

    /**
     * Get the value to place in the Cache-Control header.
     * 
     * @return the value of the Cache-Control header
     */
    protected abstract String getHeaderValue();

    /*
     * Allow sub-classes to process their own activation logic.
     */
    protected abstract void doActivate(ComponentContext context) throws Exception;

    private static final Object MARKER = new Object();

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
            final FilterChain filterChain) throws IOException, ServletException {

        if (!(servletRequest instanceof HttpServletRequest) || !(servletResponse instanceof HttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        if (this.accepts(request)) {
            String header = getHeaderName();
            String val = getHeaderValue();
            String attributeName = AbstractDispatcherCacheHeaderFilter.class.getName() + ".header." + header;
            if (request.getAttribute(attributeName) == null) {
                log.debug("Adding header {}: {}", header, val);
                response.addHeader(header, val);
                request.setAttribute(attributeName, MARKER);
            } else {
                log.debug("Header {} was already set. Skipping.", header);
            }
        }
        filterChain.doFilter(request, response);
    }


    @Override
    public void destroy() {

    }

    @SuppressWarnings("unchecked")
    protected boolean accepts(final HttpServletRequest request) {

        Enumeration<String> agentsEnum = request.getHeaders(SERVER_AGENT_NAME);
        List<String> serverAgents = agentsEnum != null ? Collections.list(agentsEnum) : Collections.<String>emptyList();

        // Only inject when:
        // - GET request
        // - No Params
        // - From Dispatcher
        if (StringUtils.equalsIgnoreCase("get", request.getMethod())
                && request.getParameterMap().isEmpty()
                && serverAgents.contains(DISPATCHER_AGENT_HEADER_VALUE)) {

            return true;
        }
        return false;
    }

    @Activate
    @SuppressWarnings("squid:S1149")
    protected final void activate(ComponentContext context) throws Exception {
        Dictionary<?, ?> properties = context.getProperties();

        doActivate(context);

        String[] filters = PropertiesUtil.toStringArray(properties.get(PROP_FILTER_PATTERN));
        if (filters == null || filters.length == 0) {
            throw new ConfigurationException(PROP_FILTER_PATTERN, "At least one filter pattern must be specified.");
        }

        for (String pattern : filters) {
            Dictionary<String, String> filterProps = new Hashtable<String, String>();

            log.debug("Adding filter ({}) to pattern: {}", this.toString(), pattern);
            filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, pattern);
            filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)");
            ServiceRegistration filterReg = context.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
            filterRegistrations.add(filterReg);
        }
    }

    @Deactivate
    protected final void deactivate(ComponentContext context) {

        for(Iterator<ServiceRegistration> it = filterRegistrations.iterator(); it.hasNext(); ) {
            ServiceRegistration registration = it.next();
            registration.unregister();
            it.remove();
        }
    }
}
