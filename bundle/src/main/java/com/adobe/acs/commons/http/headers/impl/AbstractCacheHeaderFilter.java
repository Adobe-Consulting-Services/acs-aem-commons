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
package com.adobe.acs.commons.http.headers.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.engine.EngineConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.http.whiteboard.HttpWhiteboardConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract class to handle standard logic for registering a cache header filter.
 * This either registers the filter with Sling or the OSGi HTTP Whiteboard.
 * It also contains the standard logic to determine if a request should have the header applied (based on parameters, method, and server-agent).
 * 
 */
public abstract class AbstractCacheHeaderFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(AbstractCacheHeaderFilter.class);

    /**
     * The name of the header used by Dispatcher to identify itself.
     */
    protected static final String SERVER_AGENT_NAME = "Server-Agent";

    protected static final String DISPATCHER_AGENT_HEADER_VALUE = "Communique-Dispatcher";
    
    private static final Set<String> AUTHORIZATION_COOKIE_NAMES = new HashSet<>(Arrays.asList("login-token", "authorization"));

    private final Collection<ServiceRegistration<Filter>> filterRegistrations = new ArrayList<>();

    
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

    /**
     * Holds the predicates for determining if a request's response should have a cache header.
     */
    static final class ServletRequestPredicates {
        protected final String[] patterns;
        protected final boolean allowAllParameters;
        protected final List<String> allowedParameterNames;
        protected final List<String> disallowedParameterNames;
        protected final boolean allowAuthorizedRequests;
        protected final boolean allowNonDispatcherRequests;

        ServletRequestPredicates(String[] patterns) {
            this(patterns, false, new String[] {}, new String[] {}, true, false);
        }

        ServletRequestPredicates(String[] patterns, boolean allowAllParameters, String[] disallowedParameterNames, String[] allowedParameterNames, boolean allowAuthorizedRequests, boolean allowNonDispatcherRequests) {
            if (patterns == null || patterns.length == 0) {
                throw new IllegalArgumentException("At least one filter pattern must be specified.");
            }
            this.patterns = patterns;
            this.allowAllParameters = allowAllParameters;
            this.allowedParameterNames = Arrays.asList(allowedParameterNames);
            this.disallowedParameterNames = Arrays.asList(disallowedParameterNames);
            this.allowAuthorizedRequests = allowAuthorizedRequests;
            this.allowNonDispatcherRequests = allowNonDispatcherRequests;
        }

        private boolean hasAllowedParameters(HttpServletRequest request) {
            if (allowAllParameters) {
                return disallowedParameterNames.stream().noneMatch(disallowedParameterName -> request.getParameterMap().containsKey(disallowedParameterName));
            }
            return request.getParameterMap().isEmpty() || allowedParameterNames.containsAll(request.getParameterMap().keySet());
        }
    }

    private final ServletRequestPredicates requestPredicates;

    AbstractCacheHeaderFilter(boolean isSlingFilter, String[] patterns, BundleContext bundleContext) {
        this(isSlingFilter, new ServletRequestPredicates(patterns), 0, bundleContext);
    }

    protected AbstractCacheHeaderFilter(boolean isSlingFilter, ServletRequestPredicates requestPredicates, int serviceRanking, BundleContext bundleContext) {
        this.requestPredicates = requestPredicates;

        for (String pattern : requestPredicates.patterns) {
            Dictionary<String, Object> filterProps = new Hashtable<>();

            log.debug("Adding filter ({}) to pattern: {}", this, pattern);
            filterProps.put(Constants.SERVICE_RANKING, serviceRanking);

            // If you want the filter ranking to work, all dispatcher filters have to be type "sling",
            // else the http-whiteboard will always have precedence
            if (isSlingFilter) {
                filterProps.put(EngineConstants.SLING_FILTER_SCOPE, EngineConstants.FILTER_SCOPE_REQUEST);
                filterProps.put(EngineConstants.SLING_FILTER_REQUEST_PATTERN, pattern);
            } else {
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, pattern);
                filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=org.apache.sling)");
            }

            ServiceRegistration<Filter> filterReg = bundleContext.registerService(Filter.class, this, filterProps);
            filterRegistrations.add(filterReg);
        }
    }

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
            // not more than one header allowed (https://datatracker.ietf.org/doc/html/rfc7234#section-4.2.1)
            if (!response.containsHeader(header)) {
                log.debug("Adding header {}: {} via filter {}", header, val, this.getClass().getSimpleName());
                response.setHeader(header, val);
            } else {
                log.debug("Header {} was already set. Skipping.", header);
            }
        }
        filterChain.doFilter(request, response);
    }


    @Override
    public void destroy() {

    }

    /**
     * Determines if this request should have the header applied.
     * @param request the request
     * @return true if the header should be applied, false otherwise.
     */
    protected boolean accepts(final HttpServletRequest request) {

        Enumeration<String> agentsEnum = request.getHeaders(SERVER_AGENT_NAME);
        List<String> serverAgents = agentsEnum != null ? Collections.list(agentsEnum) : Collections.<String>emptyList();

        // Only inject when:
        // - GET request
        // - No Params
        // - From Dispatcher
        if (StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            if (requestPredicates.hasAllowedParameters(request)) {
                if (requestPredicates.allowNonDispatcherRequests || serverAgents.contains(DISPATCHER_AGENT_HEADER_VALUE)) {
                    if (requestPredicates.allowAuthorizedRequests || !isAuthorizedRequest(request)) {
                        return true;
                    } else {
                        log.debug("Not accepting request because it is an authorized request which is not allowed in this filter.");
                    }
                } else {
                    log.debug("Not accepting request because it is not from dispatcher.");
                }
            } else {
                log.debug("Not accepting request because it contains non allowed parameters.");
            }
        } else {
            log.debug("Not accepting request because method is not GET but {}.", request.getMethod());
        }
        return false;
    }

    static boolean isAuthorizedRequest(HttpServletRequest request) {
        // same rules as outlined in https://experienceleague.adobe.com/en/docs/experience-manager-dispatcher/using/configuring/dispatcher-configuration#caching-when-authentication-is-used
        if (request.getHeader("authorization") != null) {
            return true;
        } else  if (request.getCookies() != null) {
            return Stream.of(request.getCookies()).anyMatch(cookie -> AUTHORIZATION_COOKIE_NAMES.contains(cookie.getName()));
        } else {
            return false;
        }
    }

    @Deactivate
    protected final void deactivate() {
        filterRegistrations.stream().forEach(registration -> {
            log.debug("Unregistering filter ({}) from pattern.", this);
            registration.unregister();
        });
        filterRegistrations.clear();
    }
}
