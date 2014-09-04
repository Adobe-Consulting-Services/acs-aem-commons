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
package com.adobe.acs.commons.cors.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component(label = "ACS AEM Commons - CORS HTTP Request Filter",
        description = "HTTPFilter to validate the requests for CORS", configurationFactory = true,
        metatype = true, policy = ConfigurationPolicy.REQUIRE)
@Service
@Properties({
        @Property(name = Constants.SERVICE_RANKING, intValue = Integer.MIN_VALUE),
        @Property(name = "filter.scope", value = "request", propertyPrivate = true)
})
public class CORSFilter implements Filter {

    @Property(label = "host name",
            description = "Host name", value = "localhost:4502")
    private static final String HOST_NAME = "host.name";
    @Property(label = "Allow any origin?",
            description = "Allow any origin?", boolValue = false)
    private static final String ALLOW_ANY_ORIGIN = "allow.any.origin";
    @Property(label = "Allow any request method?",
            description = "Allow any request method?", boolValue = false)
    private static final String ALLOW_ANY_REQ_METHOD = "allow.any.req.method";
    @Property(label = "Allow any request headers?",
            description = "Allow any request headers?", boolValue = false)
    private static final String ALLOW_ANY_REQ_HEADERS = "allow.any.req.headers";
    @Property(label = "expose any request headers?",
            description = "expose any request headers?", boolValue = false)
    private static final String EXPOSE_ANY_REQ_HEADERS = "expose.any.req.headers";
    @Property(label = "Allow credentials?",
            description = "Allow credentials?", boolValue = true)
    private static final String ALLOW_CREDENTIALS = "allow.credentials";
    @Property(label = "Allow any subdomain origin?",
            description = "Allow any subdomain origin?", boolValue = false)
    private static final String ALLOW_SUBDOMAINS = "allow.any.subdomain.origin";
    @Property(label = "Allowed request methods in Uppercase",
            description = "Allowed request methods", cardinality = 100)
    private static final String ALLOWED_REQUEST_METHODS = "allowed.request.methods";
    @Property(label = "Allowed request headers",
            description = "Allowed request headers", cardinality = 100, value = {"GET", "OPTIONS", "POST"})
    private static final String ALLOWED_REQUEST_HEADERS = "allowed.request.headers";
    @Property(label = "Allowed exposable request methods (comma " +
            "separated)",
            description = "Allowed exposable request headers", value = "Authorization")
    private static final String ALLOWED_EXPOSE_REQUEST_HEADERS = "allowed.expose.request.headers";
    @Property(
            label = "allowed origins",
            description = "allowed origins for the given host",
            cardinality = 100, value = {"http://localhost"})
    private static final String ALLOWED_ORIGINS = "allowed.origins";
    @Property(label = "Max age (in seconds)",
            description = "Max age", intValue = 10)
    private static final String MAX_AGE = "cors.maxage";

    private String host = "";
    private Boolean allowAnyOrigin;
    private Boolean allowSubdomainOrigin;
    private Boolean allowAnyReqMethods;
    private Boolean allowAnyRequestHeaders;
    private Boolean exposeAnyRequestHeaders;
    private Boolean allowCredentials;
    private Set<Origin> allowedOrigins;
    private Set<String> allowedMethods;
    private Set<String> allowedRequestHeaders;
    private String allowedExposeRequestHeaders;
    private Long maxAge;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {
        HttpServletRequest httpServletRequest =
                httpServletRequest = (HttpServletRequest) request;
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;

        REQUESTTYPE reqType = REQUESTTYPE.from(httpServletRequest, host);
        switch (reqType) {
            case OTHER:
                chain.doFilter(request, response);
                break;
            case ACTUAL:
                isRequestMethodAllowed(httpServletRequest.getMethod(), httpServletResponse);
                isOriginAllowed(httpServletRequest, httpServletResponse);
                allowCredentials(httpServletRequest, httpServletResponse);
                exposeAnyReqHeaders(httpServletRequest, httpServletResponse);
                chain.doFilter(request, response);
                break;
            case PREFLIGHT:
                isOriginAllowed(httpServletRequest, httpServletResponse);
                isRequestMethodAllowed(httpServletRequest, httpServletResponse);
                allowCredentials(httpServletRequest, httpServletResponse);
                addMaxAge(httpServletRequest, httpServletResponse);
                isRequestHeadersAllowed(httpServletRequest, httpServletResponse);
        }
    }

    @Override
    public void destroy() {

    }

    private void exposeAnyReqHeaders(HttpServletRequest request, HttpServletResponse response) {
        if (this.exposeAnyRequestHeaders) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_EXPOSE_HEADERS, "*");
        } else if (!this.allowedExposeRequestHeaders.isEmpty()) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_EXPOSE_HEADERS, this.allowedExposeRequestHeaders);
        }

    }

    private void addMaxAge(HttpServletRequest request, HttpServletResponse response) {
        if (this.maxAge > 0) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_MAX_AGE, Long.toString(this.maxAge));
        }

    }

    private void allowCredentials(HttpServletRequest request, HttpServletResponse response) {
        if (this.allowCredentials) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
        }

    }

    private void isRequestHeadersAllowed(HttpServletRequest request, HttpServletResponse response) {
        String rawRequestHeaderString = request.getHeader(CORSConstants.ACCESS_CONTROL_REQUEST_HEADERS);
        String[] requestHeaders = getHeadersAsArray(rawRequestHeaderString);
        if (requestHeaders == null || requestHeaders.length == 0) {
            return;
        }
        if (!this.allowAnyRequestHeaders) {
            for (String header : requestHeaders) {
                if (!allowedRequestHeaders.contains(header)) {
                    return;
                }
            }
        }
        response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_HEADERS, rawRequestHeaderString);

    }

    private void isRequestMethodAllowed(HttpServletRequest request, HttpServletResponse response) {
        String requestMethod = PropertiesUtil.toString(request.getHeader(CORSConstants.ACCESS_CONTROL_REQUEST_METHOD)
                , "").toUpperCase();
        if (this.allowAnyReqMethods || allowedMethods.contains(requestMethod)) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_METHODS, requestMethod);
        }
    }

    private void isRequestMethodAllowed(String requestMethod, HttpServletResponse response) {

        if (this.allowAnyReqMethods || allowedMethods.contains(requestMethod)) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_METHODS, requestMethod);
        }
    }

    private void isOriginAllowed(HttpServletRequest request, HttpServletResponse response) {
        String originStr = request.getHeader(CORSConstants.ORIGIN);
        if (StringUtils.isEmpty(originStr)) {
            return;
        }
        Origin origin = null;
        try {
            origin = new Origin(originStr);
        } catch (URISyntaxException e) {
            return;
        }
        if (this.allowAnyOrigin) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.getOriginStr());
            response.addHeader(CORSConstants.VARY, CORSConstants.ORIGIN);
        } else if (allowedOrigins.contains(origin)) {
            response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.getOriginStr());
            response.addHeader(CORSConstants.VARY, CORSConstants.ORIGIN);
        } else if (allowSubdomainOrigin) {
            if (isSubdomainAllowed(origin)) {
                response.addHeader(CORSConstants.ACCESS_CONTROL_ALLOW_ORIGIN, origin.getOriginStr());
                response.addHeader(CORSConstants.VARY, CORSConstants.ORIGIN);
            }
        }

    }

    private boolean isSubdomainAllowed(Origin origin) {

        for (Origin allowedOrigin : allowedOrigins) {
            if (origin.getHost().endsWith(allowedOrigin.getHost()) && origin.getPort() == allowedOrigin.getPort() &&
                    origin.getScheme().equals(allowedOrigin.getScheme())) {
                return true;
            }
        }

        return false;
    }

    @Activate
    @Modified
    protected void activate(Map<String, Object> properties) {
        this.host = PropertiesUtil.toString(properties.get(HOST_NAME), "");
        this.allowAnyOrigin = PropertiesUtil.toBoolean(properties.get(ALLOW_ANY_ORIGIN), false);
        this.allowCredentials = PropertiesUtil.toBoolean(properties.get(ALLOW_CREDENTIALS), false);
        this.allowSubdomainOrigin = PropertiesUtil.toBoolean(properties.get(ALLOW_SUBDOMAINS), false);
        this.allowAnyReqMethods = PropertiesUtil.toBoolean(properties.get(ALLOW_ANY_REQ_METHOD), false);
        this.allowAnyRequestHeaders = PropertiesUtil.toBoolean(properties.get(ALLOW_ANY_REQ_HEADERS), false);
        this.exposeAnyRequestHeaders = PropertiesUtil.toBoolean(properties.get(EXPOSE_ANY_REQ_HEADERS), false);
        String[] allowedOrigins = PropertiesUtil.toStringArray(properties.get(ALLOWED_ORIGINS), new String[0]);
        this.allowedOrigins = getAllowedOriginsAsSet(allowedOrigins);
        String[] allowedMethods = PropertiesUtil.toStringArray(properties.get(ALLOWED_REQUEST_METHODS), new String[0]);
        this.allowedMethods = new HashSet<String>(Arrays.asList(allowedMethods));
        String[] allowedRequestHeaders = PropertiesUtil.toStringArray(properties.get(ALLOWED_REQUEST_HEADERS), new String[0]);
        this.allowedRequestHeaders = new HashSet<String>(Arrays.asList(allowedRequestHeaders));
        this.maxAge = PropertiesUtil.toLong(properties.get(MAX_AGE), -1);
        this.allowedExposeRequestHeaders = PropertiesUtil.toString(properties.get(ALLOWED_EXPOSE_REQUEST_HEADERS), "");
    }

    private Set<Origin> getAllowedOriginsAsSet(String[] allowedOrigins) {
        Set<Origin> originSet = new HashSet<Origin>();
        for (String originStr : allowedOrigins) {
            Origin origin = null;
            try {
                origin = new Origin(originStr);
                originSet.add(origin);
            } catch (URISyntaxException e) {

            }

        }
        return originSet;
    }

    private enum REQUESTTYPE {
        //cors preflight req
        PREFLIGHT,
        //cors actual req
        ACTUAL,
        //same domain req or req with no origin  or req for a different domain
        OTHER;

        static REQUESTTYPE from(final HttpServletRequest request, String host) {
            String currHost = request.getHeader("Host");
            String originStr = request.getHeader(CORSConstants.ORIGIN);
            if (!StringUtils.equals(host, currHost) || StringUtils.isEmpty(originStr) || StringUtils.equals(request
                    .getScheme() + "://" + currHost, originStr)) {
                return OTHER;
            } else if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
                return PREFLIGHT;
            } else {
                return ACTUAL;
            }

        }
    }
        private static String[] getHeadersAsArray(final String headerValue) {

        if (headerValue == null)
            return new String[0]; // empty array

        String trimmedHeaderValue = headerValue.trim();

        if (trimmedHeaderValue.isEmpty())
            return new String[0];

        return trimmedHeaderValue.split("\\s*,\\s*|\\s+");
    }
}
