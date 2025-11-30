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

package com.adobe.acs.commons.wcm.impl;

import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.AuthoringUIModeService;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component(
    service = Filter.class,
    
        property = {
                "sling.filter.scope=REQUEST",
                "sling.filter.methods=GET",
                "sling.filter.resource.pattern=.*/bin/wcmcommand",
                "service.ranking:Integer=10000"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class AcsCommonsConsoleEditorFilter implements Filter {

    @Reference
    private AuthoringUIModeService authoringUIModeService;

    private static final String WCM_AUTHORING_MODE_COOKIE = "cq-authoring-mode";

    /**
     * {@inheritDoc}
     */
    public void init(FilterConfig filterConfig) throws ServletException {
        // ignore
    }

    /**
     * {@inheritDoc}
     */
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain filterChain) throws IOException, ServletException {
        if (request instanceof SlingHttpServletRequest) {
            final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
            final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

            /**
             * http://localhost:4502/bin/wcmcommand?cmd=open&path=/etc/acs-commons/dispatcher-flush
             */

            if (StringUtils.equals("open", slingRequest.getParameter("cmd"))
                    && StringUtils.startsWith(slingRequest.getParameter("path"), "/etc/acs-commons")) {
                // This is an open request, so we need force to open in the editor

                final String pagePath = slingRequest.getParameter("path");

                final PageManager pageManager = slingRequest.getResourceResolver().adaptTo(PageManager.class);
                final Page page = pageManager.getContainingPage(pagePath);

                final com.day.cq.wcm.api.components.Component component = WCMUtils.getComponent(page.getContentResource());
                final String view = component.getProperties().get(NameConstants.PN_DEFAULT_VIEW, page.getContentResource().getValueMap().get(NameConstants.PN_DEFAULT_VIEW, String.class));

                if (!StringUtils.equals(view, "html")) {
                    // ACS Commons /etc/acs-commons pages should always open in TouchUI Editor
                    String url = slingRequest.getContextPath() + "/editor.html" + pagePath + ".html";

                    slingResponse.sendRedirect(url);
                    return;
                }

            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * {@inheritDoc}
     */
    public void destroy() {
        // ignore
    }
}
