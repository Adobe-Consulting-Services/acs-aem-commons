/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.packaging.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPackagerServlet extends SlingAllMethodsServlet {

    @SuppressWarnings("PMD")
    protected final transient Logger log = LoggerFactory.getLogger(getClass());

    private static final String PACKAGE_NAME = "packageName";

    private static final String PACKAGE_GROUP_NAME = "packageGroupName";

    private static final String PACKAGE_VERSION = "packageVersion";

    private static final String PACKAGE_DESCRIPTION = "packageDescription";

    private static final String PACKAGE_ACL_HANDLING = "packageACLHandling";

    private static final String CONFLICT_RESOLUTION = "conflictResolution";

    private static final String INCLUDE_CONFIGURATION = "includeConfiguration";

    private static final String DEFAULT_PACKAGE_VERSION = "1.0.0";

    private static final boolean DEFAULT_INCLUDE_CONFIGURATION = false;

    /**
     * Gets the Packager Page resource.
     *
     * @param request the Sling HTTP Servlet Request object
     * @return a the PathFilterSet wrapping the cq:Page or null
     */
    protected PathFilterSet getPackagerPageResource(final SlingHttpServletRequest request) {
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(request.getResource());

        if (page != null) {
            return new PathFilterSet(page.getPath());
        }

        return null;
    }

    protected void doPackaging(SlingHttpServletRequest request, SlingHttpServletResponse response, boolean preview, ValueMap properties, List<PathFilterSet> packageResources) throws IOException, RepositoryException {
        // Add the ACL Packager Configuration page
        if (properties.get(INCLUDE_CONFIGURATION, DEFAULT_INCLUDE_CONFIGURATION)) {
            final PathFilterSet tmp = this.getPackagerPageResource(request);
            if (tmp != null) {
                packageResources.add(tmp);
            }
        }

        final Map<String, String> packageDefinitionProperties = new HashMap<String, String>();

        // ACL Handling
        packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                properties.get(PACKAGE_ACL_HANDLING, AccessControlHandling.OVERWRITE.toString()));

        // Package Description
        packageDefinitionProperties.put(
                JcrPackageDefinition.PN_DESCRIPTION,
                properties.get(PACKAGE_DESCRIPTION, getDefaultPackageDescription()));

        if (preview) {
            // Handle preview mode
            response.getWriter().print(getPackageHelper().getPathFilterSetPreviewJSON(packageResources));
        } else if (packageResources == null || packageResources.isEmpty()) {
            // Do not create empty packages; This will only clutter up CRX Package Manager
            response.getWriter().print(getPackageHelper().getErrorJSON("Refusing to create a package with no filter "
                    + "set rules."));
        } else {
            // Create JCR Package; Defaults should always be passed in via Request Parameters, but just in case
            try (final JcrPackage jcrPackage = getPackageHelper().createPackageFromPathFilterSets(packageResources,
                    request.getResourceResolver().adaptTo(Session.class),
                    properties.get(PACKAGE_GROUP_NAME, getDefaultPackageGroupName()),
                    properties.get(PACKAGE_NAME, getDefaultPackageName()),
                    properties.get(PACKAGE_VERSION, DEFAULT_PACKAGE_VERSION),
                    PackageHelper.ConflictResolution.valueOf(properties.get(CONFLICT_RESOLUTION,
                            PackageHelper.ConflictResolution.IncrementVersion.toString())),
                    packageDefinitionProperties
            )) {
                String thumbnailPath = getPackageThumbnailPath();

                if (thumbnailPath != null) {
                    // Add thumbnail to the package definition
                    getPackageHelper().addThumbnail(jcrPackage,
                            request.getResourceResolver().getResource(thumbnailPath));
                }

                log.debug("Successfully created JCR package");
                response.getWriter().print(
                        getPackageHelper().getSuccessJSON(jcrPackage));
            }
        }
    }

    /**
     * Gets the properties saved to the Asset Packager Page's jcr:content node.
     *
     * @param request The request obj
     * @return A ValueMap representing the properties
     */
    protected ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<>());
        } else {
            return request.getResource().getChild("configuration").getValueMap();
        }
    }

    protected abstract String getDefaultPackageDescription();

    protected abstract String getDefaultPackageGroupName();

    protected abstract String getDefaultPackageName();

    protected abstract String getPackageThumbnailPath();

    protected abstract PackageHelper getPackageHelper();
}
