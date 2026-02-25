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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.NameConstants;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.redirects.models.Configurations.REDIRECTS_RESOURCE_TYPE;


/**
 * A servlet to replicate redirect rules as a package
 */
@Component(service = Servlet.class, immediate = true, property = {
        "sling.servlet.label=ACS AEM Commons - Replicate Redirects Servlet",
        "sling.servlet.methods=POST",
        "sling.servlet.extensions=json",
        "sling.servlet.selectors=replicate",
        "sling.servlet.resourceTypes=" + REDIRECTS_RESOURCE_TYPE
})
public class ReplicateRedirectMapServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(ReplicateRedirectMapServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;

    static final String PACKAGE_GROUP = "com.adobe.acs.commons.redirects";
    static final String PACKAGE_VERSION = "1.0";
    static final String PACKAGE_THUMBNAIL_RESOURCE_PATH = "/apps/acs-commons/components/utilities/manage-redirects/thumbnail.png";
    static final String SERVICE_USER = "redirects-package-replicator";

    private final transient Replicator replicator;
    private final transient PackageHelper packageHelper;
    private final transient Packaging packaging;
    private final transient ResourceResolverFactory resourceResolverFactory;

    @Activate
    public ReplicateRedirectMapServlet(@Reference Replicator replicator,
                                       @Reference PackageHelper packageHelper,
                                       @Reference Packaging packaging,
                                       @Reference ResourceResolverFactory resourceResolverFactory) {
        this.replicator = replicator;
        this.packageHelper = packageHelper;
        this.packaging = packaging;
        this.resourceResolverFactory = resourceResolverFactory;
    }

    @Override
    protected void doPost(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse)
            throws ServletException, IOException {

        PostResponse postResponse = new JSONResponse();
        String redirectsParentPath = slingRequest.getResource().getPath();
        postResponse.setPath(redirectsParentPath);
        log.debug("replicating {}", redirectsParentPath);
        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(
                Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, SERVICE_USER))) {

            String packagePath = createPackage(resourceResolver, redirectsParentPath);
            Session session = resourceResolver.adaptTo(Session.class);
            replicator.replicate(session, ReplicationActionType.ACTIVATE, packagePath);
            updateReplicationStatusWithCurrentUser(slingRequest.getResource());

            postResponse.setTitle("Redirects replicated: " + redirectsParentPath);
        } catch (IOException | ReplicationException | RepositoryException | PackageException e) {
            log.error("failed to replicate redirects", e);
            postResponse.setError(e);
        } catch (LoginException e) {
            log.error("Could not get resource resolver", e);
            postResponse.setError(e);
        }
        postResponse.send(slingResponse, true);
    }

    /**
     * create package name from the path, e.g.
     * <p>
     * /conf/global/settings/redirects ==> conf.global.settings.redirects
     */
    String createPackageName(String path) {
        return Arrays.stream(path.substring(1).split("/"))
                .map(JcrUtil::createValidName)
                .collect(Collectors.joining("."));

    }

    /**
     * create a package with the redirects rules
     *
     * @param resourceResolver   user session
     * @param redirectParentPath the redirects parent, e.g. /conf/global/settings/redirects
     * @return path to the created package
     */
    String createPackage(ResourceResolver resourceResolver,
                         String redirectParentPath) throws IOException, RepositoryException, PackageException {

        long startTime = System.currentTimeMillis();
        Map<String, String> packageDefinitionProperties = new HashMap<>();
        packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                AccessControlHandling.OVERWRITE.toString());
        packageDefinitionProperties.put(JcrPackageDefinition.PN_DESCRIPTION,
                "ACS Commons Redirect Manager rules for " + redirectParentPath);

        String packageName = createPackageName(redirectParentPath);
        final JcrPackage jcrPackage = packageHelper.createPackageForPaths(
                Collections.singletonList(redirectParentPath),
                resourceResolver.adaptTo(Session.class),
                PACKAGE_GROUP,
                packageName,
                PACKAGE_VERSION,
                PackageHelper.ConflictResolution.IncrementVersion,
                packageDefinitionProperties
        );

        packageHelper.addThumbnail(jcrPackage,
                resourceResolver.getResource(PACKAGE_THUMBNAIL_RESOURCE_PATH));

        log.debug("package built in {} ms", (System.currentTimeMillis() - startTime));
        log.debug("package size: {} MB", String.format("%.2f", (float) jcrPackage.getSize() / (1024 * 1024)));

        JcrPackageManager packageManager = packaging.getPackageManager(resourceResolver.adaptTo(Session.class));
        packageManager.assemble(jcrPackage, null);
        return jcrPackage.getNode().getPath();
    }

    /**
     * Update replication status on the redirect resource so that UI can display it
     */
    void updateReplicationStatusWithCurrentUser(Resource resource) throws PersistenceException {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        ValueMap vm = resource.adaptTo(ModifiableValueMap.class);
        if (vm == null) {
            log.error("Could not adapt resource to value map");
            return;
        }
        vm.put(NameConstants.PN_PAGE_LAST_REPLICATED, Calendar.getInstance());
        vm.put(NameConstants.PN_PAGE_LAST_REPLICATED_BY, resourceResolver.getUserID());
        resource.getResourceResolver().commit();
    }
}
