/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

/**
 * ACS AEM Commons - Asset Packager Servlet
 * Servlet end-point used to create packages of pages and the referenced assets.
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = {"POST"},
        resourceTypes = {"acs-commons/components/utilities/packager/asset-packager"},
        selectors = {"package"},
        extensions = {"json"}
)
public class AssetPackagerServletImpl extends AbstractPackagerServlet {

    private static final String PN_PAGE_PATH = "pagePath";

    private static final String PN_EXCLUDE_PAGES = "excludePages";

    private static final String DEFAULT_PACKAGE_NAME = "assets";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "Assets";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "Asset Package initially defined by a ACS AEM Commons - "
            + "Asset Packager configuration.";

    private static final String THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/asset-packager/definition/package-thumbnail.png";

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
            final String pagePath = properties.get(PN_PAGE_PATH, String.class);
            final List<PathFilterSet> paths;
            if (StringUtils.isBlank(pagePath)) {
                paths = Collections.emptyList();
            } else {
                paths = this.findAssetPaths(resourceResolver, pagePath);
            }
            if (!properties.get(PN_EXCLUDE_PAGES, false) && paths.size() > 0) {
                paths.add(0, new PathFilterSet(pagePath));
            }

            doPackaging(request, response, preview, properties, paths);
        } catch (RepositoryException ex) {
            log.error("Repository error while creating Asset Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error("IO error while creating Asset Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        }
    }

    /**
     * Gets the properties saved to the Asset Packager Page's jcr:content node.
     *
     * @param request the request obj
     * @return a ValueMap representing the properties
     */
    private ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("Asset Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<>());
        } else {
            return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
        }
    }

    /**
     * Recursively iterate over the parent path specified in the configuration to aggregate all the
     * String or String Array property values that are referencing a path in the DAM.
     *
     * @param resourceResolver Resolver from the current Request
     * @param pagePath The page path
     * @return The full list of filter sets
     */
    private List<PathFilterSet> findAssetPaths(final ResourceResolver resourceResolver,
                                               final String pagePath) {

        final List<PathFilterSet> filters = new ArrayList<>();
        final Resource parentResource = resourceResolver.resolve(pagePath);
        final ValueMap properties = parentResource.getValueMap();

        // Iterate over property map for Strings and String arrays and optionally add a filter
        for (String key : properties.keySet()) {
            final Object value = properties.get(key);
            if (value instanceof String) {
                addFilter(filters, (String) value, resourceResolver);
            } else if (value instanceof String[]) {
                final String[] arrayValue = (String[]) value;
                for (String stringValue : arrayValue) {
                    addFilter(filters, stringValue, resourceResolver);
                }
            }
        }

        // Recurse over child nodes and add the asset references found there.
        final Iterator<Resource> children = parentResource.listChildren();
        while (children.hasNext()) {
            final Resource child = children.next();
            filters.addAll(findAssetPaths(resourceResolver, child.getPath()));
        }

        return filters;
    }

    /**
     * Adds a property value to the filter set list if it is not empty, referencing the DAM, and is
     * an actual DAM Asset.
     *
     * @param filters The total list of filter sets
     * @param value The current property value
     */
    private void addFilter(final List<PathFilterSet> filters, final String value,
                           final ResourceResolver resourceResolver) {
        if (StringUtils.isNotBlank(value) && value.startsWith(DamConstants.MOUNTPOINT_ASSETS)
            && DamUtil.isAsset(resourceResolver.getResource(value))) {
            filters.add(new PathFilterSet(value));
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
