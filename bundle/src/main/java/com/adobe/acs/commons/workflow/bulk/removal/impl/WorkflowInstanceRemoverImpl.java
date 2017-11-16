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
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalException;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalForceQuitException;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalMaxDurationExceededException;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalStatus;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.filter.WorkItemFilter;
import com.day.cq.workflow.WorkflowService;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.event.jobs.JobManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Pattern;

/**
 * ACS AEM Commons - Workflow Instance Remover
 */
@Component
@Service
public final class WorkflowInstanceRemoverImpl implements WorkflowInstanceRemover {
    private static final Logger log = LoggerFactory.getLogger(WorkflowInstanceRemoverImpl.class);

    private static final String WORKFLOW_FOLDER_FORMAT = "YYYY-MM-dd";

    private static final String PN_MODEL_ID = "modelId";

    private static final String PN_STARTED_AT = "startedAt";

    private static final String PN_STATUS = "status";

    private static final String PAYLOAD_PATH = "data/payload/path";

    private static final String NT_SLING_FOLDER = "sling:Folder";

    private static final String NT_CQ_WORKFLOW = "cq:Workflow";

    private static final String JOB_SEPARATOR = "_,_";

    private static final Pattern NN_SERVER_FOLDER_PATTERN = Pattern.compile("server\\d+");

    private static final Pattern NN_DATE_FOLDER_PATTERN = Pattern.compile("\\d{4}-\\d{2}-\\d{2}.*");
    
    private static final int BATCH_SIZE = 1000;

    private static final int MAX_SAVE_RETRIES = 5;

    private static final long MS_IN_ONE_MINUTE = 60000;
    private static final String STATUS_RUNNING = "RUNNING";

    private final AtomicReference<WorkflowRemovalStatus> status
            = new AtomicReference<WorkflowRemovalStatus>();

    private final AtomicBoolean forceQuit = new AtomicBoolean(false);

    @Reference
    private JobManager jobManager;

    @Reference
    private WorkflowService workflowService;

    /**
     * {@inheritDoc}
     */
    @Override
    public WorkflowRemovalStatus getStatus() {
        return this.status.get();
    }

    private String getStatus(Resource workflowInstanceResource) {
        String instanceStatus = workflowInstanceResource.getValueMap().get(PN_STATUS, "UNKNOWN");

        if (!STATUS_RUNNING.equalsIgnoreCase(instanceStatus)) {
            log.debug("Status of [ {} ] is not RUNNING, so we can take it at face value", instanceStatus);
            return instanceStatus;
        }

        // Else check if its RUNNING or STALE
        log.debug("Status is [ {} ] so we have to determine if its RUNNING or STALE", instanceStatus);

        Resource metadataResource = workflowInstanceResource.getChild("data/metaData");
        if (metadataResource == null) {
            log.debug("Workflow instance data/metaData does not exist for [ {} ]", workflowInstanceResource.getPath());
            return instanceStatus;
        }

        final ValueMap properties = metadataResource.getValueMap();
        final String[] jobIds = StringUtils.splitByWholeSeparator(properties.get("currentJobs", ""), JOB_SEPARATOR);

        if (jobIds.length == 0) {
            log.debug("No jobs found for [ {} ] so assuming status as [ {} ]", workflowInstanceResource.getPath(), instanceStatus);
        }

        // Make sure there are no JOBS that match to this jobs name
        for (final String jobId : jobIds) {
            if (jobManager.getJobById(jobId) != null) {
                // Found a job for this jobID; so this is a RUNNING job
                log.debug("JobManager found a job for jobId [ {} ] so marking workflow instances [ {} ] as RUNNING", jobId, workflowInstanceResource.getPath());
                return STATUS_RUNNING;
            }
        }
        log.debug("JobManager could not find any jobs for jobIds [ {} ] so  workflow instance [ {} ] is potentially STALE", StringUtils.join(jobIds, ", "), workflowInstanceResource.getPath());

        final WorkflowSession workflowSession = workflowInstanceResource.getResourceResolver().adaptTo(WorkflowSession.class);
        Workflow workflow = null;
        try {
            workflow = workflowSession.getWorkflow(workflowInstanceResource.getPath());
            if (workflow == null) {
                throw new WorkflowException(String.format("Workflow instance object is null for [ %s]", workflowInstanceResource.getPath()));
            }
        } catch (WorkflowException e) {
            log.warn("Unable to locate Workflow Instance for [ {} ] So it cannot be RUNNING and must be STALE. ", workflowInstanceResource.getPath(), e);
            return "STALE";
        }

        final List<WorkItem> workItems = workflow.getWorkItems(new WorkItemFilter() {
            public boolean doInclude(WorkItem workItem) {
                // Only include active Workflow instances (ones without an End Time) in this list
                return workItem.getTimeEnded() == null;
            }
        });

        if (!workItems.isEmpty()) {
            // If at least 1 work item exists that does not have an end time (meaning its still active), then its RUNNING
            return STATUS_RUNNING;
        }

        return "STALE";
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void forceQuit() {
        this.forceQuit.set(true);
    }

    /**
     * {@inheritDoc}
     */
    public int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                       final Collection<String> modelIds,
                                       final Collection<String> statuses,
                                       final Collection<Pattern> payloads,
                                       final Calendar olderThan)
            throws PersistenceException, WorkflowRemovalException, InterruptedException, WorkflowRemovalForceQuitException {
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
            throws PersistenceException, WorkflowRemovalException, InterruptedException, WorkflowRemovalForceQuitException {
        return removeWorkflowInstances(resourceResolver, modelIds, statuses, payloads, olderThan, batchSize, -1);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings({"squid:S3776", "squid:S1141"})
    public int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                       final Collection<String> modelIds,
                                       final Collection<String> statuses,
                                       final Collection<Pattern> payloads,
                                       final Calendar olderThan,
                                       final int batchSize,
                                       final int maxDurationInMins)
            throws PersistenceException, WorkflowRemovalException, InterruptedException, WorkflowRemovalForceQuitException {

        final long start = System.currentTimeMillis();
        long end = -1;

        int count = 0;
        int checkedCount = 0;
        int workflowRemovedCount = 0;

        if (maxDurationInMins > 0) {
            // Max duration has been requested (greater than 0)

            // Convert minutes to milliseconds
            long maxDurationInMs = maxDurationInMins * MS_IN_ONE_MINUTE;

            // Compute the end time
            end = start + maxDurationInMs;
        }
        
        try {
            this.start(resourceResolver);

            final List<Resource> containerFolders = this.getWorkflowInstanceFolders(resourceResolver);

            for (Resource containerFolder : containerFolders) {
                log.debug("Checking [ {} ] for workflow instances to remove", containerFolder.getPath());

                final Collection<Resource> sortedFolders = this.getSortedAndFilteredFolders(containerFolder);

                for (final Resource folder : sortedFolders) {

                    int remaining = 0;

                    for (final Resource instance : folder.getChildren()) {

                        if (this.forceQuit.get()) {
                            throw new WorkflowRemovalForceQuitException();
                        } else if (end > 0 && System.currentTimeMillis() >= end) {
                            throw new WorkflowRemovalMaxDurationExceededException();
                        }

                        final ValueMap properties = instance.getValueMap();

                        if (!StringUtils.equals(NT_CQ_WORKFLOW,
                                properties.get(JcrConstants.JCR_PRIMARYTYPE, String.class))) {

                            // Only process cq:Workflow's
                            remaining++;
                            continue;
                        }

                        checkedCount++;

                        final String instanceStatus = getStatus(instance);
                        final String model = properties.get(PN_MODEL_ID, String.class);
                        final Calendar startTime = properties.get(PN_STARTED_AT, Calendar.class);
                        final String payload = properties.get(PAYLOAD_PATH, String.class);

                        if (StringUtils.isBlank(payload)) {
                            log.warn("Unable to find payload for Workflow instance [ {} ]", instance.getPath());
                            remaining++;
                            continue;
                        } else if (CollectionUtils.isNotEmpty(statuses) && !statuses.contains(instanceStatus)) {
                            log.trace("Workflow instance [ {} ] has non-matching status of [ {} ]", instance.getPath(), instanceStatus);
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
                                log.debug("Removed workflow instance at [ {} ]", instance.getPath());

                                workflowRemovedCount++;
                                count++;
                            } catch (RepositoryException e) {
                                log.error("Could not remove workflow instance at [ {} ]. Continuing...",
                                        instance.getPath(), e);
                            }

                            if (count % batchSize == 0) {
                                this.batchComplete(resourceResolver, checkedCount, workflowRemovedCount);

                                log.info("Removed a running total of [ {} ] workflow instances", count);
                            }
                        }
                    }

                    if (remaining == 0
                            && isWorkflowDatedFolder(folder)
                            && !StringUtils.startsWith(folder.getName(), new SimpleDateFormat(WORKFLOW_FOLDER_FORMAT).format(new Date()))) {
                        // Dont remove folders w items and dont remove any of "today's" folders
                        // MUST match the YYYY-MM-DD(.*) pattern; do not try to remove root folders
                        try {
                            folder.adaptTo(Node.class).remove();
                            log.debug("Removed empty workflow folder node [ {} ]", folder.getPath());
                            // Incrementing only count to trigger batch save and not total since is not a WF
                            count++;
                        } catch (RepositoryException e) {
                            log.error("Could not remove workflow folder at [ {} ]", folder.getPath(), e);
                        }
                    }
                }

                // Save final batch if needed, and update tracking nodes
                this.complete(resourceResolver, checkedCount, workflowRemovedCount);
            }

        } catch (PersistenceException e) {
            this.forceQuit.set(false);
            log.error("Error persisting changes with Workflow Removal", e);
            this.error();
            throw e;
        } catch (WorkflowRemovalException e) {
            this.forceQuit.set(false);
            log.error("Error with Workflow Removal", e);
            this.error();
            throw e;
        } catch (InterruptedException e) {
            this.forceQuit.set(false);
            log.error("Errors in persistence retries during Workflow Removal", e);
            this.error();
            throw e;
        }  catch (WorkflowRemovalForceQuitException e) {
            this.forceQuit.set(false);
            // Uncommon instance of using Exception to control flow; Force quitting is an extreme condition.
            log.warn("Workflow removal was force quit. The removal state is unknown.");
            this.internalForceQuit();
            throw e;
        }  catch (WorkflowRemovalMaxDurationExceededException e) {
            // Uncommon instance of using Exception to control flow; Exceeding max duration extreme condition.
            log.warn("Workflow removal exceeded max duration of [ {} ] minutes. Final removal commit initiating...", maxDurationInMins);
            this.complete(resourceResolver, checkedCount, count);
        }

        if (log.isInfoEnabled()) {
            log.info("Workflow Removal Process Finished! "
                    + "Removed a total of [ {} ] workflow instances in [ {} ] ms",
                    count,
                    System.currentTimeMillis() - start);
        }

        return count;
    }

    private Collection<Resource> getSortedAndFilteredFolders(Resource folderResource) {
        final Collection<Resource> sortedCollection = new TreeSet(new WorkflowInstanceFolderComparator());
        for (Resource folder : folderResource.getChildren()) {
            // Only process sling:Folders; eg. skip rep:Policy, serverN folders
            if (folder.isResourceType(NT_SLING_FOLDER) && !isWorkflowServerFolder(folder)) {
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
        // Ensure forceQuit does not have a left-over value when starting a new run
        this.forceQuit.set(false);

        boolean running = false;

        WorkflowRemovalStatus localStatus = this.getStatus();
        
        if(localStatus != null) {
            running = localStatus.isRunning();
        }
        
        if (running) {
            log.warn("Unable to start workflow instance removal; Workflow removal already running.");
            
            throw new WorkflowRemovalException("Workflow removal already started by "
                    + this.getStatus().getInitiatedBy());
        } else {
            this.status.set(new WorkflowRemovalStatus(resourceResolver));
            log.info("Starting workflow instance removal");
        }
    }

    private void batchComplete(final ResourceResolver resourceResolver, final int checked, final int count) throws
            PersistenceException, InterruptedException {

        this.save(resourceResolver);
        
        WorkflowRemovalStatus removalStatus = this.status.get();
        
        removalStatus.setChecked(checked);
        removalStatus.setRemoved(count);
        
        this.status.set(removalStatus);
    }

    private void complete(final ResourceResolver resourceResolver, final int checked, final int count) throws
            PersistenceException, InterruptedException {

        this.save(resourceResolver);

        WorkflowRemovalStatus removalStatus = this.status.get();

        removalStatus.setRunning(false);
        removalStatus.setChecked(checked);
        removalStatus.setRemoved(count);
        removalStatus.setCompletedAt(Calendar.getInstance());
        
        this.status.set(removalStatus);
    }

    private void error() throws
            PersistenceException, InterruptedException {

        WorkflowRemovalStatus removalStatus = this.status.get();

        removalStatus.setRunning(false);
        removalStatus.setErredAt(Calendar.getInstance());
        
        this.status.set(removalStatus);
    }

    private void internalForceQuit() {
        WorkflowRemovalStatus removalStatus = this.status.get();

        removalStatus.setRunning(false);
        removalStatus.setForceQuitAt(Calendar.getInstance());

        this.status.set(removalStatus);

        // Reset force quit flag
        this.forceQuit.set(false);
    }

    private List<Resource> getWorkflowInstanceFolders(final ResourceResolver resourceResolver) {
        final List<Resource> folders = new ArrayList<Resource>();
        final Resource root = resourceResolver.getResource(WORKFLOW_INSTANCES_PATH);
        final Iterator<Resource> itr = root.listChildren();

        boolean addedRoot = false;

        while (itr.hasNext()) {
            Resource resource = itr.next();

            if (isWorkflowServerFolder(resource)) {
                folders.add(resource);
            } else if (!addedRoot && isWorkflowDatedFolder(resource)) {
                folders.add(root);
                addedRoot = true;
            }
        }

        if (folders.isEmpty()) {
            folders.add(root);
        }

        return folders;
    }

    private boolean isWorkflowDatedFolder(final Resource resource) {
        return NN_DATE_FOLDER_PATTERN.matcher(resource.getName()).matches();
    }

    private boolean isWorkflowServerFolder(final Resource folder) {
        return NN_SERVER_FOLDER_PATTERN.matcher(folder.getName()).matches();
    }

    @Activate
    @Deactivate
    protected void reset(Map<String, Object> config) {
        this.forceQuit.set(false);
    }
}
