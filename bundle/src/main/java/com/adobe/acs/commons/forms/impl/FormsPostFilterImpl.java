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
package com.adobe.acs.commons.forms.impl;

import com.adobe.acs.commons.forms.FormsRouter;
import com.adobe.acs.commons.forms.helpers.FormHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;

/**
 * ACS AEM Commons - Forms - Forms POST-Handler Filter
 * Request Filter that handles some internal routing of ACS-AEM-Commons Form POST requests
 *  to Page URIs.
 */
@Component
@Properties({
        @Property(
                name = "sling.filter.scope",
                value = "request"
        ),
        @Property(
                name = "filter.order",
                intValue = 0
        )
})
@Service
public class FormsPostFilterImpl implements javax.servlet.Filter {
    private static final Logger log = LoggerFactory.getLogger(FormsPostFilterImpl.class);

    @Reference
    private FormsRouter formsRouter;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public final void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                          FilterChain filterChain) throws IOException, ServletException {
        if (!(servletRequest instanceof SlingHttpServletRequest)
                || !(servletResponse instanceof SlingHttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;

        /**
         * Fail fast and early!
         *
         * Must be:
         *  - HTTP POST Request
         *  - Have Forms Sling Suffix
         *    - At this point, 99% of includes will be passed over
         *  - Must contain Form Resource Query Parameter
         */

        if (!StringUtils.equals("POST", slingRequest.getMethod())
                || !formsRouter.hasValidSuffix(slingRequest)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final String formResource = this.getParameter(slingRequest, FormHelper.FORM_RESOURCE_INPUT);
        if (formResource == null || slingRequest.getResourceResolver().resolve(formResource) == null) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        String formSelector = formsRouter.getFormSelector(slingRequest);
        if (formSelector == null) {
            formSelector = FormHelper.DEFAULT_FORM_SELECTOR;
        }

        final RequestDispatcherOptions options = new RequestDispatcherOptions();

        options.setReplaceSelectors(formSelector);
        options.setReplaceSuffix(slingRequest.getRequestPathInfo().getSuffix());

        if (log.isDebugEnabled()) {
            log.debug("Form Filter; Internal forward to path: {} ", formResource);
            log.debug("Form Filter; Internal forward w/ replace selectors: {} ", options.getReplaceSelectors());
            log.debug("Form Filter; Internal forward w/ suffix: {} ", options.getReplaceSuffix());
        }

        slingRequest.getRequestDispatcher(formResource, options).forward(slingRequest, slingResponse);
    }

    @Override
    public void destroy() {
        // no-op
    }

    private String getParameter(SlingHttpServletRequest slingRequest, String param) {
        final RequestParameter requestParameter =
                slingRequest.getRequestParameter(param);
        if (requestParameter == null) {
            return null;
        }
        return StringUtils.stripToNull(requestParameter.getString());
    }
}