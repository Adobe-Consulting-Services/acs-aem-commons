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
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingFilter;
import org.apache.felix.scr.annotations.sling.SlingFilterScope;
import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import java.util.Map;

@SlingFilter(
        label = "ACS AEM Commons - Component Error Handler Filter",
        description = "Handles errors at the component level. Allows different HTML renditions to display for erring "
                + "components based on WCM Modes (edit, preview, publish).",
        metatype = false,
        generateComponent = true,
        generateService = true,
        order = 1001,
        scope = SlingFilterScope.COMPONENT)
public class ComponentErrorFilterImpl implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ComponentErrorFilterImpl.class.getName());

    @Reference
    private ComponentHelper componentHelper;

    private static final String DEFAULT_EDIT_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/edit.html";
    private String editErrorHTMLPath = DEFAULT_EDIT_ERROR_HTML_PATH;
    @Property(label = "Edit HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in EDIT or DESIGN modes.",
            value = DEFAULT_EDIT_ERROR_HTML_PATH)
    public static final String PROP_EDIT_ERROR_HTML_PATH = "prop.edit-error-html-path";


    private static final String DEFAULT_PREVIEW_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/preview.html";
    private String previewErrorHTMLPath = DEFAULT_PREVIEW_ERROR_HTML_PATH;
    @Property(label = "Preview HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in PREVIEW or READONLY modes.",
            value = DEFAULT_PREVIEW_ERROR_HTML_PATH)
    public static final String PROP_PREVIEW_ERROR_HTML_PATH = "prop.preview-error-html-path";

    private static final String DEFAULT_PUBLISH_ERROR_HTML_PATH =
            "/apps/acs-commons/components/utilities/errorpagehandler/components/publish.html";

    private String publishErrorHTMLPath = DEFAULT_PUBLISH_ERROR_HTML_PATH;
    @Property(label = "Publish HTML Error Path",
            description = "Path to html file in JCR use to display an erring component in DISABLED mode.",
            value = DEFAULT_PUBLISH_ERROR_HTML_PATH)
    public static final String PROP_PUBLISH_ERROR_HTML_PATH = "prop.publish-error-html-path";


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {

        if (!(request instanceof SlingHttpServletRequest) ||
                !(response instanceof SlingHttpServletResponse)) {
            chain.doFilter(request, response);
            return;
        }

        final SlingHttpServletResponse slingResponse = (SlingHttpServletResponse) response;
        final SlingHttpServletRequest slingRequest = (SlingHttpServletRequest) request;

        final ComponentContext componentContext = WCMUtils.getComponentContext(request);
        final Resource resource = slingRequest.getResource();

        if (componentContext == null || componentContext.isRoot()) {
            log.debug("ComponentContext is null or isRoot for: {}", resource.getPath());
            chain.doFilter(request, response);
        } else if (componentHelper.isAuthoringMode(slingRequest)) {
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, editErrorHTMLPath);
        } else if (componentHelper.isPreviewMode(slingRequest)
                || componentHelper.isReadOnlyMode(slingRequest)) {
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, previewErrorHTMLPath);
        } else {
            this.doFilterWithErrorHandling(slingRequest, slingResponse, chain, publishErrorHTMLPath);
        }
    }

    private void doFilterWithErrorHandling(final SlingHttpServletRequest slingRequest,
                                           final SlingHttpServletResponse slingResponse,
                                           final FilterChain chain,
                                           final String pathToHTML) throws IOException {

        final ResourceResolver resourceResolver = slingRequest.getResourceResolver();

        if(log.isDebugEnabled()) {
            final WCMMode mode = WCMMode.fromRequest(slingRequest);
            final Resource resource = slingRequest.getResource();

            log.debug("Component error for [ {} ] under {} mode.", resource.getPath(), mode.name());
        }

        try {
            chain.doFilter(slingRequest, slingResponse);
        } catch (ServletException e) {
            this.writeErrorHTML(slingResponse, resourceResolver, pathToHTML);
        } catch (SlingException e) {
            this.writeErrorHTML(slingResponse, resourceResolver, pathToHTML);
        } catch (Throwable e) {
            this.writeErrorHTML(slingResponse, resourceResolver, pathToHTML);
        }
    }

    private void writeErrorHTML(final SlingHttpServletResponse slingResponse, final ResourceResolver resourceResolver,
                                final String pathToHTML) throws IOException {
        slingResponse.getWriter().print(this.getHTML(resourceResolver, pathToHTML));
    }

    private String getHTML(final ResourceResolver resourceResolver, final String path) {
        try {
            return ResourceDataUtil.getNTFileAsString(path, resourceResolver);
        } catch (Exception e) {
            log.error("Could not get the component error HTML at [ {} ], using blank.", path);
        }

        return "";
    }

    @Override
    public void destroy() {
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        editErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_EDIT_ERROR_HTML_PATH), DEFAULT_EDIT_ERROR_HTML_PATH);
        previewErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PREVIEW_ERROR_HTML_PATH), DEFAULT_PREVIEW_ERROR_HTML_PATH);
        publishErrorHTMLPath = PropertiesUtil.toString(config.get(PROP_PUBLISH_ERROR_HTML_PATH), DEFAULT_PUBLISH_ERROR_HTML_PATH);
    }
}