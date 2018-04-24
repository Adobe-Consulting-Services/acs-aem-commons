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

package com.adobe.acs.commons.http.injectors;

import com.adobe.acs.commons.util.BufferingResponse;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Deactivate;
import org.osgi.framework.ServiceRegistration;
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
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;

public abstract class AbstractHtmlRequestInjector implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AbstractHtmlRequestInjector.class);

    private ServiceRegistration filterRegistration;

    @Override
    public void init(final FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public final void doFilter(final ServletRequest servletRequest, final ServletResponse servletResponse,
                               final FilterChain filterChain) throws IOException, ServletException {
        
        if (!this.accepts(servletRequest, servletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        // We know these are HTTP Servlet Requests since accepts passed
        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        final HttpServletResponse response = (HttpServletResponse) servletResponse;

        // Prepare to capture the original response
        final BufferingResponse originalResponse = new BufferingResponse(response);

        // Process and capture the original response
        filterChain.doFilter(request, originalResponse);

        // Get contents
        final String originalContents = originalResponse.getContents();

        if (originalContents != null 
                && StringUtils.contains(response.getContentType(), "html")) {

            final int injectionIndex = getInjectIndex(originalContents);
            
            if (injectionIndex != -1) {
                final PrintWriter printWriter = response.getWriter();

                // Write all content up to the injection index
                printWriter.write(originalContents.substring(0, injectionIndex));

                // Inject the contents; Pass the request/response - consumer can use as needed
                inject(request, response, printWriter);

                // Write all content after the injection index
                printWriter.write(originalContents.substring(injectionIndex));
                return;
            }
        }

        if (originalContents != null) {
            response.getWriter().write(originalContents);
        }
    }

    protected abstract void inject(HttpServletRequest request, HttpServletResponse response, PrintWriter printWriter);

    protected abstract int getInjectIndex(String originalContents);

    @Override
    public void destroy() {

    }

    @SuppressWarnings("squid:S3923")
    protected boolean accepts(final ServletRequest servletRequest,
                            final ServletResponse servletResponse) {

        if (!(servletRequest instanceof HttpServletRequest)
                || !(servletResponse instanceof HttpServletResponse)) {
            return false;
        }

        final HttpServletRequest request = (HttpServletRequest) servletRequest;
        
        if (!StringUtils.equalsIgnoreCase("get", request.getMethod())) {
            // Only inject on GET requests
            return false;
        } else if (StringUtils.equals(request.getHeader("X-Requested-With"), "XMLHttpRequest")) {
            // Do not inject into XHR requests
            return false;
        } else if (StringUtils.contains(request.getPathInfo(), ".") 
                && !StringUtils.contains(request.getPathInfo(), ".html")) {
            // If extension is provided it must be .html
            return false;
        } else if (StringUtils.endsWith(request.getHeader("Referer"), "/editor.html" + request.getRequestURI())) {
            // Do not apply to pages loaded in the TouchUI editor.html
            return false;
        } else if (StringUtils.endsWith(request.getHeader("Referer"), "/cf")) {
            // Do not apply to pages loaded in the Classic Content Finder
            return false;
        }
        
        // Add HTML check
        if (log.isTraceEnabled()) {
            log.trace("Injecting HTML via AbstractHTMLRequestInjector");
        }
        return true;
    }

    @SuppressWarnings("squid:S1149")
    protected final void registerAsFilter(ComponentContext ctx, int ranking, String pattern) {
        Dictionary<String, String> filterProps = new Hashtable<String, String>();

        filterProps.put("service.ranking", String.valueOf(ranking));
        filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_FILTER_REGEX, StringUtils.defaultIfEmpty(pattern, ".*"));
        filterProps.put(HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_SELECT, "(" + HttpWhiteboardConstants.HTTP_WHITEBOARD_CONTEXT_NAME + "=*)");
        filterRegistration = ctx.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
    }

    @SuppressWarnings("squid:S1149")
    protected final void registerAsSlingFilter(ComponentContext ctx, int ranking, String pattern) {
        Dictionary<String, String> filterProps = new Hashtable<String, String>();

        filterProps.put("service.ranking", String.valueOf(ranking));
        filterProps.put("sling.filter.scope", "REQUEST");
        filterProps.put("sling.filter.pattern", StringUtils.defaultIfEmpty(pattern, ".*"));
        filterRegistration = ctx.getBundleContext().registerService(Filter.class.getName(), this, filterProps);
    }

    protected final void unregisterFilter() {
        if (filterRegistration != null) {
            filterRegistration.unregister();
            filterRegistration = null;
        }
    }

    @Deactivate
    protected void deactivate(ComponentContext ctx) {
        this.unregisterFilter();
    }
}
