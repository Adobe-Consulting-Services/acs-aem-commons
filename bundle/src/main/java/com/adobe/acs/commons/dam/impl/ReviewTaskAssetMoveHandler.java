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


import com.adobe.granite.asset.api.AssetManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Component(immediate = true)
@Properties({
        @Property(
                label = "Event Topics",
                value = {"com/adobe/granite/taskmanagement/event"},
                description = "[Required] Event Topics this event handler will to respond to.",
                name = EventConstants.EVENT_TOPIC,
                propertyPrivate = true
        ),

        /* Event filters support LDAP filter syntax and have access to event.getProperty(..) values */
        /* LDAP Query syntax: https://goo.gl/MCX2or */
        @Property(
                label = "Event Filters",
                // Only listen on events associated with nodes that end with /jcr:content
                value = "(&(TaskTypeName=dam:review)(EventType=TASK_COMPLETED))",
                description = "[Optional] Event Filters used to further restrict this event handler; Uses LDAP expression against event properties.",
                name = EventConstants.EVENT_FILTER,
                propertyPrivate = true
        )
})
@Service
public class ReviewTaskAssetMoveHandler implements EventHandler {
    private static final Logger log = LoggerFactory.getLogger(ReviewTaskAssetMoveHandler.class);

    private static final String PATH_CONTENT_DAM = "/content/dam";
    private static final String APPROVED = "approved";
    private static final String REJECTED = "rejected";
    private static final String REL_PN_DAM_STATUS = "jcr:content/metadata/dam:status";
    private static final String PN_DAM_STATUS = "dam:status";
    private static final String PN_ON_APPROVE = "onApproveMoveTo";
    private static final String PN_ON_REJECT = "onRejectMoveTo";
    private static final String PN_CONTENT_PATH = "contentPath";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private Scheduler scheduler;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public void handleEvent(Event event) {
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
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
        } finally {
            // Always close resource resolvers you open
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private class ImmediateJob implements Runnable {
        private final String path;

        public ImmediateJob(String path) {
            this.path = path;
        }

        @Override
        public void run() {
            log.info("Processing Review Task Asset Move for [ {} ]", this.path);
            ResourceResolver resourceResolver = null;
            try {
                // Always use service users; never admin resource resolvers for "real" code
                resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                // Access data passed into the Job from the Event
                Resource resource = resourceResolver.getResource(path);
                AssetManager assetManager = resourceResolver.adaptTo(AssetManager.class);

                if (resource != null) {
                    ValueMap taskProperties = resource.getValueMap();
                    String contentPath = taskProperties.get(PN_CONTENT_PATH, String.class);

                    if (StringUtils.startsWith(contentPath, PATH_CONTENT_DAM)) {
                        final Iterator<Resource> assets = findAssets(resourceResolver, contentPath);

                        while (assets.hasNext()) {
                            final Asset asset = assets.next().adaptTo(Asset.class);
                            log.debug("Processing asset [ {} ]", asset.getPath());
                            moveAsset(assetManager, asset, taskProperties);
                        }

                        if (resourceResolver.hasChanges()) {
                            resourceResolver.commit();
                        }
                    }
                }
            } catch (LoginException e) {
                log.error("Could not get resource resolver", e);
            } catch (PersistenceException e) {
                log.error("Could not persist changes", e);
            } finally {
                // Always close resource resolvers you open
                if (resourceResolver != null) {
                    resourceResolver.close();
                }
            }
        }

        /**
         * Find all assets under the Task contentPath that have a dam:status of approved or rejected.
         *
         * @param resourceResolver the resource resolver used to find the Assets to move.
         * @param contentPath      the DAM contentPath which the task covers.
         * @return the resources representing dam:Assets for which dam:status is set to approved or rejected
         */
        private Iterator<Resource> findAssets(ResourceResolver resourceResolver, String contentPath) {
            Map<String, String> params = new HashMap<String, String>();
            params.put("type", DamConstants.NT_DAM_ASSET);
            params.put("path", contentPath);
            params.put("property", REL_PN_DAM_STATUS);
            params.put("property.1_value", APPROVED);
            params.put("property.2_value", REJECTED);
            params.put("limit", "-1");

            Query query = queryBuilder.createQuery(PredicateGroup.create(params),
                    resourceResolver.adaptTo(Session.class));

            return query.getResult().getResources();
        }

        /**
         * Move the asset based on the its dam:status (approved or rejected).
         *
         * @param asset          the asset to move
         * @param taskProperties the task properties containing the target onApproveMoveTo and onRejectMoveTo paths
         */
        private void moveAsset(AssetManager assetManager, Asset asset, ValueMap taskProperties) {
            final String status = asset.getMetadataValue(PN_DAM_STATUS);

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
                    assetManager.moveAsset(asset.getPath(), destPath + "/" + asset.getName());
                    log.info("Moved [ {} ] ~> [ {} ] based on approval status [ " + status + " ]",
                            asset.getPath(), destPath + "/" + asset.getName());
                } else {
                    log.warn("Request to move reviewed asset to a non DAM Asset path [ {} ]", destPath);
                }
            }
        }
    }
}