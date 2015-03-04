/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.workflow.bulk.impl;


import com.adobe.acs.commons.workflow.bulk.BulkWorkflowEngine;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.model.WorkflowModel;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.commons.scheduler.Scheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        label = "ACS AEM Commons - Bulk Workflow Engine",
        immediate = true
)
@Service
public class BulkWorkflowEngineImpl implements BulkWorkflowEngine {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowEngineImpl.class);

    private static final int SAVE_THRESHOLD = 1000;

    private static final boolean DEFAULT_AUTO_RESUME = false;
    private boolean autoResume = DEFAULT_AUTO_RESUME;
    @Property(label = "Auto-Resume",
            description = "Stopping the ACS AEM Commons bundle will stop any executing Bulk Workflow processing. "
            + "By default when the bundle starts, it will not attempt to auto-resume stopped via deactivation bulk workflow jobs "
            + "as the query for this may be expensive in very large repositories. ",
            boolValue = DEFAULT_AUTO_RESUME)
    public static final String PROP_AUTO_RESUME = "auto-resume";
    
    @Reference
    private WorkflowService workflowService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ConcurrentHashMap<String, String> jobs = null;

    /**
     * {@inheritDoc}
     */
    @Override
    public final Resource getCurrentBatch(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        final String currentBatch = properties.get(KEY_CURRENT_BATCH, "");
        final Resource currentBatchResource = resource.getResourceResolver().getResource(currentBatch);

        if (currentBatchResource == null) {
            log.error("Current batch resource [ {} ] could not be located. Cannot process Bulk workflow.",
                    currentBatch);
        }
        return currentBatchResource;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void initialize(Resource resource, final ValueMap params) throws
            PersistenceException, RepositoryException {
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        log.trace("Entering initialized");

        if (properties.get(KEY_INITIALIZED, false)) {
            log.warn("Refusing to re-initialize an already initialized Bulk Workflow Manager.");
            return;
        }

        properties.putAll(params);
        properties.put(KEY_JOB_NAME, resource.getPath());

        // Query for all candidate resources

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final Session session = resourceResolver.adaptTo(Session.class);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final QueryResult queryResult = queryManager.createQuery(properties.get(KEY_QUERY, ""),
                Query.JCR_SQL2).execute();
        final NodeIterator nodes = queryResult.getNodes();

        long size = nodes.getSize();
        if (size < 0) {
            log.debug("Using provided estimate total size [ {} ] as actual size [ {} ] could not be retrieved.",
                    properties.get(KEY_ESTIMATED_TOTAL, DEFAULT_ESTIMATED_TOTAL), size);
            size = properties.get(KEY_ESTIMATED_TOTAL, DEFAULT_ESTIMATED_TOTAL);
        }

        final int batchSize = properties.get(KEY_BATCH_SIZE, DEFAULT_BATCH_SIZE);
        final Bucket bucket = new Bucket(batchSize, size,
                resource.getChild(NN_BATCHES).getPath(), "sling:Folder");

        final String relPath = params.get(KEY_RELATIVE_PATH, "");

        // Create the structure
        String currentBatch = null;
        int total = 0;
        Node previousBatchNode = null, batchItemNode = null;
        while (nodes.hasNext()) {
            Node payloadNode = nodes.nextNode();
            log.debug("nodes has next: {}", nodes.hasNext());

            log.trace("Processing search result [ {} ]", payloadNode.getPath());

            if (StringUtils.isNotBlank(relPath)) {
                if (payloadNode.hasNode(relPath)) {
                    payloadNode = payloadNode.getNode(relPath);
                } else {
                    log.warn("Could not find node at [ {} ]", payloadNode.getPath() + "/" + relPath);
                    continue;
                }
                // No rel path, so use the Query result node as the payload Node
            }

            total++;
            final String batchPath = bucket.getNextPath(resourceResolver);

            if (currentBatch == null) {
                // Set the currentBatch to the first batch folder
                currentBatch = batchPath;
            }

            final String batchItemPath = batchPath + "/" + total;

            batchItemNode = JcrUtil.createPath(batchItemPath, SLING_FOLDER, JcrConstants.NT_UNSTRUCTURED, session,
                    false);

            log.trace("Created batch item path at [ {} ]", batchItemPath);

            JcrUtil.setProperty(batchItemNode, KEY_PATH, payloadNode.getPath());
            log.trace("Added payload [ {} ] for batch item [ {} ]", payloadNode.getPath(), batchItemNode.getPath());

            if (total % batchSize == 0) {
                previousBatchNode = batchItemNode.getParent();
            } else if ((total % batchSize == 1) && previousBatchNode != null) {
                // Set the "next batch" property, so we know what the next batch to process is when
                // the current batch is complete
                JcrUtil.setProperty(previousBatchNode, KEY_NEXT_BATCH, batchItemNode.getParent().getPath());
            }

            if (total % SAVE_THRESHOLD == 0) {
                session.save();
            }
        } // while

        if (total > 0) {
            // Set last batch's "next batch" property to complete so we know we're done
            JcrUtil.setProperty(batchItemNode.getParent(), KEY_NEXT_BATCH, STATE_COMPLETE);

            if (total % SAVE_THRESHOLD != 0) {
                session.save();
            }

            properties.put(KEY_CURRENT_BATCH, currentBatch);
            properties.put(KEY_TOTAL, total);
            properties.put(KEY_INITIALIZED, true);
            properties.put(KEY_STATE, STATE_NOT_STARTED);

            resource.getResourceResolver().commit();

            log.info("Completed initialization of Bulk Workflow Manager");
        } else {
            throw new IllegalArgumentException("Query returned zero results.");
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void start(final Resource resource) {
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        final String jobName = properties.get(KEY_JOB_NAME, String.class);
        final String workflowModel = properties.get(KEY_WORKFLOW_MODEL, String.class);
        final String resourcePath = resource.getPath();
        long interval = properties.get(KEY_INTERVAL, DEFAULT_INTERVAL);

        final Runnable job = new Runnable() {

            private Map<String, String> activeWorkflows = new LinkedHashMap<String, String>();

            public void run() {
                ResourceResolver adminResourceResolver = null;
                Resource contentResource = null;

                try {
                    adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                    activeWorkflows = getActiveWorkflows(adminResourceResolver, activeWorkflows);
                    contentResource = adminResourceResolver.getResource(resourcePath);

                    if (contentResource == null) {
                        log.warn("Bulk workflow process resource [ {} ] could not be found. Removing periodic job.",
                                resourcePath);
                        scheduler.removeJob(jobName);
                    } else if (activeWorkflows.isEmpty()) {
                        // Either the beginning or the end of a batch process
                        activeWorkflows = process(contentResource, workflowModel);
                    } else {
                        log.debug("Workflows for batch [ {} ] are still active.",
                                contentResource.adaptTo(ValueMap.class).get(KEY_CURRENT_BATCH, "Missing batch"));

                        final ValueMap properties = contentResource.adaptTo(ValueMap.class);
                        final int batchTimeout = properties.get(KEY_BATCH_TIMEOUT, 0);

                        final Resource currentBatch = getCurrentBatch(contentResource);
                        final ModifiableValueMap currentBatchProperties =
                                currentBatch.adaptTo(ModifiableValueMap.class);

                        final int batchTimeoutCount = currentBatchProperties.get(KEY_BATCH_TIMEOUT_COUNT, 0);

                        if (batchTimeoutCount >= batchTimeout) {
                            terminateActiveWorkflows(adminResourceResolver,
                                    contentResource,
                                    activeWorkflows);
                            // Next batch will be pulled on next iteration
                        } else {
                            // Still withing batch processing range; continue.
                            currentBatchProperties.put(KEY_BATCH_TIMEOUT_COUNT, batchTimeoutCount + 1);
                            adminResourceResolver.commit();
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing periodic execution: {}", e.getMessage());

                    try {
                        if (contentResource != null) {
                            stop(contentResource, STATE_STOPPED_ERROR);
                        } else {
                            scheduler.removeJob(jobName);
                            log.error("Removed scheduled job [ {} ] due to errors content resource [ {} ] could not "
                                    + "be found.", jobName, resourcePath);
                        }
                    } catch (Exception ex) {
                        scheduler.removeJob(jobName);
                        log.error("Removed scheduled job [ {} ] due to errors and could not stop normally.", jobName);
                    }
                } finally {
                    if (adminResourceResolver != null) {
                        adminResourceResolver.close();
                    }
                }
            }
        };

        try {
            final boolean canRunConcurrently = false;
            scheduler.addPeriodicJob(jobName, job, null, interval, canRunConcurrently);
            jobs.put(resource.getPath(), jobName);

            log.debug("Added tracking for job [ {} , {} ]", resource.getPath(), jobName);
            log.info("Periodic job added for [ {} ] every [ {} seconds ]", jobName, interval);

            properties.put(KEY_STATE, STATE_RUNNING);
            properties.put(KEY_STARTED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();

        } catch (Exception e) {
            log.error("Error starting bulk workflow management. {}", e.getMessage());
        }

        log.info("Completed starting of Bulk Workflow Manager");
    }

    /**
     * Stops the bulk workflow process using the user initiated stop state.
     *
     * @param resource the jcr:content configuration resource
     * @throws PersistenceException
     */
    @Override
    public final void stop(final Resource resource) throws PersistenceException {
        this.stop(resource, STATE_STOPPED);
    }

    /**
     * Stops the bulk workflow process using the OSGi Component deactivated stop state.
     * <p>
     * Allows the system to know to resume this when the OSGi Component is activated.
     *
     * @param resource the jcr:content configuration resource
     * @throws PersistenceException
     */
    private void stopDeactivate(final Resource resource) throws PersistenceException {
        this.stop(resource, STATE_STOPPED_DEACTIVATED);
    }

    /**
     * Stops the bulk workflow process.
     *
     * @param resource the jcr:content configuration resource
     * @throws PersistenceException
     */
    private void stop(final Resource resource, final String state) throws PersistenceException {
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        final String jobName = properties.get(KEY_JOB_NAME, String.class);

        log.debug("Stopping job [ {} ]", jobName);

        if (StringUtils.isNotBlank(jobName)) {
            scheduler.removeJob(jobName);
            jobs.remove(resource.getPath());

            log.info("Bulk Workflow Manager stopped for [ {} ]", jobName);

            properties.put(KEY_STATE, state);
            properties.put(KEY_STOPPED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();
        } else {
            log.error("Trying to stop a job without a name from Bulk Workflow Manager resource [ {} ]",
                    resource.getPath());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void resume(final Resource resource) {
        this.start(resource);
        log.info("Resumed bulk workflow for [ {} ]", resource.getPath());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void resume(final Resource resource, final long interval) throws PersistenceException {
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        properties.put(KEY_INTERVAL, interval);
        resource.getResourceResolver().commit();

        this.start(resource);
        log.info("Resumed bulk workflow for [ {} ] with new interval [ {} ]", resource.getPath(), interval);
    }

    /**
     * Updates the bulk workflow process status to be completed.
     *
     * @param resource the jcr:content configuration resource
     * @throws PersistenceException
     */
    private void complete(final Resource resource) throws PersistenceException {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        final String jobName = mvm.get(KEY_JOB_NAME, String.class);

        if (StringUtils.isNotBlank(jobName)) {
            scheduler.removeJob(jobName);

            log.info("Bulk Workflow Manager completed for [ {} ]", jobName);

            mvm.put(KEY_STATE, STATE_COMPLETE);
            mvm.put(KEY_COMPLETED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();
        } else {
            log.error("Trying to complete a job without a name from Bulk Workflow Manager resource [ {} ]",
                    resource.getPath());
        }
    }

    /**
     * Processes the bulk process workflow batch; starts WF's as necessary for each batch item.
     *
     * @param resource the jcr:content configuration resource
     * @throws PersistenceException
     */
    private Map<String, String> process(final Resource resource, String workflowModel) throws
            WorkflowException, PersistenceException, RepositoryException {

        // This method can be invoked by the very first processing of a batch node, or when the batch is complete

        if (log.isDebugEnabled()) {
            log.debug("Processing batch [ {} ] with workflow model [ {} ]", this.getCurrentBatch(resource).getPath(),
                    workflowModel);
        }

        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(session);
        final WorkflowModel model = workflowSession.getModel(workflowModel);

        final Map<String, String> workflowMap = new LinkedHashMap<String, String>();

        final Resource currentBatch = this.getCurrentBatch(resource);
        final ModifiableValueMap currentProperties = currentBatch.adaptTo(ModifiableValueMap.class);

        Resource batchToProcess;
        if (currentProperties.get(KEY_STARTED_AT, Date.class) == null) {
            currentProperties.put(KEY_STARTED_AT, Calendar.getInstance());
            batchToProcess = currentBatch;
            log.debug("Virgin batch [ {} ]; preparing to initiate WF.", currentBatch.getPath());
        } else {
            batchToProcess = this.advance(resource);
            log.debug("Completed batch [ {} ]; preparing to advance and initiate WF on next batch [ {} ].",
                    currentBatch.getPath(), batchToProcess);
        }

        if (batchToProcess != null) {
            for (final Resource child : batchToProcess.getChildren()) {
                final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);

                final String state = properties.get(KEY_STATE, "");
                final String payloadPath = properties.get(KEY_PATH, String.class);

                if (StringUtils.isBlank(state)
                        && StringUtils.isNotBlank(payloadPath)) {

                    // Don't try to restart already processed batch items

                    final Workflow workflow = workflowSession.startWorkflow(model,
                            workflowSession.newWorkflowData("JCR_PATH", payloadPath));
                    properties.put(KEY_WORKFLOW_ID, workflow.getId());
                    properties.put(KEY_STATE, workflow.getState());

                    workflowMap.put(child.getPath(), workflow.getId());
                }
            }
        } else {
            log.error("Cant find the current batch to process.");
        }

        resource.getResourceResolver().commit();

        log.debug("Bulk workflow batch tracking map: {}", workflowMap);
        return workflowMap;
    }

    /**
     * Advance to the next batch and update all properties on the current and next batch nodes accordingly.
     * <p>
     * This method assumes the current batch has been verified as complete.
     *
     * @param resource the bulk workflow manager content resource
     * @return the next batch resource to process
     * @throws PersistenceException
     * @throws RepositoryException
     */
    private Resource advance(final Resource resource) throws PersistenceException, RepositoryException {
        // Page Resource
        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);
        final boolean autoCleanupWorkflow = properties.get(KEY_PURGE_WORKFLOW, true);

        // Current Batch
        final Resource currentBatch = this.getCurrentBatch(resource);
        final ModifiableValueMap currentProperties = currentBatch.adaptTo(ModifiableValueMap.class);

        if (autoCleanupWorkflow) {
            this.purge(currentBatch);
        }

        currentProperties.put(KEY_COMPLETED_AT, Calendar.getInstance());

        // Next Batch
        final String nextBatchPath = currentProperties.get(KEY_NEXT_BATCH, STATE_COMPLETE);

        if (StringUtils.equalsIgnoreCase(nextBatchPath, STATE_COMPLETE)) {

            // Last batch

            this.complete(resource);

            properties.put(KEY_COMPLETE_COUNT,
                    properties.get(KEY_COMPLETE_COUNT, 0) + this.getSize(currentBatch.getChildren()));

            return null;

        } else {

            // Not the last batch

            final Resource nextBatch = resourceResolver.getResource(nextBatchPath);
            final ModifiableValueMap nextProperties = nextBatch.adaptTo(ModifiableValueMap.class);

            currentProperties.put(KEY_STATE, STATE_COMPLETE);
            currentProperties.put(KEY_COMPLETED_AT, Calendar.getInstance());

            nextProperties.put(KEY_STATE, STATE_RUNNING);
            nextProperties.put(KEY_STARTED_AT, Calendar.getInstance());

            properties.put(KEY_CURRENT_BATCH, nextBatch.getPath());
            properties.put(KEY_COMPLETE_COUNT,
                    properties.get(KEY_COMPLETE_COUNT, 0) + this.getSize(currentBatch.getChildren()));

            return nextBatch;
        }
    }

    /**
     * Deletes the Workflow instances for each item in the batch.
     *
     * @param batchResource the batch resource
     * @return the number of workflow instances purged
     * @throws RepositoryException
     */
    private int purge(Resource batchResource) throws RepositoryException {
        final ResourceResolver resourceResolver = batchResource.getResourceResolver();
        final List<String> payloadPaths = new ArrayList<String>();

        for (final Resource child : batchResource.getChildren()) {
            final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);
            final String workflowId = properties.get(KEY_WORKFLOW_ID, "Missing WorkflowId");
            final String path = properties.get(KEY_PATH, "Missing Path");

            final Resource resource = resourceResolver.getResource(workflowId);
            if (resource != null) {
                final Node node = resource.adaptTo(Node.class);
                node.remove();
                payloadPaths.add(path);
            } else {
                log.warn("Could not find workflowId at [ {} ] to purge.", workflowId);
            }
        }

        if (payloadPaths.size() > 0) {
            resourceResolver.adaptTo(Session.class).save();
            log.info("Purged {} workflow instances for payloads: {}",
                    payloadPaths.size(),
                    Arrays.toString(payloadPaths.toArray(new String[payloadPaths.size()])));
        }

        return payloadPaths.size();
    }

    /**
     * Retrieves the active worklfows for the batch.
     *
     * @param resourceResolver the resource resolver
     * @param workflowMap      the map tracking what batch items are under WF
     * @return the updated map of which batch items and their workflow state
     * @throws RepositoryException
     * @throws PersistenceException
     */
    private Map<String, String> getActiveWorkflows(ResourceResolver resourceResolver,
                                                   final Map<String, String> workflowMap)
            throws RepositoryException, PersistenceException {

        final Map<String, String> activeWorkflowMap = new LinkedHashMap<String, String>();
        final WorkflowSession workflowSession =
                workflowService.getWorkflowSession(resourceResolver.adaptTo(Session.class));

        boolean dirty = false;
        for (final Map.Entry<String, String> entry : workflowMap.entrySet()) {
            final String workflowId = entry.getValue();

            final Workflow workflow;
            try {
                workflow = workflowSession.getWorkflow(workflowId);
                if (workflow.isActive()) {
                    activeWorkflowMap.put(entry.getKey(), workflow.getId());
                }

                final Resource resource = resourceResolver.getResource(entry.getKey());
                final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

                if (!StringUtils.equals(mvm.get(KEY_STATE, String.class), workflow.getState())) {
                    mvm.put(KEY_STATE, workflow.getState());
                    dirty = true;
                }
            } catch (WorkflowException e) {
                log.error("Could not get workflow with id [ {} ]. {}", workflowId, e.getMessage());
            }
        }

        if (dirty) {
            resourceResolver.commit();
        }

        return activeWorkflowMap;
    }


    /**
     * Terminate active workflows.
     *
     * @param resourceResolver
     * @param contentResource
     * @param workflowMap
     * @return number of terminated workflows
     * @throws RepositoryException
     * @throws PersistenceException
     */
    private int terminateActiveWorkflows(ResourceResolver resourceResolver,
                                         final Resource contentResource,
                                         final Map<String, String> workflowMap)
            throws RepositoryException, PersistenceException {

        final WorkflowSession workflowSession =
                workflowService.getWorkflowSession(resourceResolver.adaptTo(Session.class));

        boolean dirty = false;
        int count = 0;
        for (final Map.Entry<String, String> entry : workflowMap.entrySet()) {
            final String workflowId = entry.getValue();

            final Workflow workflow;
            try {
                workflow = workflowSession.getWorkflow(workflowId);
                if (workflow.isActive()) {

                    workflowSession.terminateWorkflow(workflow);

                    count++;

                    log.info("Terminated workflow [ {} ]", workflowId);

                    final Resource resource = resourceResolver.getResource(entry.getKey());
                    final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

                    mvm.put(KEY_STATE, STATE_FORCE_TERMINATED.toUpperCase());

                    dirty = true;
                }

            } catch (WorkflowException e) {
                log.error("Could not get workflow with id [ {} ]. {}", workflowId, e.getMessage());
            }
        }

        if (dirty) {
            final ModifiableValueMap properties = contentResource.adaptTo(ModifiableValueMap.class);
            properties.put(KEY_FORCE_TERMINATED_COUNT,
                    properties.get(KEY_FORCE_TERMINATED_COUNT, 0) + count);
            resourceResolver.commit();
        }

        return count;
    }


    /**
     * Gets the size of an iterable; Used for getting number of items under a batch.
     *
     * @param theIterable the iterable to count
     * @return number of items in the iterable
     */
    private int getSize(Iterable<?> theIterable) {
        int count = 0;

        final Iterator<?> iterator = theIterable.iterator();
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }

        return count;
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        this.jobs = new ConcurrentHashMap<String, String>();

        this.autoResume = PropertiesUtil.toBoolean(config.get(PROP_AUTO_RESUME), DEFAULT_AUTO_RESUME);

        if (this.autoResume) {
            log.info("Looking for any Bulk Workflow Manager pages to resume processing.");
            
            ResourceResolver adminResourceResolver = null;

            try {
                adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

                final String query = "SELECT * FROM [cq:PageContent] WHERE [sling:resourceType] = "
                        + "'" + SLING_RESOURCE_TYPE + "'"
                        + " AND [" + KEY_STATE + "] = '" + STATE_STOPPED_DEACTIVATED + "'";

                log.debug("Finding bulk work flows to reactivate using query: {}", query);

                final Iterator<Resource> resources = adminResourceResolver.findResources(query, Query.JCR_SQL2);

                while (resources.hasNext()) {
                    final Resource resource = resources.next();
                    log.info("Automatically resuming bulk workflow at [ {} ]", resource.getPath());
                    this.resume(resource);
                }
            } catch (LoginException e) {
                log.error("Could not obtain resource resolver for finding stopped Bulk Workflow jobs", e);
            } finally {
                if (adminResourceResolver != null) {
                    adminResourceResolver.close();
                }
            }
        }
    }

    @Deactivate
    protected final void deactivate(final Map<String, String> config) {
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            for (final Map.Entry<String, String> entry : jobs.entrySet()) {
                final String path = entry.getKey();
                final String jobName = entry.getValue();

                log.debug("Stopping scheduled job at resource [ {} ] and job name [ {} ] by way of de-activation",
                        path, jobName);

                try {
                    this.stopDeactivate(resourceResolver.getResource(path));
                } catch (Exception e) {
                    this.scheduler.removeJob(jobName);
                    jobs.remove(path);
                    log.error("Performed a hard stop for [ {} ] at de-activation due to: ", jobName, e.getMessage());
                }
            }
        } catch (org.apache.sling.api.resource.LoginException e) {
            log.error("Could not acquire a resource resolver: {}", e.getMessage());
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }
}