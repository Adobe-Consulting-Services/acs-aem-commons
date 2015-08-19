/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.bulk.removal.impl;

import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * ACS AEM Commons - Workflow Instance Remover
 */
@Component
@Service
public final class WorkflowInstanceRemoverImpl implements WorkflowInstanceRemover {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceRemoverImpl.class);

    private static final SimpleDateFormat WORKFLOW_FOLDER_FORMAT = new SimpleDateFormat("YYYY-MM-dd");

    private static final String PN_PAYLOAD_PATH = "data/payload/path";

    private static final String NT_SLING_FOLDER = "sling:Folder";

    private static final String NT_CQ_WORKFLOW = "cq:Workflow";

    private static final int BATCH_SIZE = 1000;

    private static final int MAX_SAVE_RETRIES = 5;

    /**
     * {@inheritDoc}
     */
    public int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                       final Collection<String> modelIds,
                                       final Collection<String> statuses,
                                       final Collection<Pattern> payloads,
                                       final Calendar olderThan)
            throws PersistenceException, WorkflowRemovalException, InterruptedException {
        return removeWorkflowInstances(resourceResolver, modelIds, statuses, payloads, olderThan, BATCH_SIZE);
    }

    /**
     * {@inheritDoc}
     */
    public int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                       final Collection<String> modelIds,
                                       final Collection<String> statuses,
                                       final Collection<Pattern> payloads,
                                       final Calendar olderThan,
                                       final int batchSize)
            throws PersistenceException, WorkflowRemovalException, InterruptedException {

        try {
            final long start = System.currentTimeMillis();

            this.start(resourceResolver);

            final Resource folders = resourceResolver.getResource(WORKFLOW_INSTANCES_PATH);
            final Collection<Resource> sortedFolders = this.getSortedAndFilteredFolders(folders);

            int count = 0;
            int checkedCount = 0;
            int workflowRemovedCount = 0;

            for (final Resource folder : sortedFolders) {

                int remaining = 0;

                for (final Resource instance : folder.getChildren()) {
                    final ValueMap properties = instance.getValueMap();

                    if (!StringUtils.equals(NT_CQ_WORKFLOW,
                            properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {

                        // Only process cq:Workflow's
                        remaining++;
                        continue;
                    }

                    checkedCount++;

                    final String status = properties.get(PN_STATUS, String.class);
                    final String model = properties.get(PN_MODEL_ID, String.class);
                    final Calendar startTime = properties.get(PN_STARTED_AT, Calendar.class);
                    final String payload = properties.get(PN_PAYLOAD_PATH, String.class);

                    if (CollectionUtils.isNotEmpty(statuses) && !statuses.contains(status)) {
                        log.trace("Workflow instance [ {} ] has non-matching status of [ {} ]", instance.getPath(), status);
                        remaining++;
                        continue;
                    } else if (CollectionUtils.isNotEmpty(modelIds) && !modelIds.contains(model)) {
                        log.trace("Workflow instance [ {} ] has non-matching model of [ {} ]", instance.getPath(), model);
                        remaining++;
                        continue;
                    } else if (olderThan != null && startTime != null && startTime.before(olderThan)) {
                        log.trace("Workflow instance [ {} ] has non-matching start time of [ {} ]", instance.getPath(),
                                startTime);
                        remaining++;
                        continue;
                    } else {

                        if (CollectionUtils.isNotEmpty(payloads)) {
                            // Only evaluate payload patterns if they are provided

                            boolean match = false;

                            if (StringUtils.isNotEmpty(payload)) {
                                for (final Pattern pattern : payloads) {
                                    if (payload.matches(pattern.pattern())) {
                                        // payload matches a pattern
                                        match = true;
                                        break;
                                    }
                                }

                                if (!match) {
                                    // Not a match; skip to next workflow instance
                                    log.trace("Workflow instance [ {} ] has non-matching payload path [ {} ]",
                                            instance.getPath(), payload);
                                    remaining++;
                                    continue;
                                }
                            }
                        }

                        // Only remove matching

                        try {
                            instance.adaptTo(Node.class).remove();
                            log.trace("Removed workflow instance at [ {} ]", instance.getPath());

                            workflowRemovedCount++;
                            count++;
                        } catch (RepositoryException e) {
                            log.error("Could not remove workflow instance at [ {} ]. Continuing...",
                                    instance.getPath(), e);
                        }

                        if (count % batchSize == 0) {
                            this.batchComplete(resourceResolver, checkedCount, workflowRemovedCount);

                            log.debug("Removed a running total of [ {} ] workflow instances", count);
                        }
                    }
                }

                if (remaining == 0
                        && !StringUtils.startsWith(folder.getName(), WORKFLOW_FOLDER_FORMAT.format(new Date()))) {
                    // Dont remove folders w items and dont remove any of "today's" folders
                    try {
                        folder.adaptTo(Node.class).remove();
                        // Incrementing only count to trigger batch save and not total since is not a WF
                        count++;
                    } catch (RepositoryException e) {
                        log.error("Could not remove workflow folder at [ {} ]", folder.getPath(), e);
                    }
                }
            }

            // Save final batch if needed, and update tracking nodes
            this.complete(resourceResolver, checkedCount, workflowRemovedCount);

            log.info("Removed a total of [ {} ] workflow instances in [ {} ] ms", count,
                    System.currentTimeMillis() - start);

            return count;

        } catch (PersistenceException e) {
            log.error("Error persisting changes with Workflow Removal", e);
            this.error(resourceResolver);
            throw e;
        } catch (WorkflowRemovalException e) {
            log.error("Error with Workflow Removal", e);
            this.error(resourceResolver);
            throw e;
        } catch (InterruptedException e) {
            log.error("Errors in persistence retries during Workflow Removal", e);
            this.error(resourceResolver);
            throw e;
        }
    }

    private Collection<Resource> getSortedAndFilteredFolders(Resource folderResource) {
        final Collection<Resource> sortedCollection = new TreeSet(new WorkflowInstanceFolderComparator());
        final Iterator<Resource> folders = folderResource.listChildren();

        while (folders.hasNext()) {
            final Resource folder = folders.next();

            if (!folder.isResourceType(NT_SLING_FOLDER)) {
                // Only process sling:Folders; eg. skip rep:Policy
                continue;
            } else {
                sortedCollection.add(folder);
            }
        }

        return sortedCollection;
    }

    private void save(ResourceResolver resourceResolver) throws PersistenceException, InterruptedException {
        int count = 0;

        while (count++ <= MAX_SAVE_RETRIES) {
            try {
                if (resourceResolver.hasChanges()) {
                    final long start = System.currentTimeMillis();
                    resourceResolver.commit();
                    log.debug("Saving batch workflow instance removal in [ {} ] ms", System.currentTimeMillis() - start);
                }

                // No changes or save did not error; return from loop
                return;
            } catch (PersistenceException ex) {
                if (count <= MAX_SAVE_RETRIES) {
                    // If error occurred within bounds of retries, keep retrying
                    resourceResolver.refresh();
                    log.warn("Could not persist Workflow Removal changes, trying again in {} ms", 1000 * count);
                    Thread.sleep(1000 * count);
                } else {
                    // If error occurred outside bounds of returns, throw the exception to exist this method
                    throw ex;
                }
            }
        }
    }

    private void start(final ResourceResolver resourceResolver) throws PersistenceException, WorkflowRemovalException, InterruptedException {

        final Resource resource = resourceResolver.getResource(WORKFLOW_REMOVAL_STATUS_PATH);
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        if (StringUtils.equals(Status.RUNNING.toString(), mvm.get(PN_STATUS, String.class))) {
            throw new WorkflowRemovalException("Workflow removal already started by "
                    + mvm.put(PN_INITIATED_BY, "Unknown"));
        } else {

            mvm.put(PN_INITIATED_BY, resourceResolver.getUserID());
            mvm.put(PN_STATUS, StringUtils.lowerCase(Status.RUNNING.toString()));
            mvm.put(PN_STARTED_AT, System.currentTimeMillis());

            mvm.put(PN_COUNT, 0);
            mvm.put(PN_CHECKED_COUNT, 0);

            mvm.remove(PN_COMPLETED_AT);

            this.save(resourceResolver);
        }
    }

    private void batchComplete(final ResourceResolver resourceResolver, final int checked, final int count) throws
            PersistenceException, InterruptedException {
        final Resource resource = resourceResolver.getResource(WORKFLOW_REMOVAL_STATUS_PATH);
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        mvm.put(PN_STATUS, StringUtils.lowerCase(Status.RUNNING.toString()));
        mvm.put(PN_CHECKED_COUNT, checked);
        mvm.put(PN_COUNT, count);

        this.save(resourceResolver);
    }

    private void complete(final ResourceResolver resourceResolver, final int checked, final int count) throws
            PersistenceException, InterruptedException {
        final Resource resource = resourceResolver.getResource(WORKFLOW_REMOVAL_STATUS_PATH);
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        mvm.put(PN_STATUS, StringUtils.lowerCase(Status.COMPLETE.toString()));
        mvm.put(PN_CHECKED_COUNT, checked);
        mvm.put(PN_COUNT, count);
        mvm.put(PN_COMPLETED_AT, System.currentTimeMillis());

        this.save(resourceResolver);
    }

    private void error(final ResourceResolver resourceResolver) throws
            PersistenceException, InterruptedException {
        final Resource resource = resourceResolver.getResource(WORKFLOW_REMOVAL_STATUS_PATH);
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        mvm.put(PN_STATUS, StringUtils.lowerCase(Status.ERROR.toString()));
        mvm.put(PN_CHECKED_COUNT, -1);
        mvm.put(PN_COUNT, -1);
        mvm.put(PN_COMPLETED_AT, System.currentTimeMillis());

        this.save(resourceResolver);
    }
}
