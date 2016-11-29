/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationStatus;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.PackageException;
import com.day.jcr.vault.packaging.Packaging;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Package Replication Status Updater",
        description = "Event handler that listens for Jcr Package replications and updates the Replication Status of "
                + "its content accordingly.",
        metatype = true,
        immediate = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {ReplicationAction.EVENT_TOPIC},
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),
        @Property(
                label = "Event Filters",
                value = "(" + ReplicationAction.PROPERTY_TYPE + "=ACTIVATE)",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        ),
        @Property(
                name = JobConsumer.PROPERTY_TOPICS,
                value = JcrPackageReplicationStatusEventHandler.JOB_TOPIC
        )
})
@Service
public class JcrPackageReplicationStatusEventHandler implements JobConsumer, EventHandler, ClusterAware {
    private static final String PROPERTY_PATHS = "paths";

    private static final Logger log = LoggerFactory.getLogger(JcrPackageReplicationStatusEventHandler.class);

    private enum ReplicatedAt {
        CURRENT_TIME,
        PACKAGE_LAST_MODIFIED;
    }

    private static final String[] DEFAULT_REPLICATION_STATUS_NODE_TYPES = {
            ReplicationStatus.NODE_TYPE,
            "cq:Page/cq:PageContent",
            "dam:AssetContent",
            "rep:User",
            "rep:Group",
            "sling:OrderedFolder/nt:unstructured"
    };

    private String[] replicationStatusNodeTypes = DEFAULT_REPLICATION_STATUS_NODE_TYPES;

    @Property(label = "Replication Status Types",
            description = "Node types that are candidates to update Replication Status on",
            cardinality = Integer.MAX_VALUE,
            value = {
                    ReplicationStatus.NODE_TYPE,
                    "cq:PageContent",
                    "dam:AssetContent",
                    "rep:User",
                    "rep:Group",
                    "sling:OrderedFolder/nt:unstructured"
            })
    public static final String PROP_REPLICATION_STATUS_NODE_TYPES = "node-types";

    protected static final String JOB_TOPIC = "acs-commons/replication/package";

    @Reference
    private Packaging packaging;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private ReplicationStatusManager replicationStatusManager;

    @Reference
    private PackageHelper packageHelper;

    @Reference
    private JobManager jobManager;

    private boolean isMaster = false;

    private static final String DEFAULT_REPLICATED_BY = "Package Replication";
    private String replicatedBy = DEFAULT_REPLICATED_BY;
    @Property(label = "Replicated By",
            description = "The 'name' to set the 'replicated by' property to. Defaults to: " + DEFAULT_REPLICATED_BY,
            value = DEFAULT_REPLICATED_BY)
    public static final String PROP_REPLICATED_BY = "replicated-by";

    private static final ReplicatedAt DEFAULT_REPLICATED_AT = ReplicatedAt.PACKAGE_LAST_MODIFIED;
    private ReplicatedAt replicatedAt = DEFAULT_REPLICATED_AT;
    @Property(label = "Replicated At",
            description = "The 'value' used to set the 'replicated at' property. [ Default: Package Last Modified ]",
            options = {
                    @PropertyOption(
                        name = "PACKAGE_LAST_MODIFIED",
                        value = "Package Last Modified"
                    ),
                    @PropertyOption(
                        name = "CURRENT_TIME",
                        value = "Current Time"
                    )
            })
    public static final String PROP_REPLICATED_AT = "replicated-at";

    @Override
    public final void handleEvent(final Event event) {
        if (this.isMaster) {
            // Only run on master

            final String[] paths = (String[]) event.getProperty(PROPERTY_PATHS);

            if (this.containsJcrPackagePath(paths)) {
                ResourceResolver resourceResolver = null;
                try {
                    resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                    if (CollectionUtils.isNotEmpty(this.getJcrPackages(resourceResolver, paths))) {
                        jobManager.addJob(JOB_TOPIC, Collections.<String, Object>singletonMap(PROPERTY_PATHS, paths));
                    }
                } catch (LoginException e) {
                    log.error("Could not obtain a resource resolver.", e);
                } finally {
                    if (resourceResolver != null) {
                        resourceResolver.close();
                    }
                }
            }
        }
    }

    @Override
    public final JobResult process(final Job job) {
        final String[] paths = (String[]) job.getProperty(PROPERTY_PATHS);

        log.debug("Processing Replication Status Update for JCR Package: {}", paths);

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            final List<JcrPackage> jcrPackages = this.getJcrPackages(resourceResolver, paths);

            if (CollectionUtils.isEmpty(jcrPackages)) {
                log.warn("JCR Package is unavailable for Replication Status Update at: {}", paths);
                return JobResult.OK;
            }

            for (final JcrPackage jcrPackage : jcrPackages) {
                try {
                    final List<Resource> resources = new ArrayList<Resource>();

                    for (final String packagePath : packageHelper.getContents(jcrPackage)) {
                        final Resource resource = resourceResolver.getResource(packagePath);
                        if (this.accept(resource))  {
                            resources.add(resource);
                        }
                    }

                    if (resources.size() > 0) {
                        replicationStatusManager.setReplicationStatus(resourceResolver,
                                this.replicatedBy,
                                getJcrPackageLastModified(resourceResolver, jcrPackage),
                                ReplicationStatusManager.Status.ACTIVATED,
                                resources.toArray(new Resource[resources.size()]));

                        log.info("Updated Replication Status for JCR Package: {}", jcrPackage.getDefinition().getId());
                    } else {
                        log.info("Could not find any resources in JCR Package [ {} ] that are candidates to have their Replication Status updated",
                                jcrPackage.getDefinition().getId());
                    }
                } catch (RepositoryException e) {
                    log.error("RepositoryException occurred updating replication status for contents of package");
                    log.error(e.getMessage());

                } catch (IOException e) {
                    log.error("IOException occurred updating replication status for contents of package");
                    log.error(e.getMessage());

                } catch (PackageException e) {
                    log.error("Could not retrieve the Packages contents.");
                    log.error(e.getMessage());
                }
            }
        } catch (LoginException e) {
            log.error("Could not obtain a resource resolver for applying replication status updates", e);
            return JobResult.FAILED;
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }

        return JobResult.OK;
    }

    /**
     * Checks if any path in the array of paths looks like a Jcr Package path.
     *
     * Provides a very fast, String-based, in-memory check to weed out most false positives and avoid
     * resolving the path to a Jcr Package and ensure it is valid.
     *
     * @param paths the array of paths
     * @return true if at least one path looks like a Jcr Package path
     */
    private boolean containsJcrPackagePath(final String[] paths) {
        for (final String path : paths) {
            if (StringUtils.startsWith(path, "/etc/packages/")
                    && StringUtils.endsWith(path, ".zip")) {
                // At least 1 entry looks like a package
                return true;
            }
        }

        // Nothing looks like a package...
        return false;
    }

    /**
     * Resolves paths to Jcr Packages. If any path does not resolve to a valid Jcr Package, it is discarded.
     *
     * @param paths the list of paths to resolve to Jcr Packages
     * @return a list of Jcr Packages that correspond to the provided paths
     */
    private List<JcrPackage> getJcrPackages(final ResourceResolver resourceResolver, final String[] paths) {
        final List<JcrPackage> packages = new ArrayList<JcrPackage>();

        for (final String path : paths) {
            final Resource eventResource = resourceResolver.getResource(path);

            JcrPackage jcrPackage;

            try {
                jcrPackage = packaging.open(eventResource.adaptTo(Node.class), false);
                if (jcrPackage != null) {
                    packages.add(jcrPackage);
                }
            } catch (RepositoryException e) {
                log.warn("Error checking if the path [ {} ] is a JCR Package.", path);
            }

        }
        return packages;
    }

    /**
     * Checks if the ReplicationStatusManager should make the provides resource w replication status.
     *
     * @param resource the return
     * @return true is the resource is markable resource
     * @throws RepositoryException
     */
    private boolean accept(final Resource resource) throws RepositoryException {
        if (resource == null || ResourceUtil.isNonExistingResource(resource)) {
            return false;
        }

        for (final String nodeTypes : this.replicationStatusNodeTypes) {
            final String[] hierarchyNodeTypes = StringUtils.split(nodeTypes, "/");

            boolean match = true;
            Resource walkingResource = resource;

            for (int i = (hierarchyNodeTypes.length - 1); i >= 0; i--) {

                if (walkingResource == null) {
                    match = false;
                    break;
                } else {
                    final Node node = walkingResource.adaptTo(Node.class);

                    if (node == null || !node.isNodeType(hierarchyNodeTypes[i])) {
                        match = false;
                        break;
                    }

                    walkingResource = walkingResource.getParent();
                }
            }

            if (match) {
                return true;
            }
        }

        return false;
    }

    /**
     * Gets the last build time of the package.
     *
     * @param resourceResolver the resource resolver to access the package properties
     * @param jcrPackage the package obj
     * @return the package's last build time or null if none can be found
     * @throws RepositoryException
     */
    private Calendar getJcrPackageLastModified(final ResourceResolver resourceResolver,
                                               final JcrPackage jcrPackage) throws RepositoryException {
        if (ReplicatedAt.CURRENT_TIME.equals(this.replicatedAt)) {
            return Calendar.getInstance();
        } else {
            final String path = jcrPackage.getNode().getPath();
            final Resource resource = resourceResolver.getResource(path).getChild(JcrConstants.JCR_CONTENT);
            final ValueMap properties = resource.adaptTo(ValueMap.class);

            return properties.get(JcrConstants.JCR_LASTMODIFIED, Calendar.class);
        }
    }

    @Activate
    private void activate(final Map<String, String> config) throws LoginException {
        log.trace("Activating the ACS AEM Commons - JCR Package Replication Status Updater (Event Handler)");

        this.replicatedBy = PropertiesUtil.toString(config.get(PROP_REPLICATED_BY), DEFAULT_REPLICATED_BY);

        String tmp = PropertiesUtil.toString(config.get(PROP_REPLICATED_AT), "");
        try {
            this.replicatedAt = ReplicatedAt.valueOf(tmp);
        } catch (IllegalArgumentException ex) {
            this.replicatedAt = ReplicatedAt.PACKAGE_LAST_MODIFIED;
        }

        this.replicationStatusNodeTypes = PropertiesUtil.toStringArray(config.get(PROP_REPLICATION_STATUS_NODE_TYPES),
                DEFAULT_REPLICATION_STATUS_NODE_TYPES);

        log.info("Package Replication Status - Replicated By: [ {} ]", this.replicatedBy);
        log.info("Package Replication Status - Replicated At: [ {} ]", this.replicatedAt.toString());
        log.info("Package Replication Status - Node Types: [ {} ]",
                StringUtils.join(this.replicationStatusNodeTypes, ", "));
    }

    @Override
    public final void bindRepository(String repositoryId, String clusterId, boolean newIsMaster) {
        this.isMaster = newIsMaster;
    }

    @Override
    public final void unbindRepository() {
        this.isMaster = false;
    }
}
