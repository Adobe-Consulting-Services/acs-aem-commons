package com.adobe.acs.commons.wcm.impl;

import com.day.cq.wcm.api.AuthoringUIMode;
import com.day.cq.wcm.api.AuthoringUIModeService;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import org.apache.commons.lang.StringUtils;
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
        property = {
                "sling.filter.scope=REQUEST",
                "sling.filter.methods=GET",
                "sling.filter.resource.pattern=/etc/acs-commons/.*",
                "sling.filter.extensions=html",
                "service.ranking:Integer=-2501"
        },
        configurationPolicy = ConfigurationPolicy.REQUIRE
)
public class AcsCommonsConsoleAuthoringUIModeFilter implements Filter {

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

            // This filter only accepts GET requests to /etc/acs-commons that end with a html extension
            if (!WCMMode.DISABLED.equals(WCMMode.fromRequest(slingRequest)) && slingRequest.getResource().adaptTo(Page.class) != null) {
                Cookie authoringModeCookie = slingRequest.getCookie(WCM_AUTHORING_MODE_COOKIE);

                // Add cookie if not existing (or forced) for performance reason,
                // as it will avoid to look for user preferences next time
                if ((authoringModeCookie == null || !authoringModeCookie.getValue().equals(AuthoringUIMode.TOUCH.name())) && !slingResponse.isCommitted()) {
                    authoringModeCookie = new Cookie(WCM_AUTHORING_MODE_COOKIE, AuthoringUIMode.TOUCH.name());
                    authoringModeCookie.setPath(slingRequest.getContextPath() + "/etc/acs-commons");
                    authoringModeCookie.setMaxAge(60 * 60 * 24 * 7); // 7 days
                    slingResponse.addCookie(authoringModeCookie);
                }

                // Add request attribute
                slingRequest.setAttribute(AuthoringUIMode.REQUEST_ATTRIBUTE_NAME, AuthoringUIMode.TOUCH);
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
