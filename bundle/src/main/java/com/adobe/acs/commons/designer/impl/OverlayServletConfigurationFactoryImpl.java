package com.adobe.acs.commons.designer.impl;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.Constants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Map;

@Component(
        label = "ACS Commons - Overlay Servlet",
        description = "Overlay Servlet enabling the unobtrusive overlay of Resource Types.",
        configurationFactory = true,
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        immediate = false)
@Properties({
        @Property(
                label = "Vendor",
                name = Constants.SERVICE_VENDOR,
                value = "ACS Commons",
                propertyPrivate = true
        ),
        @Property(
                label = "Source Resource Types",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.resourceTypes",
                cardinality = Integer.MAX_VALUE,
                value = {""}),
        @Property(
                label = "Source Selectors",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.selectors",
                cardinality = Integer.MAX_VALUE,
                value = {""}),
        @Property(
                label = "Source Extensions",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.extensions",
                cardinality = Integer.MAX_VALUE,
                value = {"html"}),
        @Property(
                label = "Source HTTP Methods",
                description = "Requests matching the \"Source resource types, selectors, extensions and methods\" will be overlayed using the \"Target Resource Type\"",
                name = "sling.servlet.methods",
                cardinality = Integer.MAX_VALUE,
                value = {"GET"}
        )
})
@Service(Servlet.class)
public class OverlayServletConfigurationFactoryImpl extends SlingSafeMethodsServlet {
    protected static final Logger log = LoggerFactory.getLogger(OverlayServletConfigurationFactoryImpl.class);

    private static final String DEFAULT_TARGET_RESOURCE_TYPE = "";
    private String targetResourceType = DEFAULT_TARGET_RESOURCE_TYPE;
    @Property(label = "Target Resource Type",
            description = "The resource type to proxy requests to.",
            value = DEFAULT_TARGET_RESOURCE_TYPE)
    public static final String PROP_TARGET_RESOURCE_TYPE = "prop.target-resource-type";


    public void doGet(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {
        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        if(StringUtils.isNotBlank(targetResourceType)) {
            log.debug("Overlaying Request resource type with: {}", targetResourceType);
            options.setForceResourceType(targetResourceType);
        } else {
            log.warn("Overlay Servlet's \"Target Resource Type\" is blank or null");
        }

        try {
            request.getRequestDispatcher(request.getResource(), options).forward(request, response);
        } catch (ServletException e) {
            log.error("Could not properly re-route request to overlay resource type: {}", targetResourceType);
        } catch (IOException e) {
            log.error("Could not properly re-route request to overlay resource type: {}", targetResourceType);
        }
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        targetResourceType = PropertiesUtil.toString(config.get(PROP_TARGET_RESOURCE_TYPE), "");
        log.debug("Target Resource Type: {}", targetResourceType);
    }
}