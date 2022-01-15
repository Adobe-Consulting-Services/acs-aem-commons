/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.dam.impl;


import com.adobe.acs.commons.cqsearch.QueryUtil;
import com.adobe.granite.asset.api.Asset;
import com.adobe.granite.asset.api.AssetManager;
import com.adobe.granite.asset.api.AssetVersionManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Review Task Move Handler",
        description = "Create an OSGi configuration to enable this feature.",
        metatype = true,
        immediate = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                label = "Event Topics",
                value = {ReviewTaskAssetMoverHandler.DEFAULT_TOPIC},
                description = "[Required] Event Topics this event handler will to respond to. Defaults to: com/adobe/granite/taskmanagement/event",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),

        /* Event filters support LDAP filter syntax and have access to event.getProperty(..) values */
        /* LDAP Query syntax: https://goo.gl/MCX2or */
        @Property(
                label = "Event Filters",
                // Only listen on events associated with nodes that end with /jcr:content
                value = "(&(TaskTypeName=dam:review)(EventType=TASK_COMPLETED))",
                description = "Event Filters used to further restrict this event handler; Uses LDAP expression against event properties. Defaults to: (&(TaskTypeName=dam:review)(EventType=TASK_COMPLETED))",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@Service
public class ReviewTaskAssetMoverHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskAssetMoverHandler.class);

    public static final String DEFAULT_TOPIC = "com/adobe/granite/taskmanagement/event";
    private static final String PATH_CONTENT_DAM = DamConstants.MOUNTPOINT_ASSETS;
    private static final String APPROVED = "approved";
    private static final String REJECTED = "rejected";
    private static final String REL_ASSET_METADATA = "jcr:content/metadata";
    private static final String REL_ASSET_RENDITIONS = "jcr:content/renditions";
    private static final String REL_PN_DAM_STATUS = REL_ASSET_METADATA + "/dam:status";

    private static final String PN_ON_APPROVE = "onApproveMoveTo";
    private static final String PN_ON_REJECT = "onRejectMoveTo";
    private static final String PN_CONTENT_PATH = "contentPath";
    private static final String PN_CONFLICT_RESOLUTION = "onReviewConflictResolution";
    private static final String CONFLICT_RESOLUTION_SKIP = "skip";
    private static final String CONFLICT_RESOLUTION_REPLACE = "replace";
    private static final String CONFLICT_RESOLUTION_NEW_ASSET = "new-asset";
    private static final String CONFLICT_RESOLUTION_NEW_VERSION = "new-version";

    public static final String USER_EVENT_TYPE = "acs-aem-commons.review-task-mover";

    private static final String SERVICE_NAME = "review-task-asset-mover";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private QueryBuilder queryBuilder;

    private static final String DEFAULT_DEFAULT_CONFLICT_RESOLUTION = CONFLICT_RESOLUTION_NEW_VERSION;
    private String defaultConflictResolution = DEFAULT_DEFAULT_CONFLICT_RESOLUTION;
    @Property(label = "Default Conflict Resolution",
            description = "Select default behavior if conflict resolution is not provided at the review task level.",
            options = {
                    @PropertyOption(name = CONFLICT_RESOLUTION_NEW_VERSION, value = "Add as version (new-version)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_NEW_ASSET, value = "Add as new asset (new-asset)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_REPLACE, value = "Replace (replace)"),
                    @PropertyOption(name = CONFLICT_RESOLUTION_SKIP, value = "Skip (skip)")
            },
            value = DEFAULT_DEFAULT_CONFLICT_RESOLUTION)
    public static final String PROP_DEFAULT_CONFLICT_RESOLUTION = "conflict-resolution.default";

    private static final String DEFAULT_LAST_MODIFIED_BY = "Review Task";
    private String lastModifiedBy = DEFAULT_LAST_MODIFIED_BY;
    @Property(label = "Last Modified By",
            description = "For Conflict Resolution: Version, the review task event does not track the user that completed the event. Use this property to specify the static name of of the [dam:Asset]/jcr:content@jcr:lastModifiedBy. Default: Review Task",
            value = DEFAULT_LAST_MODIFIED_BY)
    public static final String PROP_LAST_MODIFIED_BY = "conflict-resolution.version.last-modified-by";


    @Activate
    protected void activate(Map<String, Object> config) {
        lastModifiedBy = PropertiesUtil.toString(config.get(PROP_LAST_MODIFIED_BY), DEFAULT_LAST_MODIFIED_BY);
        defaultConflictResolution = PropertiesUtil.toString(config.get(PROP_DEFAULT_CONFLICT_RESOLUTION), DEFAULT_DEFAULT_CONFLICT_RESOLUTION);
    }

    @Override
    public void handleEvent(Event event) {

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {
            final String path = (String) event.getProperty("TaskId");
            final Resource taskResource = resourceResolver.getResource(path);

            if (taskResource != null) {
                final ValueMap taskProperties = taskResource.getValueMap();

                // Perform a fast check to see if this project has the required properties to perform the asset moving
                if (StringUtils.startsWith(taskProperties.get(PN_ON_APPROVE, String.class), PATH_CONTENT_DAM)
                        || StringUtils.startsWith(taskProperties.get(PN_ON_REJECT, String.class), PATH_CONTENT_DAM)) {

                    log.debug("Handling event (creating a Job) for Assets Review Task @ [ {} ]", path);

                    ScheduleOptions options = scheduler.NOW();
                    String jobName = this.getClass().getSimpleName().toString().replace(".", "/") + "/" + path;
                    options.name(jobName);

                    options.canRunConcurrently(false);

                    scheduler.schedule(new ImmediateJob(path), options);
                }
            }
        } catch (LoginException e) {
            log.error("Could not get resource resolver", e);
        }
    }

    private class ImmediateJob implements Runnable {
        private final String path;

        public ImmediateJob(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)) {

                // Access data passed into the Job from the Event
                Resource resource = resourceResolver.getResource(path);
                AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

                if (resource != null) {
                    ValueMap taskProperties = resource.getValueMap();
                    String contentPath = taskProperties.get(PN_CONTENT_PATH, String.class);

                    if (StringUtils.startsWith(contentPath, PATH_CONTENT_DAM)) {
                        Query query = findAssets(resourceResolver, contentPath);
                        log.debug("Found [ {} ] assets under [ {} ] that were reviewed and require processing.",
                                query.getResult().getHits().size(),
                                contentPath);

                        final Iterator<Resource> assets = query.getResult().getResources();
                        resourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData(USER_EVENT_TYPE);

                        while (assets.hasNext()) {
                            final Asset asset = assetManager.getAsset(assets.next().getPath());
                            moveAsset(resourceResolver, assetManager, asset, taskProperties);
                        }
                    }
                }
            } catch (Exception e) {
                log.error("Could not process Review Task Mover", e);
            }
        }

        /**
         * Find all assets under the Task contentPath that have a dam:status of approved or rejected.
         *
         * @param resourceResolver the resource resolver used to find the Assets to move.
         * @param contentPath      the DAM contentPath which the task covers.
         * @return the CloseableQuery whose result represents dam:Assets for which dam:status is set to approved or rejected
         */
        private Query findAssets(ResourceResolver resourceResolver, String contentPath) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("type", DamConstants.NT_DAM_ASSET);
            params.put("path", contentPath);
            params.put("property", REL_PN_DAM_STATUS);
            params.put("property.1_value", APPROVED);
            params.put("property.2_value", REJECTED);
            params.put("p.offset", "0");
            params.put("p.limit", "-1");

            Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
            QueryUtil.setResourceResolverOn(resourceResolver, query);
            return query;
        }


        /**
         * Create a unique asset name based on the current time and a up-to-1000 counter.
         *
         * @param assetManager assetManager object
         * @param destPath     the folder the asset will be moved into
         * @param assetName    the asset name
         * @return a unique asset path to the asset
         * @throws PersistenceException
         */
        private String createUniqueAssetPath(AssetManager assetManager, String destPath, String assetName) throws PersistenceException {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            final String now = sdf.format(new Date());
            String destAssetPath = destPath + "/" + assetName;

            int count = 0;
            while (assetManager.assetExists(destAssetPath)) {
                if (count > 1000) {
                    throw new PersistenceException("Unable to generate a unique name after 1000 attempts. Something must be wrong!");
                }

                if (count == 0) {
                    destAssetPath = destPath + "/" + now + "_" + assetName;
                } else {
                    destAssetPath = destPath + "/" + now + "_" + count + "_" + assetName;
                }

                count++;
            }

            return destAssetPath;
        }

        /**
         * Creates a new revision of an asset and replaces its renditions (including original), and metadata node.
         *
         * @param resourceResolver the ResourceResolver object
         * @param assetManager     the AssetManager object
         * @param originalAsset    the asset to create a new version for
         * @param reviewedAsset    the asset to that will represent the new version
         * @throws PersistenceException
         */
        private void createRevision(ResourceResolver resourceResolver, AssetManager assetManager, Asset originalAsset, Asset reviewedAsset) throws PersistenceException {
            Session session = resourceResolver.adaptTo(Session.class);

            // Create the new version
            AssetVersionManager versionManager = resourceResolver.adaptTo(AssetVersionManager.class);
            versionManager.createVersion(originalAsset.getPath(), "Review Task (" + reviewedAsset.getValueMap().get(REL_PN_DAM_STATUS, "Unknown") + ")");

            String assetPath = originalAsset.getPath();

            // Delete the existing metadata and renditions from the old asset

            resourceResolver.delete(resourceResolver.getResource(assetPath + "/" + REL_ASSET_METADATA));
            resourceResolver.delete(resourceResolver.getResource(assetPath + "/" + REL_ASSET_RENDITIONS));

            try {
                Node originalAssetJcrContentNode = session.getNode(originalAsset.getPath() + "/" + JcrConstants.JCR_CONTENT);

                Node newAssetMetadataNode = session.getNode(reviewedAsset.getPath() + "/" + REL_ASSET_METADATA);
                Node newAssetRenditionsNode = session.getNode(reviewedAsset.getPath() + "/" + REL_ASSET_RENDITIONS);

                JcrUtil.copy(newAssetMetadataNode, originalAssetJcrContentNode, null);
                JcrUtil.copy(newAssetRenditionsNode, originalAssetJcrContentNode, null);

                JcrUtil.setProperty(originalAssetJcrContentNode, JcrConstants.JCR_LASTMODIFIED, new Date());
                JcrUtil.setProperty(originalAssetJcrContentNode, JcrConstants.JCR_LAST_MODIFIED_BY, lastModifiedBy);

                assetManager.removeAsset(reviewedAsset.getPath());
            } catch (RepositoryException e) {
                log.error("Could not create a new version of the asset", e);
                throw new PersistenceException(e.getMessage());
            }
        }

        /**
         * Move the asset based on the its dam:status (approved or rejected).
         *
         * @param asset          the asset to move
         * @param taskProperties the task properties containing the target onApproveMoveTo and onRejectMoveTo paths
         */
        @SuppressWarnings("squid:S3776")
        private void moveAsset(ResourceResolver resourceResolver, AssetManager assetManager, Asset asset, ValueMap taskProperties) {
            try {
                final String status = asset.getValueMap().get(REL_PN_DAM_STATUS, String.class);
                final String conflictResolution = taskProperties.get(PN_CONFLICT_RESOLUTION, defaultConflictResolution);
                final String onApprovePath = taskProperties.get(PN_ON_APPROVE, String.class);
                final String onRejectPath = taskProperties.get(PN_ON_REJECT, String.class);

                String destPath = null;

                if (StringUtils.equals(APPROVED, status)) {
                    destPath = onApprovePath;
                } else if (StringUtils.equals(REJECTED, status)) {
                    destPath = onRejectPath;
                }

                if (destPath != null) {
                    if (StringUtils.startsWith(destPath, PATH_CONTENT_DAM)) {

                        String destAssetPath = destPath + "/" + asset.getName();
                        final boolean exists = assetManager.assetExists(destAssetPath);

                        if (exists) {
                            if (StringUtils.equals(asset.getPath(), destAssetPath)) {
                                log.info("Reviewed asset [ {} ] is already in its final location, so there is nothing to do.", asset.getPath());
                            } else if (CONFLICT_RESOLUTION_REPLACE.equals(conflictResolution)) {
                                assetManager.removeAsset(destAssetPath);
                                resourceResolver.commit();
                                assetManager.moveAsset(asset.getPath(), destAssetPath);
                                log.info("Moved with replace [ {} ] ~> [ {} ] based on approval status [ {} ]",
                                        asset.getPath(), destAssetPath, status);
                            } else if (CONFLICT_RESOLUTION_NEW_ASSET.equals(conflictResolution)) {
                                destAssetPath = createUniqueAssetPath(assetManager, destPath, asset.getName());
                                assetManager.moveAsset(asset.getPath(), destAssetPath);
                                log.info("Moved with unique asset name [ {} ] ~> [ {} ] based on approval status [ {} ]",
                                        asset.getPath(), destAssetPath, status);
                            } else if (CONFLICT_RESOLUTION_NEW_VERSION.equals(conflictResolution)) {
                                log.info("Creating new version of existing asset [ {} ] ~> [ {} ] based on approval status [ {} ]",
                                        asset.getPath(), destAssetPath, status);
                                createRevision(resourceResolver, assetManager, assetManager.getAsset(destAssetPath), asset);
                            } else if (CONFLICT_RESOLUTION_SKIP.equals(conflictResolution)) {
                                log.info("Skipping with due to existing asset at the same destination [ {} ] ~> [ {} ] based on approval status [ {} ]",
                                        asset.getPath(), destAssetPath, status);
                            }
                        } else {
                            assetManager.moveAsset(asset.getPath(), destAssetPath);
                            log.info("Moved [ {} ] ~> [ {} ] based on approval status [ {} ]",
                                    asset.getPath(), destAssetPath, status);
                        }
                    } else {
                        log.warn("Request to move reviewed asset to a non DAM Asset path [ {} ]", destPath);
                    }
                }

                if (resourceResolver.hasChanges()) {
                    resourceResolver.commit();
                }
            } catch (PersistenceException e) {
                log.error("Could not move reviewed asset [ {} ]", asset.getPath(), e);
                resourceResolver.revert();
                resourceResolver.refresh();
            }
        }
    }

    protected void bindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory == null) {
            this.resourceResolverFactory = resourceResolverFactory;
        }
    }

    protected void unbindResourceResolverFactory(ResourceResolverFactory resourceResolverFactory) {
        if (this.resourceResolverFactory == resourceResolverFactory) {
            this.resourceResolverFactory = null;
        }
    }

    protected void bindScheduler(Scheduler scheduler) {
        if (this.scheduler == null) {
            this.scheduler = scheduler;
        }
    }

    protected void unbindScheduler(Scheduler scheduler) {
        if (this.scheduler == scheduler) {
            this.scheduler = null;
        }
    }

    protected void bindQueryBuilder(QueryBuilder queryBuilder) {
        if (this.queryBuilder == null) {
            this.queryBuilder = queryBuilder;
        }
    }

    protected void unbindQueryBuilder(QueryBuilder queryBuilder) {
        if (this.queryBuilder == queryBuilder) {
            this.queryBuilder = null;
        }
    }
}