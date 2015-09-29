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

package com.adobe.acs.commons.analysis.jcrchecksum.impl;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
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

/**
 * This filter adds CORS headers to allow Cross-Origin ajax requests for the Checksum Generator and JSONDumpServlet.
 */
@Component(immediate = true)
@Properties({
        @Property(name = "service.description", value = "ACS AEM Commons - JCR Checksum CORS Filter"),
        @Property(name = "pattern", value = ServletConstants.SERVLET_PATH + ".*"),
        @Property(name = "service.ranking", value = "2147483647"),
})
@Service
public class CORSOptionsFilter implements Filter {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private static final String ACCESS_CONTROL_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";

    private static final String ACCESS_CONTROL_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Headers";

    private static final String ACCESS_CONTROL_ALLOW_METHODS = "Access-Control-Allow-Methods";

    private static final String ORIGIN = "Origin";

    public final void init(FilterConfig filterConfig) throws ServletException {
        // Nothing to do
    }

    public final void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
            ServletException {

        if (request instanceof HttpServletRequest && response instanceof HttpServletResponse) {
            HttpServletRequest httpRequest = (HttpServletRequest) request;
            HttpServletResponse httpResponse = (HttpServletResponse) response;

            //Check that it is an options request.
            boolean isOptions = "OPTIONS".equalsIgnoreCase(httpRequest.getMethod());

            if (isOptions) {

                if (httpRequest.getRequestURI().startsWith(ServletConstants.SERVLET_PATH)) {
                    final String origin = httpRequest.getHeader(ORIGIN);

                    if (origin != null && origin.length() > 0) {
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_ORIGIN, origin);
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_CREDENTIALS, "true");
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_HEADERS, "Authorization");
                        httpResponse.setHeader(ACCESS_CONTROL_ALLOW_METHODS, "GET, POST");

                        // Options check has already occurred
                        httpResponse.setStatus(HttpServletResponse.SC_OK);

                        return;
                    }
                }
            }
        }

        // Always process the chain if not a CORS OPTIONS Request
        chain.doFilter(request, response);
    }

    public final void destroy() {
        // Nothing to do
    }
}