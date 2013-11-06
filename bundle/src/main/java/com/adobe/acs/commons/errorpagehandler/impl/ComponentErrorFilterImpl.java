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

package com.adobe.acs.commons.errorpagehandler.impl;

import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

@SlingFilter(
        label = "ACS AEM Commons - Component Error Handler Filter",
        description = "Sample implementation of a Sling Filter",
        metatype = false,
        generateComponent = false,
        generateService = false,
        order = Integer.MAX_VALUE,
        scope = SlingFilterScope.COMPONENT)
public class ComponentErrorFilterImpl implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ComponentErrorFilterImpl.class.getName());

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof SlingHttpServletRequest) ||
                !(response instanceof SlingHttpServletResponse)) {
            // Not a SlingHttpServletRequest/Response, so ignore.
            chain.doFilter(request, response);
            return;
        }

        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final Resource resource = slingRequest.getResource();

        if(ResourceUtil.isA(resource, "cq/Page")) {
            chain.doFilter(request, response);
        } else {
            try {
                chain.doFilter(request, response);
            } catch (Exception ex) {
                log.error("Error occurred while processing the resource at: {}", resource.getPath());
                response.getOutputStream().print("YOUR COMPONENT IS BROKEN!");
            }
        }
    }

    @Override
    public void destroy() {
    }
}