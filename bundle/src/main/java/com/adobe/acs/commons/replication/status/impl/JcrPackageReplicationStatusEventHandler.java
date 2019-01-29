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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.jcr.Node;
import javax.jcr.RepositoryException;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
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
import org.apache.sling.discovery.TopologyEvent;
import org.apache.sling.discovery.TopologyEventListener;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationEvent;
import com.day.cq.replication.ReplicationStatus;

@Component(
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                EventConstants.EVENT_TOPIC + "=[" + ReplicationAction.EVENT_TOPIC + "," + ReplicationEvent.EVENT_TOPIC + "]",
                EventConstants.EVENT_FILTER + "=" + "(" + ReplicationAction.PROPERTY_TYPE + "=ACTIVATE)",
                JobConsumer.PROPERTY_TOPICS + "=" + JcrPackageReplicationStatusEventHandler.JOB_TOPIC
        }
)
@Designate(
        ocd = JcrPackageReplicationStatusEventHandler.Config.class
)
public class JcrPackageReplicationStatusEventHandler implements JobConsumer, EventHandler, TopologyEventListener {
    private static final Logger log = LoggerFactory.getLogger(JcrPackageReplicationStatusEventHandler.class);

    private static final String FALLBACK_REPLICATION_USER_ID = "Package Replication";
    private static final String PROPERTY_PATHS = "paths"; // this is not used for the actual Sling Job
    private static final String PROPERTY_PATH = "path";
    private static final String PROPERTY_REPLICATED_BY = "replicatedBy";

    private enum ReplicatedAt {
        CURRENT_TIME,
        PACKAGE_LAST_MODIFIED;
    }

    static final String[] DEFAULT_REPLICATION_STATUS_NODE_TYPES = {
        "cq:Page/cq:PageContent (?!/conf/.*/settings/wcm/templates/[^/]*/initial).*", // make sure to not cover initial content below editable templates
        "dam:AssetContent",
        "rep:User",
        "rep:Group",
        "sling:OrderedFolder/nt:unstructured",
        ReplicationStatus.NODE_TYPE, // replication status must be after cq:PageContent, because cq:PageContent is of mixin "cq:ReplicatonStatus" as well
        "cq:Page/nt:unstructured /conf/.*/settings/wcm/templates/.*/policies/.*", // this is for editable template's policy mappings
        "nt:unstructured /conf/.*/settings/wcm/policies/.*" // cover policies below editable templates
    };

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Package Replication Status Updater",
            description = "Event handler that listens for Jcr Package replications and updates the Replication Status of "
                    + "its content accordingly.")
    public @interface Config {
        @AttributeDefinition(
                name = "Replication Status Node Type and Path Restrictions",
                description = "Node types that are candidates to update Replication Status on. Each item has the format '<nodetype-restriction> (<path-restriction>)'. The <path-restriction> is optional. The <nodetype-restriction> may be composed out of several node types separated by '/'. Make sure that one (composed)nodetype value appears only once in the list (because duplicate node$_$types will overwrite each other)! Also the order is important as the first nodetype hit (from the top of the list) determines the outcome.",
                defaultValue = {
                        "cq:Page/cq:PageContent (?!/conf/.*/settings/wcm/templates/[^/]*/initial).*", // make sure to not cover initial content below editable templates
                        "dam:AssetContent",
                        "rep:User",
                        "rep:Group",
                        "sling:OrderedFolder/nt:unstructured",
                        ReplicationStatus.NODE_TYPE, // replication status must be after cq:PageContent, because cq:PageContent is of mixin "cq:ReplicatonStatus" as well
                        "cq:Page/nt:unstructured /conf/.*/settings/wcm/templates/.*/policies/.*", // this is for editable template's policy mappings
                        "nt:unstructured /conf/.*/settings/wcm/policies/.*" // cover policies below editable templates
                })
        String[] node$_$types();

        @AttributeDefinition(name = "'Replicated By' Override",
                description = "The 'user name' to set the 'replicated by' property to. If left blank the ACTUAL user that issued the package replication will be used. Defaults to blank.",
                defaultValue = DEFAULT_REPLICATED_BY_OVERRIDE)
        String replicated$_$by_override();

        @AttributeDefinition(
                name = "Replicated At",
                description = "The 'value' used to set the 'replicated at' property. [ Default: Package Last Modified ]",
                options = {
                        @Option(
                                value = "PACKAGE_LAST_MODIFIED",
                                label = "Package Last Modified"
                        ),
                        @Option(
                                value = "CURRENT_TIME",
                                label = "Current Time"
                        )
                })
        String replicated$_$at();
    }

    public static final String PROP_REPLICATION_STATUS_NODE_TYPES = "node-types";

    public static final String PROP_REPLICATED_BY_OVERRIDE = "replicated-by.override";
    public static final String LEGACY_PROP_REPLICATED_BY_OVERRIDE = "replicated-by";

    public static final String PROP_REPLICATED_AT = "replicated-at";

    /**
     * key = allowed node type (hierarchy), value = optional path restriction (may be null).
     */
    private Map<String, Pattern> pathRestrictionByNodeType;

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

    private boolean isLeader = false;

    private static final String DEFAULT_REPLICATED_BY_OVERRIDE = "";
    private String replicatedByOverride = DEFAULT_REPLICATED_BY_OVERRIDE;

    private static final ReplicatedAt DEFAULT_REPLICATED_AT = ReplicatedAt.PACKAGE_LAST_MODIFIED;
    private ReplicatedAt replicatedAt = DEFAULT_REPLICATED_AT;

    private static final String SERVICE_NAME = "package-replication-status-event-listener";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Override
    @SuppressWarnings("squid:S3776")
    public final void handleEvent(final Event event) {
        if (this.isLeader) {
            // Only run on master
            final Map<String, Object> jobConfig = getInfoFromEvent(event);
            final String[] paths = (String[]) jobConfig.get(PROPERTY_PATHS);

            try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO))  {
            
                for (String path : paths) {
                    if (!this.containsJcrPackagePath(path)) {
                        continue;
                    }
                    
                    final JcrPackage jcrPackage = this.getJcrPackage(resourceResolver, path);
                    if (jcrPackage != null) {
                        // Close jcrPackages after they've been used to check if a Job should be invoked.
                        jcrPackage.close();
                        jobConfig.put(PROPERTY_PATH, path);
                        // trigger one job per package to make one exception not affect other packages
                        jobManager.addJob(JOB_TOPIC, jobConfig);
                    }
                }
            } catch (LoginException e) {
                log.error("Could not obtain a resource resolver.", e);
            } 
        }
    }

    @Override
    public final JobResult process(final Job job) {
        final String path = (String) job.getProperty(PROPERTY_PATH);
        final String replicatedBy =
                StringUtils.defaultIfEmpty(this.replicatedByOverride, (String) job.getProperty(PROPERTY_REPLICATED_BY));

        log.debug("Processing Replication Status Update for JCR Package: {}", path);

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){

            final JcrPackage jcrPackage = this.getJcrPackage(resourceResolver, path);
            if (jcrPackage == null) {
                log.warn("JCR Package is unavailable for Replication Status Update at: {}", path);
                return JobResult.OK;
            }
            
            try {
                setReplicationStatus(jcrPackage, replicatedBy, resourceResolver);
            } finally {
                // Close  package when we are done.
                jcrPackage.close();
            }
            
        } catch (LoginException e) {
            log.error("Could not obtain a resource resolver for applying replication status updates", e);
            return JobResult.CANCEL;
        } catch (RepositoryException e) {
            logJobError(job, "Could not update replication metadata", e);
            return JobResult.FAILED;
        }

        return JobResult.OK;
    }

    /**
     * Emits the given error and exception either with level WARN or ERROR depending on whether the job is retried.
     * This method can be removed once <a href="https://issues.apache.org/jira/browse/SLING-7756">SLING-7756</a> is resolved.
     * @param job
     * @param errorMessage
     * @param e
     * 
     */
    private void logJobError(Job job, String errorMessage, Exception e) {
        if (job.getRetryCount() < job.getNumberOfRetries()) {
            log.warn("Job failed with error '{}' in attempt '{}', retry later.", errorMessage, job.getRetryCount(), e);
        } else {
            log.error("Job permanently failed with error '{}' in attempt '{}', no more retries", errorMessage, job.getRetryCount(), e);
        }
    }
    
    
    private void setReplicationStatus(JcrPackage jcrPackage, String replicatedBy, ResourceResolver resourceResolver) throws RepositoryException {
        final List<Resource> resources = new ArrayList<>();
        final String packageId;
        try {
            JcrPackageDefinition packageDefinition = jcrPackage.getDefinition();
            if (packageDefinition == null) {
                throw new RepositoryException("Could not determine the ID for just replicated package (package invalid?)");
            } else {
                packageId = packageDefinition.getId().toString();
            }
        } catch (RepositoryException e) {
            throw new RepositoryException("Could not determine the ID for just replicated package (package invalid?).", e);
        } 
        
        try {
            for (final String packagePath : packageHelper.getContents(jcrPackage)) {
                final Resource resource = resourceResolver.getResource(packagePath);
                if (this.accept(resource)) {
                    resources.add(resource);
                }
            }
        } catch (RepositoryException|PackageException|IOException e) {
            throw new RepositoryException("Could not retrieve the Packages contents for package '" + packageId + "'", e);
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
            // enrich exception with path information (limited to 10 paths only)
            String paths = resources.stream().map( r -> r.getPath() ).limit(10).collect( Collectors.joining( ", " ) );
            throw new RepositoryException("Exception occurred updating replication status for contents of package '" + packageId + "' covering paths: '" + paths + ", ...'", e);
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
     * Checks if the given path looks like a Jcr Package path.
     *
     * Provides a very fast, String-based, in-memory check to weed out most false positives and avoid
     * resolving the path to a Jcr Package and ensure it is valid.
     *
     * @param path
     * @return true if at least one path looks like a Jcr Package path
     */
    private boolean containsJcrPackagePath(final String path) {
        if (StringUtils.startsWith(path, "/etc/packages/")
                && StringUtils.endsWith(path, ".zip")) {
            // At least 1 entry looks like a package
            return true;
        }

        // Nothing looks like a package...
        return false;
    }

    /**
     * Resolves path to Jcr Package.
     *
     * @param path the path to resolve to Jcr Package
     * @return the Jcr Package that corresponds to the provided path or {@code null}
     */
    private JcrPackage getJcrPackage(final ResourceResolver resourceResolver, final String path) {

        final Resource eventResource = resourceResolver.getResource(path);
        if (eventResource == null) {
            log.warn("Could not find resource at path [ {} ] with the mapped service user. Either the resource has been removed meanwhile or the service user does not have the necessary rights.", path);
            return null;
        }
        JcrPackage jcrPackage = null;

        try {
            jcrPackage = packaging.open(eventResource.adaptTo(Node.class), false);
        } catch (RepositoryException e) {
            log.warn("Error checking if the path [ {} ] is a JCR Package.", path);
        }
        
        return jcrPackage;
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

        for (final Map.Entry<String, Pattern> nodeTypeAndPathRestriction : this.pathRestrictionByNodeType.entrySet()) {
            final String[] hierarchyNodeTypes = StringUtils.split(nodeTypeAndPathRestriction.getKey(), "/");

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
                // check path restrictions
                Pattern pathRestriction = nodeTypeAndPathRestriction.getValue();
                if (pathRestriction != null && !pathRestriction.matcher(resource.getPath()).matches()) {
                    log.debug("Path restriction '{}' prevents the resource at '{}' from getting its replication status updated!", pathRestriction, resource.getPath());
                    return false;
                }
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

        final String[] nodeTypeAndPathRestrictions = PropertiesUtil.toStringArray(config.get(PROP_REPLICATION_STATUS_NODE_TYPES),
                DEFAULT_REPLICATION_STATUS_NODE_TYPES);

        // the map must keep the order!
        pathRestrictionByNodeType = new LinkedHashMap<>();
        for (String nodeTypeAndPathRestrictionEntry : nodeTypeAndPathRestrictions) {
            Map.Entry<String, String> nodeTypeAndPathRestriction = ParameterUtil.toMapEntryWithOptionalValue(nodeTypeAndPathRestrictionEntry, " ");
            final Pattern pathRestrictionPattern;
            if (StringUtils.isNotBlank(nodeTypeAndPathRestriction.getValue())) {
                pathRestrictionPattern = Pattern.compile(nodeTypeAndPathRestriction.getValue());
            } else {
                pathRestrictionPattern = null;
            }
            
            pathRestrictionByNodeType.put(nodeTypeAndPathRestriction.getKey(), pathRestrictionPattern);
        }
        log.info("Package Replication Status - Replicated By Override User: [ {} ]", this.replicatedByOverride);
        log.info("Package Replication Status - Replicated At: [ {} ]", this.replicatedAt.toString());
        log.info("Package Replication Status - Node Types and Path Restrictions: [ {} ]", pathRestrictionByNodeType);
    }

    @Override
    public void handleTopologyEvent(TopologyEvent te) {
        this.isLeader = te.getNewView().getLocalInstance().isLeader();
    }
}
