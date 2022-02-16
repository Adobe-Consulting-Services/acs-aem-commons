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
import org.osgi.framework.Constants;
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
import java.util.Arrays;
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
    public static final String PROP_ALLOW_ALL_PARAMS = "allow.all.params";
    public static final String PROP_PASS_THROUGH_PARAMS = "pass.through.params";
    public static final String PROP_BLOCK_PARAMS = "block.params";

    protected static final String SERVER_AGENT_NAME = "Server-Agent";

    protected static final String DISPATCHER_AGENT_HEADER_VALUE = "Communique-Dispatcher";

    public static final String PROP_DISPATCHER_FILTER_ENGINE = "dispatcher.filter.engine";
    public static final String PROP_DISPATCHER_FILTER_ENGINE_SLING = "sling";
    public static final String PROP_DISPATCHER_FILTER_ENGINE_HTTP_WHITEBOARD = "http-whiteboard";

    private List<ServiceRegistration> filterRegistrations = new ArrayList<ServiceRegistration>();

    private boolean allowAllParams = false;
    private List<String> passThroughParams = new ArrayList<String>();
    private List<String> blockParams = new ArrayList<String>();

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
    protected abstract String getHeaderValue(HttpServletRequest request);


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
            String val = getHeaderValue(request);
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
                && hasValidParameters(request)
                && serverAgents.contains(DISPATCHER_AGENT_HEADER_VALUE)) {

            return true;
        }
        return false;
    }

    private boolean hasValidParameters(HttpServletRequest request) {
        if (allowAllParams) {
            return blockParams.stream().noneMatch(blockParam -> request.getParameterMap().containsKey(blockParam));
        }
        return request.getParameterMap().isEmpty() || passThroughParams.containsAll(request.getParameterMap().keySet());
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

        String filterEngine = PropertiesUtil.toString(properties.get(PROP_DISPATCHER_FILTER_ENGINE), PROP_DISPATCHER_FILTER_ENGINE_HTTP_WHITEBOARD);

        allowAllParams = PropertiesUtil.toBoolean(properties.get(PROP_ALLOW_ALL_PARAMS), false);
        passThroughParams = Arrays.asList(PropertiesUtil.toStringArray(properties.get(PROP_PASS_THROUGH_PARAMS), new String[0]));
        blockParams = Arrays.asList(PropertiesUtil.toStringArray(properties.get(PROP_BLOCK_PARAMS), new String[0]));

        for (String pattern : filters) {
            Dictionary<String, Object> filterProps = new Hashtable<String, Object>();

            log.debug("Adding filter ({}) to pattern: {}", this, pattern);
            filterProps.put(Constants.SERVICE_RANKING, PropertiesUtil.toInteger(properties.get(Constants.SERVICE_RANKING), 0));

            // If you want the filter ranking to work, all dispatcher filters have to be type "sling",
            // else the http-whiteboard will always have precedence
            if ("sling".equals(filterEngine)) {
                filterProps.put("sling.filter.scope", "REQUEST");
                filterProps.put("sling.filter.request.pattern", pattern);
            } else {
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, pattern);
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.apache.sling)");
            }

            ServiceRegistration filterReg = context.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
            filterRegistrations.add(filterReg);
        }
    }


    @Deactivate
    protected final void deactivate(ComponentContext context) {

        for (Iterator<ServiceRegistration> it = filterRegistrations.iterator(); it.hasNext(); ) {
            ServiceRegistration registration = it.next();
            registration.unregister();
            it.remove();
        }
    }
}
