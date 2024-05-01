/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.packaging.impl;

import java.io.IOException;

import javax.jcr.RepositoryException;
import javax.servlet.Servlet;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.packaging.util.AssetPackageUtil;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.HttpConstants;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import static org.apache.sling.api.servlets.ServletResolverConstants.*;

/**
 * ACS AEM Commons - Asset Packager Servlet
 * Servlet end-point used to create packages of pages and the referenced assets.
 */
@SuppressWarnings("serial")
@Component(
        service = {Servlet.class},
        property = {
                SLING_SERVLET_RESOURCE_TYPES + "=acs-commons/components/utilities/packager/asset-packager",
                SLING_SERVLET_METHODS + "=" + HttpConstants.METHOD_POST,
                SLING_SERVLET_EXTENSIONS + "=json",
                SLING_SERVLET_SELECTORS + "=package",
        }
)
public class AssetPackagerServletImpl extends AbstractPackagerServlet {

    /* Default Properties */
    private static final String DEFAULT_PACKAGE_NAME = "assets";
    private static final String DEFAULT_PACKAGE_GROUP_NAME = "Assets";
    private static final String DEFAULT_PACKAGE_DESCRIPTION = "Asset Package initially defined by a ACS AEM Commons - "
            + "Asset Packager configuration.";
    private static final String THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/asset-packager/definition/package-thumbnail.png";

    @Reference
    private transient PackageHelper packageHelper;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.debug("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);

        // Instantiate AssetPackageUtil class to run the search in a threadsafe manner
        AssetPackageUtil assetPackageUtil = new AssetPackageUtil(properties, resourceResolver);

        try {
            doPackaging(request, response, preview, properties, assetPackageUtil.getPackageFilterPaths());
        } catch (RepositoryException ex) {
            log.error("Repository error while creating Asset Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error("IO error while creating Asset Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        }
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
        return THUMBNAIL_RESOURCE_PATH;
    }

    @Override
    protected PackageHelper getPackageHelper() {
        return packageHelper;
    }
}
