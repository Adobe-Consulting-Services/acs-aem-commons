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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.jcrclustersupport.ClusterAware;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationEvent;
import com.day.cq.replication.ReplicationStatus;

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
                value = {ReplicationAction.EVENT_TOPIC, ReplicationEvent.EVENT_TOPIC},
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
                value = JcrPackageReplicationStatusEventHandler.JOB_TOPIC,
                propertyPrivate = true
        )
})
@Service
public class JcrPackageReplicationStatusEventHandler implements JobConsumer, EventHandler, ClusterAware {
    private static final Logger log = LoggerFactory.getLogger(JcrPackageReplicationStatusEventHandler.class);

    private static final String FALLBACK_REPLICATION_USER_ID = "Package Replication";
    private static final String PROPERTY_PATHS = "paths";
    private static final String PROPERTY_REPLICATED_BY = "replicatedBy";

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

    // Previously "Package Replication"
    private static final String DEFAULT_REPLICATED_BY_OVERRIDE = "";
    private String replicatedByOverride = DEFAULT_REPLICATED_BY_OVERRIDE;
    @Property(label = "'Replicated By' Override",
            description = "The 'user name' to set the 'replicated by' property to. If left blank the ACTUAL user that issued the package replication will be used. Defaults to blank.",
            value = DEFAULT_REPLICATED_BY_OVERRIDE)
    public static final String PROP_REPLICATED_BY_OVERRIDE = "replicated-by.override";
    public static final String LEGACY_PROP_REPLICATED_BY_OVERRIDE = "replicated-by";


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

    private static final String SERVICE_NAME = "package-replication-status-event-listener";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public final void handleEvent(final Event event) {
        if (this.isMaster) {
            // Only run on master

            final Map<String, Object> jobConfig = getInfoFromEvent(event);
            final String[] paths = (String[]) jobConfig.get(PROPERTY_PATHS);

            if (this.containsJcrPackagePath(paths)) {
                ResourceResolver resourceResolver = null;
                try {
                    resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

                    final List<JcrPackage> jcrPackages = this.getJcrPackages(resourceResolver, paths);
                    if (CollectionUtils.isNotEmpty(jcrPackages)) {

                        for (final JcrPackage jcrPackage : jcrPackages) {
                            // Close jcrPackages after they've been used to check if a Job should be invoked.
                            jcrPackage.close();
                        }

                        jobManager.addJob(JOB_TOPIC, jobConfig);
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
        final String replicatedBy =
                StringUtils.defaultIfEmpty(this.replicatedByOverride, (String) job.getProperty(PROPERTY_REPLICATED_BY));

        log.debug("Processing Replication Status Update for JCR Package: {}", paths);

        ResourceResolver resourceResolver = null;
        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

            final List<JcrPackage> jcrPackages = this.getJcrPackages(resourceResolver, paths);

            if (CollectionUtils.isEmpty(jcrPackages)) {
                log.warn("JCR Package is unavailable for Replication Status Update at: {}", paths);
                return JobResult.OK;
            }

            for (final JcrPackage jcrPackage : jcrPackages) {
                try {
                    setReplicationStatus(jcrPackage, replicatedBy, resourceResolver);
                } finally {
                    // Close each package when we are done.
                    jcrPackage.close();
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

    private void setReplicationStatus(JcrPackage jcrPackage, String replicatedBy, ResourceResolver resourceResolver) {
        final List<Resource> resources = new ArrayList<Resource>();
        final String packageId;
        try {
            JcrPackageDefinition packageDefinition = jcrPackage.getDefinition();
            if (packageDefinition == null) {
                log.error("Could not determine the ID for just replicated package (package invalid?)");
                return;
            } else {
                packageId = packageDefinition.getId().toString();
            }
        } catch (RepositoryException e) {
            log.error("Could not determine the ID for just replicated package (package invalid?). ", e);
            return;
        } 
        
        try {
            for (final String packagePath : packageHelper.getContents(jcrPackage)) {
                final Resource resource = resourceResolver.getResource(packagePath);
                if (this.accept(resource)) {
                    resources.add(resource);
                }
            }
        } catch (RepositoryException|PackageException|IOException e) {
            log.error("Could not retrieve the Packages contents for package '" + packageId + "'", e);
            return;
        }
        try {
            if (resources.size() > 0) {
                replicationStatusManager.setReplicationStatus(resourceResolver,
                        replicatedBy,
                        getJcrPackageLastModified(jcrPackage),
                        ReplicationStatusManager.Status.ACTIVATED,
                        resources.toArray(new Resource[resources.size()]));

                log.info("Updated Replication Status for JCR Package: {}", packageId);
            } else {
                log.info("Could not find any resources in JCR Package [ {} ] that are candidates to have their Replication Status updated",
                        packageId);
            }
        } catch (RepositoryException|IOException e) {
            String paths = resources.stream().map( r -> r.getPath() ).collect( Collectors.joining( "," ) );
            log.error("Exception occurred updating replication status for contents of package '" + packageId + "' covering paths '" + paths + "'.", e);
        }
    }

    /**
     * Extracts relevant event information from a Granite Replication Event OR a Day CQ Replication event.
     * @param event the Osgi Event
     * @return a Map containing the relevant data points.
     */
    protected final Map<String, Object> getInfoFromEvent(Event event) {
        final Map<String, Object> eventConfig = new HashMap<>();

        final ReplicationEvent replicationEvent = ReplicationEvent.fromEvent(event);
        if (replicationEvent != null) {
            // Granite event
            final ReplicationAction replicationAction = replicationEvent.getReplicationAction();
            eventConfig.put(PROPERTY_PATHS, replicationAction.getPaths());
            eventConfig.put(PROPERTY_REPLICATED_BY, replicationAction.getUserId());
        } else {
            // CQ event
            String[] paths = (String[]) event.getProperty(ReplicationAction.PROPERTY_PATHS);
            if (paths == null) {
                paths = ArrayUtils.EMPTY_STRING_ARRAY;
            }

            String userId = (String) event.getProperty(ReplicationAction.PROPERTY_USER_ID);
            if (StringUtils.isBlank(userId)) {
                userId = StringUtils.defaultIfEmpty(this.replicatedByOverride, FALLBACK_REPLICATION_USER_ID);
            }

            eventConfig.put(PROPERTY_PATHS, paths);
            eventConfig.put(PROPERTY_REPLICATED_BY,userId);
        }

        return eventConfig;
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

            JcrPackage jcrPackage = null;

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
    @SuppressWarnings("squid:S3776")
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
     * @param jcrPackage the package obj
     * @return the package's last build time or null if none can be found
     * @throws RepositoryException
     */
    private Calendar getJcrPackageLastModified(final JcrPackage jcrPackage) throws RepositoryException, IOException {
        if (ReplicatedAt.CURRENT_TIME.equals(this.replicatedAt)) {
            return Calendar.getInstance();
        } else {
            return jcrPackage.getPackage().getCreated();
        }
    }

    @Activate
    protected void activate(final Map<String, String> config) throws LoginException {
        log.trace("Activating the ACS AEM Commons - JCR Package Replication Status Updater (Event Handler)");

        this.replicatedByOverride = PropertiesUtil.toString(config.get(PROP_REPLICATED_BY_OVERRIDE),
                                        PropertiesUtil.toString(config.get(LEGACY_PROP_REPLICATED_BY_OVERRIDE),
                                                DEFAULT_REPLICATED_BY_OVERRIDE));

        String tmp = PropertiesUtil.toString(config.get(PROP_REPLICATED_AT), "");
        try {
            this.replicatedAt = ReplicatedAt.valueOf(tmp);
        } catch (IllegalArgumentException ex) {
            this.replicatedAt = ReplicatedAt.PACKAGE_LAST_MODIFIED;
        }

        this.replicationStatusNodeTypes = PropertiesUtil.toStringArray(config.get(PROP_REPLICATION_STATUS_NODE_TYPES),
                DEFAULT_REPLICATION_STATUS_NODE_TYPES);

        log.info("Package Replication Status - Replicated By Override User: [ {} ]", this.replicatedByOverride);
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
