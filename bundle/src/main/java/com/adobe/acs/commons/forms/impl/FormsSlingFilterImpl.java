package com.adobe.acs.commons.forms.impl;

import com.adobe.acs.commons.forms.helpers.FormHelper;
import com.adobe.acs.commons.forms.helpers.ForwardFormHelper;
import com.adobe.acs.commons.forms.helpers.PostFormHelper;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestParameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;

@SlingFilter(
        label = "ACS AEM Commons - Forms POST-Handler Filter",
        description = "Servlet Filter that handles some internal routing of ACS-AEM-Commons Form POST requests to Page URIs.",
        metatype = false,
        generateComponent = true,
        generateService = true,
        order = 0,
        scope = SlingFilterScope.REQUEST)
public class FormsSlingFilterImpl implements javax.servlet.Filter {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Reference
    private PostFormHelper formHelper;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        if(!(servletRequest instanceof  SlingHttpServletRequest) ||
                !(servletResponse instanceof SlingHttpServletResponse)) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) servletRequest;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) servletResponse;

        if(!StringUtils.equals("POST", slingRequest.getMethod()) ||
                !StringUtils.equals(slingRequest.getRequestPathInfo().getSuffix(), formHelper.getSuffix())) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final RequestParameter requestParameter = slingRequest.getRequestParameter(FormHelper.FORM_RESOURCE_INPUT);
        if(requestParameter == null) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final String formResource = StringUtils.stripToEmpty(requestParameter.getString());
        if(slingRequest.getResourceResolver().resolve(formResource) == null) {
            filterChain.doFilter(servletRequest, servletResponse);
            return;
        }

        final RequestDispatcherOptions options = new RequestDispatcherOptions();

        options.setReplaceSelectors(FormHelper.SELECTOR);
        options.setReplaceSuffix("");

        slingRequest.getRequestDispatcher(formResource, options).forward(slingRequest, slingResponse);
        return;
    }

    @Override
    public void destroy() {
    }
}