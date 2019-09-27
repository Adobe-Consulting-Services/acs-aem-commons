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

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.wcm.api.Page;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.DamConstants;

import org.apache.commons.lang3.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.api.ProgressTrackerListener;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.util.DefaultProgressListener;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import java.util.Date;

/**
 * ACS AEM Commons - Instant Package Utility for quickly creating package from
 * Sites or Assets UI Console
 */
@SuppressWarnings("serial")
@SlingServlet(methods = { "POST" }, resourceTypes = {
        "acs-commons/components/utilities/instant-package" }, selectors = { "package" }, extensions = { "json" })
public class InstantPackageImpl extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(InstantPackageImpl.class);

    private static final String DEFAULT_PACKAGE_NAME = "instant-package-";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "Instant Package";

    private static final String DEFAULT_PACKAGE_VERSION = "1.0.0";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "Package has been created using ACS Commons Instant Package utility";

    private static final String JCR_CONTENT_APPEND = "/jcr:content";

    private static final String INSTANT_PACKAGE_THUMBNAIL_RESOURCE_PATH = "/apps/acs-commons/components/utilities/instant-package/thumbnail.png";

    @Reference
    private Packaging packaging;

    @Reference
    private PackageHelper packageHelper;

    @Override
    public final void doPost(final SlingHttpServletRequest request, final SlingHttpServletResponse response)
            throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();

        final String paths = request.getParameter("pathList");
        final String optionType = request.getParameter("optionType");

        if (StringUtils.isNotBlank(paths)) {

            try {

                List<Resource> packageResources = preparePackageResources(paths, optionType, resourceResolver);

                final Map<String, String> packageDefinitionProperties = new HashMap<>();

                // Package Description
                packageDefinitionProperties.put(JcrPackageDefinition.PN_DESCRIPTION, DEFAULT_PACKAGE_DESCRIPTION);

                if (packageResources.isEmpty()) {
                    // Do not create empty packages; This will only clutter up
                    // CRX Package Manager
                    response.getWriter().print(
                            packageHelper.getErrorJSON("Refusing to create a package with no filter " + "set rules."));
                } else {

                    // Create JCR Package; Defaults should always be passed in
                    // via Request Parameters, but just in case
                    final JcrPackage jcrPackage = packageHelper.createPackage(packageResources,
                            request.getResourceResolver().adaptTo(Session.class), DEFAULT_PACKAGE_GROUP_NAME,
                            DEFAULT_PACKAGE_NAME + new Date().getTime(), DEFAULT_PACKAGE_VERSION,
                            PackageHelper.ConflictResolution
                                    .valueOf(PackageHelper.ConflictResolution.IncrementVersion.toString()),
                            packageDefinitionProperties);

                    // Add thumbnail to the package definition
                    packageHelper.addThumbnail(jcrPackage,
                            request.getResourceResolver().getResource(INSTANT_PACKAGE_THUMBNAIL_RESOURCE_PATH));

                    ProgressTrackerListener listener = new DefaultProgressListener();
                    final JcrPackageManager jcrPackageManager = packaging
                            .getPackageManager(resourceResolver.adaptTo(Session.class));

                    // build package
                    jcrPackageManager.assemble(jcrPackage, listener);

                    log.debug("Successfully created and build JCR package");
                    response.getWriter().print(packageHelper.getSuccessJSON(jcrPackage));
                }
            } catch (PackageException ex) {
                log.error("Package Exception error while creating Instant Package", ex);
                response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
            } catch (RepositoryException ex) {
                log.error("Repository error while creating Instant Package", ex);
                response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
            } catch (IOException ex) {
                log.error("IO error while creating Instant Package", ex);
                response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
            }
        }

    }

    private List<Resource> preparePackageResources(String paths, String optionType, ResourceResolver resourceResolver) {
        String[] pathList = paths.split(",");
        ArrayList<Resource> allPaths = new ArrayList<>();
        List<Resource> packageResources;

        if ("selectedResource".equals(optionType)) {
            packageResources = getSelectedPath(resourceResolver, pathList, allPaths);

        } else if ("immediateChildren".equals(optionType)) {
            packageResources = getImmediateChildren(resourceResolver, pathList, allPaths);

        } else {
            // all children
            packageResources = getAllChildren(resourceResolver, pathList, allPaths);
        }
        return packageResources;
    }

    /**
     * Get the list of immediate children for the current selected path in sites
     * console
     * 
     * @param comma
     *            separated select paths from sites console
     * @param resource
     *            resolver object
     */
    private ArrayList<Resource> getImmediateChildren(ResourceResolver resourceResolver, String[] pathList,
            ArrayList<Resource> allPaths) {

        Iterator<Resource> childAssetRes;
        Iterator<Page> childPageRes;
        // iterate over all paths and create list with immediate children
        for (String path : pathList) {

            Resource currentPath = resourceResolver.getResource(path);
            if (path.startsWith("/content/dam")) {
                childAssetRes = currentPath.listChildren();
                getImmediateAssets(resourceResolver, childAssetRes, allPaths);

            } else {
                childPageRes = currentPath.adaptTo(Page.class).listChildren();
                getImmediatePages(resourceResolver, currentPath, childPageRes, allPaths);

            }

        }

        return allPaths;
    }

    /**
     * Get the list of immediate child pages
     * 
     * @param resourceResolver
     * @param path
     * @param childPageRes
     * @param allPaths
     * @return
     */
    private ArrayList<Resource> getImmediatePages(ResourceResolver resourceResolver, Resource pagePath,
            Iterator<Page> childPageRes, ArrayList<Resource> allPaths) {

        allPaths.add(resourceResolver.getResource(pagePath.getPath() + JCR_CONTENT_APPEND));
        while (childPageRes.hasNext()) {
            Resource res = resourceResolver.getResource(childPageRes.next().getPath() + JCR_CONTENT_APPEND);

            // ignore folder and add pages only
            if ("cq:PageContent".equals(res.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
                allPaths.add(res);
            }
        }

        return allPaths;
    }

    /**
     * Get the list of immediate assets
     * 
     * @param resourceResolver
     * @param path
     * @param childRes
     * @param allPaths
     * @return
     */
    private ArrayList<Resource> getImmediateAssets(ResourceResolver resourceResolver, Iterator<Resource> childRes,
            ArrayList<Resource> allPaths) {

        while (childRes.hasNext()) {
            Resource res = resourceResolver.getResource(childRes.next().getPath());

            // ignore folder and add assets only
            if (DamConstants.NT_DAM_ASSET.equals(res.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {
                allPaths.add(res);
            }
        }

        return allPaths;
    }

    /**
     * Get only selected path in sites console
     * 
     * @param comma
     *            separated select paths from sites console
     * @param resource
     *            resolver object
     */
    private ArrayList<Resource> getSelectedPath(ResourceResolver resourceResolver, String[] pathList,
            ArrayList<Resource> allPaths) {

        // only selected path by adding jcr:content at the end
        for (String path : pathList) {
            Resource currentPath = resourceResolver.getResource(path);
            allPaths.add(resourceResolver.getResource(currentPath.getPath() + JCR_CONTENT_APPEND));

        }

        return allPaths;
    }

    /**
     * Get all children of selected path in sites console
     * 
     * @param comma
     *            separated select paths from sites console
     * @param resource
     *            resolver object
     */
    private ArrayList<Resource> getAllChildren(ResourceResolver resourceResolver, String[] pathList,
            ArrayList<Resource> allPaths) {
        // all selected path with jcr:content to include all children
        for (String path : pathList) {
            Resource currentPath = resourceResolver.getResource(path);
            allPaths.add(resourceResolver.getResource(currentPath.getPath()));

        }

        return allPaths;
    }
}
