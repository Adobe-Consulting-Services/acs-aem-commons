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

import com.adobe.acs.commons.util.ResourceDataUtil;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.commons.WCMUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Component-Level Error Handler",
        description = "Handles errors at the component level. Allows different HTML renditions to display for erring "
                + "components based on WCM Mode collections (Edit, Preview, Publish).",
        policy = ConfigurationPolicy.REQUIRE,
        metatype = true,
        immediate = false
)
@Properties({
    @Property(
        name = "sling.filter.scope",
        value = "component",
        propertyPrivate =  true
    ),
    @Property(
        name = "filter.order",
        intValue = ComponentErrorHandlerImpl.FILTER_ORDER,
        propertyPrivate = true
    )
})
@Service(javax.servlet.Filter.class)
public class ComponentErrorHandlerImpl implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ComponentErrorHandlerImpl.class.getName());

    // Magic number pushes filter lower in the chain so it executes after the
    // OOTB WCM Debug Filter
    static final int FILTER_ORDER = 1001;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ComponentHelper componentHelper;

    /* Edit Mode */

    private static final boolean DEFAULT_EDIT_ENABLED = true;
    private boolean editModeEnabled = DEFAULT_EDIT_ENABLED;
    @Property(label = "Edit Error Handling",
            description = "Enable handling of Edit-mode errors (EDIT, DESIGN, ANALYTICS)",
            boolValue = DEFAULT_EDIT_ENABLED)
    public static final String PROP_EDIT_ENABLED = "prop.edit.enabled";

    private static final String DEFAULT_EDIT_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/edit.html";
    private String editErrorHTMLPath = DEFAULT_EDIT_ERROR_HTML_PATH;
    @Property(label = "Edit HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in EDIT or DESIGN modes.",
            value = DEFAULT_EDIT_ERROR_HTML_PATH)
    public static final String PROP_EDIT_ERROR_HTML_PATH = "prop.edit.html";

    /* Preview Mode */

    private static final boolean DEFAULT_PREVIEW_ENABLED = false;
    private boolean previewModeEnabled = DEFAULT_PREVIEW_ENABLED;
    @Property(label = "Preview Error Handling",
            description = "Enable handling of Edit-mode errors (PREVIEW and READ_ONLY)",
            boolValue = DEFAULT_PREVIEW_ENABLED)
    public static final String PROP_PREVIEW_ENABLED = "prop.preview.enabled";

    private static final String DEFAULT_PREVIEW_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/preview.html";
    private String previewErrorHTMLPath = DEFAULT_PREVIEW_ERROR_HTML_PATH;
    @Property(label = "Preview HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in PREVIEW or READONLY modes.",
            value = DEFAULT_PREVIEW_ERROR_HTML_PATH)
    public static final String PROP_PREVIEW_ERROR_HTML_PATH = "prop.preview.html";

    private static final String DEFAULT_PUBLISH_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/publish.html";

    /* Publish Mode */

    private static final boolean DEFAULT_PUBLISH_ENABLED = false;
    private boolean publishModeEnabled = DEFAULT_PUBLISH_ENABLED;
    @Property(label = "Publish Error Handling",
            description = "Enable handling of Edit-mode errors (PREVIEW and READONLY)",
            boolValue = DEFAULT_PUBLISH_ENABLED)
    public static final String PROP_PUBLISH_ENABLED = "prop.publish.enabled";

    private String publishErrorHTMLPath = DEFAULT_PUBLISH_ERROR_HTML_PATH;
    @Property(label = "Publish HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in DISABLED mode.",
            value = DEFAULT_PUBLISH_ERROR_HTML_PATH)
    public static final String PROP_PUBLISH_ERROR_HTML_PATH = "prop.publish.html";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public final void doFilter(ServletRequest request, ServletResponse response,
                               FilterChain chain) throws IOException, ServletException {
        if (!(request instanceof SlingHttpServletRequest)
                || !(response instanceof SlingHttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        final ComponentContext componentContext = WCMUtils.getComponentContext(request);

        if (componentContext == null || componentContext.isRoot()) {
            chain.doFilter(request, response);
        } else if (editModeEnabled
                && (componentHelper.isEditMode(slingRequest)
                || componentHelper.isDesignMode(slingRequest)
                || WCMMode.ANALYTICS.equals(WCMMode.fromRequest(slingRequest)))) {
            // Edit Modes
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, editErrorHTMLPath);
        } else if (previewModeEnabled
                && (componentHelper.isPreviewMode(slingRequest)
                || componentHelper.isReadOnlyMode(slingRequest))) {
            // Preview Modes
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, previewErrorHTMLPath);
        } else if (publishModeEnabled
                && componentHelper.isDisabledMode(slingRequest)) {
            // Publish Modes
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, publishErrorHTMLPath);
        } else {
            // Normal Behavior
            chain.doFilter(request, response);
        }
    }

    private void doFilterWithErrorHandling(final SlingHttpServletRequest slingRequest,
                                           final SlingHttpServletResponse slingResponse,
                                           final FilterChain chain,
                                           final String pathToHTML) throws IOException {

        log.debug("Including resource with ACS AEM Commons Component Level Error Handling for: {}",
                slingRequest.getResource().getPath());

        try {
            chain.doFilter(slingRequest, slingResponse);
        } catch (final ServletException ex) {
            this.handleError(slingResponse, slingRequest.getResource(), pathToHTML, ex);
        } catch (final SlingException ex) {
            this.handleError(slingResponse, slingRequest.getResource(), pathToHTML, ex);
        } catch (final Throwable ex) {
            this.handleError(slingResponse, slingRequest.getResource(), pathToHTML, ex);
        }
    }

    private void handleError(final SlingHttpServletResponse slingResponse, final Resource resource,
                                final String pathToHTML, final Throwable ex) throws IOException {
        // Log the error to the log files, so the exception is not lost
        this.logError(ex);

        // Write the custom "pretty" error message out to the response
        this.writeErrorHTML(slingResponse, resource, pathToHTML);
    }

    private void logError(final Throwable ex) {
        final StringWriter stringWriter = new StringWriter();
        ex.printStackTrace(new PrintWriter(stringWriter));
        log.error(stringWriter.toString());
    }

    private void writeErrorHTML(final SlingHttpServletResponse slingResponse, final Resource resource,
                                final String pathToHTML) throws IOException {
        log.info("ACS AEM Commons Component-Level Error Handling trapped error for: {}",
                resource.getPath());

        slingResponse.getWriter().print(this.getHTML(pathToHTML));
    }

    private String getHTML(final String path) {
        ResourceResolver resourceResolver = null;

        try {
            // Component error renditions are typically stored under /apps as part of the application; and thus
            // require elevated ACLs to work on Publish instances.
            // ONLY use this admin resource resolver to get the component error HTML and then immediately close.
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            return ResourceDataUtil.getNTFileAsString(path, resourceResolver);
        } catch (final Exception e) {
            log.error("Could not get the component error HTML at [ {} ], using blank.", path);
        } finally {
            if(resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return "";
    }

    @Override
    public final void destroy() {
        editModeEnabled = false;
        previewModeEnabled = false;
        publishModeEnabled = false;
    }

    @Activate
    public final void activate(final Map<String, String> config) {
        editModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_EDIT_ENABLED),
                DEFAULT_EDIT_ENABLED);
        previewModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_PREVIEW_ENABLED),
                DEFAULT_PREVIEW_ENABLED);
        publishModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_PUBLISH_ENABLED),
                DEFAULT_PUBLISH_ENABLED);

        editErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_EDIT_ERROR_HTML_PATH),
                DEFAULT_EDIT_ERROR_HTML_PATH);
        previewErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PREVIEW_ERROR_HTML_PATH),
                DEFAULT_PREVIEW_ERROR_HTML_PATH);
        publishErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PUBLISH_ERROR_HTML_PATH),
                DEFAULT_PUBLISH_ERROR_HTML_PATH);

        log.info("Component Error Handling for Edit Modes: {} ~> {}",
                editModeEnabled ? "Enabled" : "Disabled",
                editErrorHTMLPath);

        log.info("Component Error Handling for Preview Modes: {} ~> {}",
                previewModeEnabled ? "Enabled" : "Disabled",
                previewErrorHTMLPath);

        log.info("Component Error Handling for Publish Modes: {} ~> {}",
                publishModeEnabled ? "Enabled" : "Disabled",
                publishErrorHTMLPath);
    }
}