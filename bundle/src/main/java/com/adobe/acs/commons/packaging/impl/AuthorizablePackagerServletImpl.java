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

package com.adobe.acs.commons.packaging.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.jcr.RepositoryException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.acs.commons.packaging.PackageHelper;

/**
 * ACS AEM Commons - Authorizable Packager Servlet
 * Servlet end-point used to create CRX packages of authorizables based on the underlying resource's configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/packager/authorizable-packager" },
        selectors = { "package" },
        extensions = { "json" }
)
public class AuthorizablePackagerServletImpl extends AbstractPackagerServlet {

    private static final String DEFAULT_PACKAGE_NAME = "authorizables";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "Authorizables";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "Authorizable Package initially defined by a ACS AEM Commons - "
            + "Authorizable Packager configuration.";

    private static final String PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/authorizable-packager/definition/package-thumbnail.png";

    @Reference
    private Packaging packaging;

    @Reference
    private PackageHelper packageHelper;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.debug("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);

        try {
            final List<PathFilterSet> paths = this.findPaths(resourceResolver,
                    properties.get("authorizableIds", new String[0]));

            doPackaging(request, response, preview, properties, paths);
        } catch (RepositoryException ex) {
            log.error("Repository error while creating Query Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error("IO error while creating Query Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        }
    }

    /**
     * Gets the properties saved to the Query Packager Page's jcr:content node.
     *
     * @param request the request obj
     * @return a ValueMap representing the properties
     */
    private ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("Authorizable Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<String, Object>());
        } else {
            return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
        }
    }

    private List<PathFilterSet> findPaths(final ResourceResolver resourceResolver,
                                         final String[] authorizableIds) throws RepositoryException {

        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final String authorizableId : authorizableIds) {
            try {
                final Authorizable authorizable = userManager.getAuthorizable(authorizableId);
                if (authorizable != null) {
                    final String path = authorizable.getPath();
                    final PathFilterSet principal = new PathFilterSet(path);
                    // Exclude tokens as they are not vlt installable in AEM6/Oak
                    principal.addExclude(new DefaultPathFilter(path + "/\\.tokens"));
                    pathFilterSets.add(principal);
                }
            } catch (RepositoryException e) {
                log.warn("Unable to find path for authorizable " + authorizableId, e);
            }
        }

        return pathFilterSets;
    }

    @Override
    protected String getDefaultPackageDescription() {
        return DEFAULT_PACKAGE_DESCRIPTION;
    }

    @Override
    protected String getDefaultPackageGroupName() {
        return DEFAULT_PACKAGE_GROUP_NAME;
    }

    @Override
    protected String getDefaultPackageName() {
        return DEFAULT_PACKAGE_NAME;
    }

    @Override
    protected String getPackageThumbnailPath() {
        return PACKAGE_THUMBNAIL_RESOURCE_PATH;
    }

    @Override
    protected PackageHelper getPackageHelper() {
        return packageHelper;
    }
}
