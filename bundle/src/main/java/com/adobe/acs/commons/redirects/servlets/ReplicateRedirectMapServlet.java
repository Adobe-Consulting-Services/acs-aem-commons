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
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.servlets.post.JSONResponse;
import org.apache.sling.servlets.post.PostResponse;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.*;
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

    @Reference
    private transient Replicator replicator;

    @Reference
    private transient PackageHelper packageHelper;

    @Override
    protected void doPost(SlingHttpServletRequest slingRequest, SlingHttpServletResponse slingResponse)
            throws ServletException, IOException {

        PostResponse postResponse = new JSONResponse();
        String path = slingRequest.getResource().getPath();
        postResponse.setPath(path);
        log.debug("replicating {}", path);
        try {
            ResourceResolver resourceResolver = slingRequest.getResourceResolver();
            String packagePath = createPackage(resourceResolver, slingRequest.getResource());

            Session session = resourceResolver.adaptTo(Session.class);
            replicator.replicate(session, ReplicationActionType.ACTIVATE, packagePath);

            updateStatus(slingRequest.getResource());
            postResponse.setTitle("Redirects replicated: " + path);
        } catch (IOException | ReplicationException | RepositoryException | PackageException e) {
            log.error("failed to replicate redirects", e);
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
                .map(p -> JcrUtil.createValidName(p))
                .collect(Collectors.joining("."));

    }

    /**
     * create a package with the redirects rules
     *
     * @param resourceResolver user session
     * @param resource         the redirects parent, e.g. /conf/global/settings/redirects
     * @return path to the created package
     */
    String createPackage(ResourceResolver resourceResolver, Resource resource) throws IOException, RepositoryException, PackageException {

        long t0 = System.currentTimeMillis();
        Map<String, String> packageDefinitionProperties = new HashMap<>();
        packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                AccessControlHandling.OVERWRITE.toString());
        packageDefinitionProperties.put(JcrPackageDefinition.PN_DESCRIPTION,
                "ACS Commons Redirect Manager rules for " + resource.getPath());

        String packageName = createPackageName(resource.getPath());
        final JcrPackage jcrPackage = packageHelper.createPackage(
                Collections.singletonList(resource),
                resourceResolver.adaptTo(Session.class),
                PACKAGE_GROUP,
                packageName,
                PACKAGE_VERSION,
                PackageHelper.ConflictResolution.IncrementVersion,
                packageDefinitionProperties
        );

        log.debug("package built in {} ms", (System.currentTimeMillis() - t0));
        log.debug("package size: {} MB", String.format("%.2f", (float) jcrPackage.getSize() / (1024 * 1024)));

        JcrPackageManager packageManager = PackagingService.getPackageManager(resourceResolver.adaptTo(Session.class));
        packageManager.assemble(jcrPackage, null);
        return jcrPackage.getNode().getPath();
    }

    /**
     * Update replication status on the redirect resource so that UI can display it
     */
    void updateStatus(Resource resource) throws PersistenceException {
        ResourceResolver resourceResolver = resource.getResourceResolver();
        ;
        ValueMap vm = resource.adaptTo(ModifiableValueMap.class);
        vm.put("cq:lastReplicated", Calendar.getInstance());
        vm.put("cq:lastReplicatedBy", resourceResolver.getUserID());
        resource.getResourceResolver().commit();
    }
}
