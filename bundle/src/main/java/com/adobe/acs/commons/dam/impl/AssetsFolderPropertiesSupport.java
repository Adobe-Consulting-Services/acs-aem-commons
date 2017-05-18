package com.adobe.acs.commons.dam.impl;

import com.day.text.Text;
import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.CompositeValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Assets Folder Properties Support",
        metatype = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                name = "sling.filter.scope",
                value = "request",
                propertyPrivate = true
        ),
        @Property(
                name = "filter.order",
                intValue = 10000,
                propertyPrivate = true
        ),
        @Property(
                name = "sling.servlet.methods",
                value = "GET",
                propertyPrivate = true
        ),
        @Property(
                name = "sling.servlet.resourceTypes",
                value = "acs-commons/touchui-widgets/asset-folder-properties-support",
                propertyPrivate = true
        ),
})
@Service
public class AssetsFolderPropertiesSupport extends SlingSafeMethodsServlet implements Filter {
    private static final Logger log = LoggerFactory.getLogger(AssetsFolderPropertiesSupport.class);

    private static final String[] DEFAULT_PROPERTIES = new String[]{};
    private List<String> properties = null;
    @Property(label = "Allowed properties to persist",
            description = "Relative paths from /content/dam/.../[sling:Folder] | [sling:OrderedFolder]",
            value = {})
    public static final String PROP_PROPERTIES = "properties";

    public static final String GRANITE_UI_FORM_VALUES = "granite.ui.form.values";

    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;

        if (!accepts(request)) {
            chain.doFilter(servletRequest, servletResponse);
            return;
        }

        chain.doFilter(servletRequest, servletResponse);

        if (log.isTraceEnabled()) {
            log.trace("Accepted by AssetsFolderPropertiesSupport [ {} ]", request.getResource().getPath());
        }

        for (final String property : properties) {
            log.debug("Looking for request parameter named [ {} ] in [ {} ]", property, StringUtils.join(request.getRequestParameterMap().keySet()));
            boolean deleteHint = false;
            boolean propertyParamExists = false;

            if (request.getRequestParameterMap().keySet().contains(property)) {
                log.trace("Request parameter named [ {} ] does NOT exist", property);
                propertyParamExists = true;
            }

            if (request.getRequestParameterMap().keySet().contains(property + "@Delete")) {
                log.trace("Delete hint'd request parameter named [ {} ] does NOT exist", property + "@Delete");
                deleteHint = true;
            }

            if (!propertyParamExists && !deleteHint) {
                // Cannot find the property nor a delete hint, so just skip it!
                continue;
            }

            final RequestParameter requestParameter = request.getRequestParameter(property);

            String value = null;
            if (requestParameter != null) {
                value = requestParameter.getString("UTF-8");
            }

            final String canonicalPath = Text.makeCanonicalPath(request.getResource().getPath() + "/" + StringUtils.removeStart(property, "/"));
            final String resourcePath = StringUtils.substringBeforeLast(canonicalPath, "/");
            final String propertyName = StringUtils.substringAfterLast(canonicalPath, "/");
            final Resource resource = ResourceUtil.getOrCreateResource(request.getResourceResolver(),
                    resourcePath, new HashMap<String, Object>(), JcrConstants.NT_UNSTRUCTURED, false);

            ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

            log.debug("Processing Assets Folder property [ {} -> {} ]", propertyName, value);

            if (deleteHint) {
                mvm.remove(propertyName);
                log.debug("Processed @Delete hint for property [ {} ] from [ {} ]", propertyName, resource.getPath());
            }

            if (value == null && !deleteHint) {
                mvm.remove(propertyName);
                log.debug("Removed property [ {} ] from [ {} ]", propertyName, resource.getPath());
            } else {
                mvm.put(propertyName, value);
                log.debug("Added/updated property [ {} ] on [ {} ]", propertyName, resource.getPath());
            }
        }

        if (request.getResourceResolver().hasChanges()) {
            request.getResourceResolver().commit();
            if (log.isTraceEnabled()) {
                log.trace("Saving property changes to the Assets Folder [ {} ]", request.getResource().getPath());
            }
        }
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // Do Nothing
    }

    public void destroy() {
        // Do Nothing
    }

    protected boolean accepts(SlingHttpServletRequest request) {
        Resource resource = request.getResource();
        if (request == null || resource == null) {
            return false;
        } else if (!StringUtils.equals(request.getParameter(":operation"), "dam.share.folder")) {
            return false;
        } else if (!StringUtils.equalsIgnoreCase("post", request.getMethod())) {
            return false;
        } else if (!StringUtils.startsWith(resource.getPath(), "/content/dam")) {
            // Must be in /content/dam
            return false;
        } else if (!resource.isResourceType("sling:Folder") && !resource.isResourceType("sling:OrderedFolder")) {
            return false;
        } else if (resource.adaptTo(ModifiableValueMap.class) == null) {
            return false;
        }

        return true;
    }


    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        final Resource suffixResource = request.getRequestPathInfo().getSuffixResource();
        if (suffixResource == null) { return; }

        log.trace("AssetsFolderPropertiesSupport GET method for folder resource [ {} ]", suffixResource.getPath());

        ValueMap formProperties = (ValueMap) request.getAttribute(GRANITE_UI_FORM_VALUES);

        if (formProperties == null) {
            formProperties = new ValueMapDecorator(new HashMap<String, Object>());
        }

        ValueMap composite = new CompositeValueMap(formProperties, suffixResource.getValueMap(), true);

        request.setAttribute(GRANITE_UI_FORM_VALUES, composite);
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        properties = new ArrayList<String>();
        final String[] tmp = PropertiesUtil.toStringArray(config.get(PROP_PROPERTIES), DEFAULT_PROPERTIES);
        for (String property : tmp) {
            if (StringUtils.isNotBlank(property)) {
                properties.add(property);
            }
        }

        log.info("Initialized AssetsFolderPropertiesSupport with registered properties: {}", properties);
    }
}



