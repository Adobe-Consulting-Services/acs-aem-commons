package com.adobe.acs.commons.designer.impl;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

@SlingServlet(
        label = "ACS Commons - Designer Routing Servlet",
        description = "Routing Servlet for the designer resource type to intercept and re-route to custom ACS-Commons implementation.",
        methods = {"GET"},
        resourceTypes = {"wcm/core/components/designer"},
        extensions = {"html"}
)
public class DesignerRoutingServletImpl extends SlingSafeMethodsServlet {
    protected static final Logger log = LoggerFactory.getLogger(DesignerRoutingServletImpl.class);

    private static final String RESOURCE_TYPE = "acs-commons/components/utilities/designer";

    public void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setForceResourceType(RESOURCE_TYPE);

        try {
            request.getRequestDispatcher(request.getResource(), options).forward(request, response);
        } catch (ServletException e) {
            log.error("Could not properly re-route request for Designer page. Clientlib Management will not be available.");
        } catch (IOException e) {
            log.error("Could not properly re-route request for Designer page. Clientlib Management will not be available.");
        }
    }
}
