/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

package com.adobe.acs.commons.dam.impl;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.api.wrappers.CompositeValueMap;
import org.apache.sling.api.wrappers.SlingHttpServletRequestWrapper;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.apache.sling.servlets.post.AbstractPostResponse;
import org.apache.sling.servlets.post.Modification;
import org.apache.sling.servlets.post.PostOperation;
import org.apache.sling.servlets.post.SlingPostProcessor;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

@Component(configurationPolicy=ConfigurationPolicy.REQUIRE,properties= {
        "service.ranking:Integer=-2000",
        "sling.filter.scope=REQUEST",
        "sling.filter.pattern=/content/dam/.*",
        "sling.servlet.methods=GET",
        "sling.servlet.resourceTypes=acs-commons/touchui-widgets/asset-folder-properties-support"
})

public class AssetsFolderPropertiesSupport extends SlingSafeMethodsServlet implements Filter, SlingPostProcessor {
    private static final Logger log = LoggerFactory.getLogger(AssetsFolderPropertiesSupport.class);

    private static final String DAM_PATH_PREFIX = "/content/dam";
    private static final String POST_METHOD = "post";
    private static final String OPERATION = ":operation";
    private static final String DAM_FOLDER_SHARE_OPERATION = "dam.share.folder";
    private static final String GRANITE_UI_FORM_VALUES = "granite.ui.form.values";

    /**
     * The is a reference to the OOTB AEM PostOperation that handles updates for Folder Properties; This is used below in process(..) 
     * to ensure that all OOTB behaviors are executed.
     * NOTE: When switching to OSGI annotations, the original annotation caused validation errors; I had to put
     * parenthesis ("(" and ")" characters) around the whole expression to make it pass validation. You might
     * want to recheck this!
     */
    @Reference(target="(&(sling.post.operation=dam.share.folder)(sling.servlet.methods=POST))")
    private PostOperation folderShareHandler;

    /**
     * This method is responsible for post processing POSTs to the FolderShareHandler PostOperation (:operation = dam.share.folder).
     * This method will store a whitelisted set of request parameters to their relative location off of the [sling:*Folder] node.
     *
     * Note, this is executed AFTER the OOTB FolderShareHandler PostOperation.
     *
     * At this time this method only supports single-value Strings and ignores all @typeHints.
     *
     * This method must fail fast via the accepts(...) method.
     *
     * @param servletRequest the request object
     * @param servletResponse the response object
     * @param chain the filter chain
     * @throws IOException
     * @throws ServletException
     */
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain chain) throws IOException, ServletException {
        final SlingHttpServletRequest request = (SlingHttpServletRequest) servletRequest;
        final SlingHttpServletResponse response = (SlingHttpServletResponse) servletResponse;

        if (!accepts(request)) {
            chain.doFilter(request, response);
            return;
        }

        log.trace("ACS AEM Commons Assets Folder Properties Support applied to POST Request");
        chain.doFilter(new AssetsFolderPropertiesSupportRequest(request, null), response);
    }

    public void init(FilterConfig filterConfig) throws ServletException {
        // Do Nothing
    }

    public void destroy() {
        // Do Nothing
    }

    public void process(SlingHttpServletRequest request, List<Modification> changes) throws Exception {
        if (AssetsFolderPropertiesSupportRequest.isMarked(request)) {
            log.trace("Sending the the wrapped dam.folder.share request to the AEM Assets dam.folder.share PostOperation for final processing");

            final AssetsFolderPropertiesSupportRequest wrappedRequest = new AssetsFolderPropertiesSupportRequest(request, DAM_FOLDER_SHARE_OPERATION);

            folderShareHandler.run(wrappedRequest, new DummyPostResponse(), new SlingPostProcessor[]{});

            log.trace("Processed the the wrapped dam.folder.share request with the AEM Assets dam.folder.share PostOperation");
        }
    }

    /**
     * Gateway method the Filter uses to determine if the request is a candidate for processing by Assets Folder Properties Support.
     * These checks should be fast and fail broadest and fastest first.
     *
     * @param request the request
     * @return true if Assets Folder Properties Support should process this request.
     */
    @SuppressWarnings("squid:S3923")
    protected boolean accepts(SlingHttpServletRequest request) {
        if (!StringUtils.equalsIgnoreCase(POST_METHOD, request.getMethod())) {
            // Only POST methods are processed
            return false;
        } else if (!DAM_FOLDER_SHARE_OPERATION.equals(request.getParameter(OPERATION))) {
            // Only requests with :operation=dam.share.folder are processed
            return false;
        } else if (!StringUtils.startsWith(request.getResource().getPath(), DAM_PATH_PREFIX)) {
            // Only requests under /content/dam are processed
            return false;
        } else if (!request.getResource().isResourceType(JcrResourceConstants.NT_SLING_FOLDER)
                && !request.getResource().isResourceType(JcrResourceConstants.NT_SLING_ORDERED_FOLDER)) {
            // Only requests to sling:Folders or sling:Ordered folders are processed
            return false;
        }

        // If the above checks do not fail, treat as a valid request
        return true;
    }

    /**
     * This method handles the READING of the properties so that granite UI widgets can display stored data in the form.
     * This needs to be included AFTER /apps/dam/gui/content/assets/foldersharewizard/jcr:content/body/items/form/items/wizard/items/settingStep/items/fixedColumns/items/fixedColumn2/items/tabs/items/tab1/items/folderproperties
     * such that it can augment the Property map constructed by that OOTB script.
     *
     * Note that this exposes a value map for the [sling:*Folder] node, and NOT the [sling:*Folder]/jcr:content, so properties must be prefixed with jcr:content/...
     *
     * This can be achieved by creating a resource merge:
     * /apps/dam/gui/content/assets/foldersharewizard/jcr:content/body/items/form/items/wizard/items/settingStep/items/fixedColumns/items/fixedColumn2/items/tabs/items/tab1/items/folderproperties/assets-folder-properties-support@sling:resourceType = acs-commons/touchui-widgets/asset-folder-properties-support
     * /apps/dam/gui/content/assets/foldersharewizard/jcr:content/body/items/form/items/wizard/items/settingStep/items/fixedColumns/items/fixedColumn2/items/tabs/items/tab1/items/folderproperties/assets-folder-properties-support@sling:orderBefore = titlefield
     *
     * @param request the request object
     * @param response the response object
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {
        final Resource suffixResource = request.getResourceResolver().resolve(request.getRequestPathInfo().getSuffix());
        if (suffixResource == null) { return; }

        log.trace("AssetsFolderPropertiesSupport GET method for folder resource [ {} ]", suffixResource.getPath());

        ValueMap formProperties = (ValueMap) request.getAttribute(GRANITE_UI_FORM_VALUES);

        if (formProperties == null) {
            formProperties = new ValueMapDecorator(new HashMap<String, Object>());
        }

        request.setAttribute(GRANITE_UI_FORM_VALUES, new CompositeValueMap(formProperties, suffixResource.getValueMap(), true));
    }

    /** Synthetic Objects to allow this request to traverse the request processing stack **/

    protected class DummyPostResponse extends AbstractPostResponse {
        protected void doSend(HttpServletResponse response) throws IOException {
            // Do nothing
        }

        public void onChange(String type, String... arguments) {
            // Do nothing
        }
    }


    /**
     * Sling HTTP Request wrapper that masks the :operation so the default Sling POST Servlet can be invoked.
     */
    protected static class AssetsFolderPropertiesSupportRequest extends SlingHttpServletRequestWrapper {
        private static  String REQUEST_ATTR_KEY = AssetsFolderPropertiesSupportRequest.class.getName();

        private String operationValue;

        public AssetsFolderPropertiesSupportRequest(SlingHttpServletRequest request, String operationValue) {
            super(request);
            markRequest(request);
            this.operationValue = operationValue;
        }

        public static void markRequest(SlingHttpServletRequest request) {
            request.setAttribute(REQUEST_ATTR_KEY, true);
        }

        protected static boolean isMarked(SlingHttpServletRequest request) {
            return request.getAttribute(REQUEST_ATTR_KEY) != null;
        }

        public String getParameter(String key) {
            if (isMarked(this) && OPERATION.equals(key)) {
                return operationValue;
            } else {
                return super.getParameter(key);
            }
        }
    }
}