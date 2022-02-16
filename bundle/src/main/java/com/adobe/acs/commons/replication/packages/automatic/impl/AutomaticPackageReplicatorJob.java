/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.PackagingService;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventAdmin;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.Replicator;

/**
 * Simple Job Runnable for automatically replicating a package.
 */
public class AutomaticPackageReplicatorJob implements Runnable, EventHandler {
    private static final Logger log = LoggerFactory.getLogger(AutomaticPackageReplicatorJob.class);
    public static final String SERVICE_USER_NAME = "acs-commons-automatic-package-replication-service";
    public static final String OSGI_EVENT_REPLICATED_TOPIC = "com/adobe/acs/commons/automatic_page_replicator/REPLICATED";
    public static final String OSGI_EVENT_FAILED_TOPIC = "com/adobe/acs/commons/automatic_page_replicator/REPLICATION_FAILED";
    public static final String OSGI_EVENT_PACKAGE_PATH_PARAM = "packagePath";

    private final Replicator replicator;
    private final String packagePath;
    private final ResourceResolverFactory resolverFactory;
    private final EventAdmin eventAdmin;

    public AutomaticPackageReplicatorJob(final ResourceResolverFactory resolverFactory, final Replicator replicator,
            final EventAdmin eventAdmin, final String packagePath) {
        this.replicator = replicator;
        this.packagePath = packagePath;
        this.resolverFactory = resolverFactory;
        this.eventAdmin = eventAdmin;
    }

    public void excute() throws RepositoryException, PackageException, IOException, ReplicationException {

        boolean succeeded = false;
        try (ResourceResolver resolver = ConfigurationUpdateListener.getResourceResolver(resolverFactory)){

            Session session = resolver.adaptTo(Session.class);

            JcrPackageManager pkgMgr = PackagingService.getPackageManager(session);
            PackageId packageId = new PackageId(packagePath);

            // check if the package exists
            try (final JcrPackage jcrPackage = pkgMgr.open(packageId)) {
                if (jcrPackage == null || jcrPackage.getNode() == null) {
                    log.warn("Package at path '{}' does not exist", packagePath);
                    throw new IllegalArgumentException("Package at path " + packagePath + " does not exist");
                }

                log.debug("Assembling package {}", packagePath);
                pkgMgr.assemble(jcrPackage, null);

                log.debug("Replicating package {}", packagePath);
                replicator.replicate(session, ReplicationActionType.ACTIVATE, jcrPackage.getNode().getPath());

                log.debug("Package {} replicated successfully!", packagePath);
                fireEvent(OSGI_EVENT_REPLICATED_TOPIC);
                succeeded = true;
            }
        } finally {
            if(!succeeded){
                fireEvent(OSGI_EVENT_FAILED_TOPIC);
            }
        }
    }

    private void fireEvent(String topic) {
        final Event event = new Event(topic, Collections.singletonMap(OSGI_EVENT_PACKAGE_PATH_PARAM, packagePath));
        eventAdmin.postEvent(event);
    }

    @Override
    public void run() {
        log.trace("run");
        try {
            excute();
        } catch (Exception e) {
            log.error("Excepting running Automatic Package Replication task", e);
        }
    }

    @Override
    public void handleEvent(Event event) {
        run();
    }

}
