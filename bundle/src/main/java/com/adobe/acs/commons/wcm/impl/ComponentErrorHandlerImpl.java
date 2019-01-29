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

package com.adobe.acs.commons.wcm.impl;

import static org.apache.sling.engine.EngineConstants.FILTER_SCOPE_COMPONENT;
import static org.apache.sling.engine.EngineConstants.SLING_FILTER_SCOPE;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ModeUtil;
import com.adobe.acs.commons.util.ResourceDataUtil;
import com.adobe.acs.commons.wcm.ComponentErrorHandler;
import com.adobe.acs.commons.wcm.ComponentHelper;
import com.day.cq.wcm.api.components.ComponentContext;
import com.day.cq.wcm.commons.WCMUtils;

@Component(
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        service = {
                ComponentErrorHandler.class,
                Filter.class},
        property = {
                SLING_FILTER_SCOPE + "=" + FILTER_SCOPE_COMPONENT,
                "filter.order" + ":Integer=" + ComponentErrorHandlerImpl.FILTER_ORDER
        }
)
@Designate(
        ocd = ComponentErrorHandlerImpl.Config.class
)
public class ComponentErrorHandlerImpl implements ComponentErrorHandler, Filter{
    private static final Logger log = LoggerFactory.getLogger(ComponentErrorHandlerImpl.class.getName());

    // Magic number pushes filter lower in the chain so it executes after the OOTB WCM Debug Filter
    // In AEM6 this must execute after WCM Developer Mode Filter which requires overriding the service.ranking via a
    // sling:OsgiConfig node
    static final int FILTER_ORDER = 1000000;

    static final String BLANK_HTML = "/dev/null";

    static final String REQ_ATTR_PREVIOUSLY_PROCESSED =
            ComponentErrorHandlerImpl.class.getName() + "_previouslyProcessed";


    private static final String SERVICE_NAME = "component-error-handler";
    private static final Map<String, Object> AUTH_INFO;
    private static final String DISABLED = "Disabled";
    private static final String ENABLED = "Enabled";

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ComponentHelper componentHelper;
    
    @Reference
    private ModeUtil modeHelper;
   

    /* Edit Mode */

    private static final boolean DEFAULT_EDIT_ENABLED = true;

    private boolean editModeEnabled = DEFAULT_EDIT_ENABLED;

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Component-Level Error Handler",
            description = "Handles errors at the component level. Allows different HTML renditions to display for erring "
                    + "components based on WCM Mode collections (Edit, Preview, Publish)."
    )
    public @interface Config {

        @AttributeDefinition(
                name = "Edit Error Handling",
                description = "Enable handling of Edit-mode errors (EDIT, DESIGN, ANALYTICS)",
                defaultValue = "" + DEFAULT_EDIT_ENABLED
        )
        boolean edit_enabled();

        @AttributeDefinition(
                name = "Edit HTML Error Path",
                description = "Path to html file in JCR use to display an erring component in EDIT or DESIGN modes.",
                defaultValue = DEFAULT_EDIT_ERROR_HTML_PATH
        )
        String edit_html();

        @AttributeDefinition(
                name = "Preview Error Handling",
                description = "Enable handling of Edit-mode errors (PREVIEW and READ_ONLY)",
                defaultValue = "" + DEFAULT_PREVIEW_ENABLED
        )
        boolean preview_enabled();

        @AttributeDefinition(
                name = "Preview HTML Error Path",
                description = "Path to html file in JCR use to display an erring component in PREVIEW or READONLY modes.",
                defaultValue = DEFAULT_PREVIEW_ERROR_HTML_PATH
        )
        String preview_html();

        @AttributeDefinition(
                name = "Publish Error Handling",
                description = "Enable handling of Edit-mode errors (PREVIEW and READONLY)",
                defaultValue = "" + DEFAULT_PUBLISH_ENABLED
        )
        boolean publish_enabled();

        @AttributeDefinition(
                name = "Suppressed Resource Types",
                description = "Resource types this Filter will ignore during Sling Includes.",
                cardinality = Integer.MAX_VALUE
        )
        String[] suppress$_$resource$_$types();
    }
    
    public static final String PROP_EDIT_ENABLED = "edit.enabled";

    private static final String DEFAULT_EDIT_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/component-error-handler/edit.html";

    private String editErrorHTMLPath = DEFAULT_EDIT_ERROR_HTML_PATH;

    public static final String PROP_EDIT_ERROR_HTML_PATH = "edit.html";

    /* Preview Mode */

    private static final boolean DEFAULT_PREVIEW_ENABLED = false;

    private boolean previewModeEnabled = DEFAULT_PREVIEW_ENABLED;

    public static final String PROP_PREVIEW_ENABLED = "preview.enabled";

    private static final String DEFAULT_PREVIEW_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/component-error-handler/preview.html";

    private String previewErrorHTMLPath = DEFAULT_PREVIEW_ERROR_HTML_PATH;

    public static final String PROP_PREVIEW_ERROR_HTML_PATH = "preview.html";

    /* Publish Mode */

    private static final boolean DEFAULT_PUBLISH_ENABLED = false;

    private boolean publishModeEnabled = DEFAULT_PUBLISH_ENABLED;

    public static final String PROP_PUBLISH_ENABLED = "publish.enabled";

    private static final String DEFAULT_PUBLISH_ERROR_HTML_PATH = BLANK_HTML;

    private String publishErrorHTMLPath = DEFAULT_PUBLISH_ERROR_HTML_PATH;

    public static final String PROP_PUBLISH_ERROR_HTML_PATH = "publish.html";

    /* Suppressed Resource Types */

    private static final String[] DEFAULT_SUPPRESSED_RESOURCE_TYPES = new String[]{};

    private String[] suppressedResourceTypes = DEFAULT_SUPPRESSED_RESOURCE_TYPES;

    public static final String PROP_SUPPRESSED_RESOURCE_TYPES = "suppress-resource-types";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        // no-op
    }

    @Override
    public final void doFilter(ServletRequest servletRequest, ServletResponse servletResponse,
                               FilterChain chain) throws IOException, ServletException {

        // We are in a Sling Filter, so these request/response objects are guaranteed to be of type Sling...
        final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
        final SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;

        if (!this.accepts(request, response)) {
            chain.doFilter(request, response);
            return;
        }

        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;

        if (editModeEnabled
                && (modeHelper.isEdit(request)
                || modeHelper.isDesign(request)
                || ModeUtil.isAnalytics(slingRequest))) {
            // Edit Modes
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, editErrorHTMLPath);
        } else if (previewModeEnabled
                && (modeHelper.isPreview(request)
                || modeHelper.isReadOnly(request))) {
            // Preview Modes
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, previewErrorHTMLPath);
        } else if (publishModeEnabled
                && modeHelper.isDisabled(request)
                && !this.isFirstInChain(slingRequest)) {
            // Publish Modes; Requires special handling in Published Modes - do not process first filter chain
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, publishErrorHTMLPath);
        } else {
            // Normal Behavior
            chain.doFilter(request, response);
        }
    }

    private void doFilterWithErrorHandling(final SlingHttpServletRequest slingRequest,
                                           final SlingHttpServletResponse slingResponse,
                                           final FilterChain chain,
                                           final String pathToHTML) throws ServletException, IOException {

        final boolean suppress = this.isComponentErrorHandlingSuppressed(slingRequest);

        if (suppress) {
            log.debug("Suppressing component error handling for: {}", slingRequest.getResource().getPath());
        }

        try {
            chain.doFilter(slingRequest, slingResponse);
        } catch (final Exception ex) {
            if (this.isComponentErrorHandlingSuppressed(slingRequest)) {
                // Allows disabling from within an inclusion.
                // This is checked before the suppression is reset to the "pre-inclusion" state
                log.debug("Suppressed component error handling for: {}",
                        slingRequest.getResource().getPath());

                throw new ServletException(ex);
            } else {
                // Handle error using the Component Error Handler HTML
                this.handleError(slingResponse, slingRequest.getResource(), pathToHTML, ex);
            }
        } finally {
            // Re/set component error handling suppression to its pre-include state.
            if (suppress) {
                // Continue suppressing future includes even if turned off from WITHIN the inclusion chain
                this.suppressComponentErrorHandling(slingRequest);
            } else if (this.isComponentErrorHandlingSuppressed(slingRequest)) {
                // If suppression was set from WITHIN the inclusion chain, turn it off
                log.debug("Removing suppression component error handling at: {}",
                        slingRequest.getResource().getPath());

                this.allowComponentErrorHandling(slingRequest);
            }
        }
    }

    private void handleError(final SlingHttpServletResponse slingResponse, final Resource resource,
                             final String pathToHTML, final Throwable ex) throws IOException {
        // Log the error to the log files, so the exception is not lost
        log.error(ex.getMessage(), ex);

        // Write the custom "pretty" error message out to the response
        this.writeErrorHTML(slingResponse, resource, pathToHTML);
    }

    private void writeErrorHTML(final SlingHttpServletResponse slingResponse, final Resource resource,
                                final String pathToHTML) throws IOException {
        log.info("ACS AEM Commons Component-Level Error Handling trapped error for: {}",
                resource.getPath());

        slingResponse.getWriter().print(this.getHTML(pathToHTML));
    }

    private String getHTML(final String path) {
        // Handle blank HTML conditions first; Avoid looking in JCR for them.
        if (StringUtils.isBlank(path) || StringUtils.equals(BLANK_HTML, path)) {
            return "";
        }

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){
            // Component error renditions are typically stored under /apps as part of the application; and thus
            // requires elevated ACLs to work on Publish instances.

            return ResourceDataUtil.getNTFileAsString(path, resourceResolver);
        } catch (final Exception e) {
            log.error("Could not get the component error HTML at [ {} ], using blank.", path);
        }

        return "";
    }

    protected final boolean accepts(final SlingHttpServletRequest request, final SlingHttpServletResponse response) {

        if (!StringUtils.endsWith(request.getRequestURI(), ".html")
                || !StringUtils.contains(response.getContentType(), "html")) {
            // Do not inject around non-HTML requests
            return false;
        }

        final ComponentContext componentContext = WCMUtils.getComponentContext(request);
        if (componentContext == null // ComponentContext is null
                || componentContext.getComponent() == null // Component is null
                || componentContext.isRoot()) { // Suppress on root context
            return false;
        }

        // Check to make sure the suppress key has not been added to the request
        if (this.isComponentErrorHandlingSuppressed(request)) {
            // Suppress key is detected, skip handling

            return false;
        }

        // Check to make sure the SlingRequest's resource isn't in the suppress list
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;
        for (final String suppressedResourceType : suppressedResourceTypes) {
            if (slingRequest.getResource().isResourceType(suppressedResourceType)) {
                return false;
            }
        }

        return true;
    }

    private boolean isFirstInChain(final SlingHttpServletRequest request) {
        if (request.getAttribute(REQ_ATTR_PREVIOUSLY_PROCESSED) != null) {
            return false;
        } else {
            request.setAttribute(REQ_ATTR_PREVIOUSLY_PROCESSED, true);
            return true;
        }
    }

    @Override
    public final void destroy() {
        editModeEnabled = false;
        previewModeEnabled = false;
        publishModeEnabled = false;
    }

    @Activate
    public final void activate(final Map<String, String> config) {
        final String legacyPrefix = "prop.";

        editModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_EDIT_ENABLED),
                PropertiesUtil.toBoolean(config.get(legacyPrefix + PROP_EDIT_ENABLED),
                        DEFAULT_EDIT_ENABLED));

        previewModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_PREVIEW_ENABLED),
                PropertiesUtil.toBoolean(config.get(legacyPrefix + PROP_PREVIEW_ENABLED),
                        DEFAULT_PREVIEW_ENABLED));

        publishModeEnabled = PropertiesUtil.toBoolean(config.get(PROP_PUBLISH_ENABLED),
                PropertiesUtil.toBoolean(config.get(legacyPrefix + PROP_PUBLISH_ENABLED),
                        DEFAULT_PUBLISH_ENABLED));


        editErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_EDIT_ERROR_HTML_PATH),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_EDIT_ERROR_HTML_PATH),
                        DEFAULT_EDIT_ERROR_HTML_PATH));

        previewErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PREVIEW_ERROR_HTML_PATH),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_PREVIEW_ERROR_HTML_PATH),
                        DEFAULT_PREVIEW_ERROR_HTML_PATH));

        publishErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PUBLISH_ERROR_HTML_PATH),
                PropertiesUtil.toString(config.get(legacyPrefix + PROP_PUBLISH_ERROR_HTML_PATH),
                        DEFAULT_PUBLISH_ERROR_HTML_PATH));


        log.info("Component Error Handling for Edit Modes: {} ~> {}",
                editModeEnabled ? ENABLED : DISABLED,
                editErrorHTMLPath);

        log.info("Component Error Handling for Preview Modes: {} ~> {}",
                previewModeEnabled ? ENABLED : DISABLED,
                previewErrorHTMLPath);

        log.info("Component Error Handling for Publish Modes: {} ~> {}",
                publishModeEnabled ? ENABLED : DISABLED,
                publishErrorHTMLPath);

        suppressedResourceTypes = PropertiesUtil.toStringArray(config.get(PROP_SUPPRESSED_RESOURCE_TYPES),
                DEFAULT_SUPPRESSED_RESOURCE_TYPES);

        log.info("Suppressed Resource Types: {}", Arrays.toString(suppressedResourceTypes));
    }

    @Override
    public final void suppressComponentErrorHandling(final SlingHttpServletRequest request) {
        request.setAttribute(SUPPRESS_ATTR, true);
    }

    @Override
    public final void allowComponentErrorHandling(final SlingHttpServletRequest request) {
        request.removeAttribute(SUPPRESS_ATTR);
    }

    private boolean isComponentErrorHandlingSuppressed(final ServletRequest request) {
        final Boolean suppress = (Boolean) request.getAttribute(SUPPRESS_ATTR);

        if (suppress != null) {
            return suppress.booleanValue();
        } else {
            return false;
        }
    }
}