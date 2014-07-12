package com.adobe.acs.commons.workflow.bulk.impl;


import com.adobe.acs.commons.workflow.bulk.Bucket;
import com.adobe.acs.commons.workflow.bulk.BulkWorkflowManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.search.QueryBuilder;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.model.WorkflowModel;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
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
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component(
        immediate = true
)
@Service
public class BulkWorkflowManagerImpl implements BulkWorkflowManager {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflowManagerImpl.class);

    private static final int SAVE_THRESHOLD = 1000;

    @Reference
    QueryBuilder queryBuilder;

    @Reference
    WorkflowService workflowService;

    @Reference
    private Scheduler scheduler;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ConcurrentHashMap<String, String> jobs = null;

    public Resource getCurrentBatch(final Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        final String currentBatch = properties.get(PN_CURRENT_BATCH, "");
        final Resource currentBatchResource = resource.getResourceResolver().getResource(currentBatch);

        if(currentBatchResource == null) {
            log.error("Current batch resource [ {} ] could not be located. Cannot process Bulk workflow.",
                    currentBatch);
        }
        return currentBatchResource;
    }

    public void initialize(Resource resource, String query, long estimatedSize, int batchSize,
                           int interval, String workflowModel) throws
            PersistenceException, RepositoryException {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        if (mvm.get(PN_INITIALIZED, false)) {
            log.warn("Refusing to re-initialize an already initialized Bulk Workflow Manager.");
            return;
        }

        mvm.put(PN_QUERY, query);
        mvm.put(PN_BATCH_SIZE, batchSize);
        mvm.put(PN_WORKFLOW_MODEL, workflowModel);
        mvm.put(PN_JOB_NAME, resource.getPath());
        mvm.put(PN_INTERVAL, interval);
        mvm.put(PN_AUTO_PURGE_WORKFLOW, true);

        // Query for all candidate resources

        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final Session session = resourceResolver.adaptTo(Session.class);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final QueryResult queryResult = queryManager.createQuery(query, Query.JCR_SQL2).execute();
        final NodeIterator nodes = queryResult.getNodes();

        long size = queryResult.getNodes().getSize();
        if (size < 0) {
            log.debug("Using provided estimate total size [ {} ] as actual size [ {} ] could not be retrieved.",
                    estimatedSize, size);
            size = estimatedSize;
        }

        final Bucket bucket = new Bucket(batchSize, size,
                resource.getChild(NN_BATCHES).getPath(), "sling:Folder");

        // Create the structure
        String currentBatch = null;
        int total = 0;
        Node previousBatchNode = null, node = null;
        while (nodes.hasNext()) {

            final String batchPath = bucket.getNextPath(resourceResolver);

            if(currentBatch == null) {
                // Set the currentBatch to the first batch folder
                currentBatch = batchPath;
            }

            final String batchItemPath = batchPath + "/" + total++;
            node = JcrUtil.createPath(batchItemPath, SLING_FOLDER, JcrConstants.NT_UNSTRUCTURED, session, false);

            JcrUtil.setProperty(node, PN_PATH, nodes.nextNode().getPath());

            if (total % batchSize == 0) {
                previousBatchNode = node.getParent();
            } else if (total % batchSize == 1) {
                if (previousBatchNode != null) {
                    // Set the "next batch" property, so we know what the next batch to process is when
                    // the current batch is complete
                    JcrUtil.setProperty(previousBatchNode, PN_NEXT_BATCH, node.getParent().getPath());
                }
            }

            if (total % SAVE_THRESHOLD == 0) {
                session.save();
            }
        }

        // Set last batch's "next batch" property to complete so we know we're done
        JcrUtil.setProperty(node.getParent(), PN_NEXT_BATCH, STATE_COMPLETE);

        if (total % SAVE_THRESHOLD != 0) {
            session.save();
        }

        mvm.put(PN_CURRENT_BATCH, currentBatch);
        mvm.put(PN_TOTAL, total);
        mvm.put(PN_INITIALIZED, true);
        mvm.put(PN_STATE, STATE_NOT_STARTED);

        resource.getResourceResolver().commit();
    }

    public void start(final Resource resource) {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        final String jobName = mvm.get(PN_JOB_NAME, String.class);
        final String workflowModel = mvm.get(PN_WORKFLOW_MODEL, String.class);
        final String resourcePath = resource.getPath();
        long interval = mvm.get(PN_INTERVAL, DEFAULT_INTERVAL);

        final Runnable job = new Runnable() {

            Map<String, String> workflowMap = new LinkedHashMap<String, String>();

            public void run() {
                ResourceResolver adminResourceResolver = null;

                try {
                    adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                    workflowMap = getActiveWorkflows(adminResourceResolver, workflowMap);
                    final Resource contentResource = adminResourceResolver.getResource(resourcePath);

                    if(contentResource == null) {
                      log.warn("Bulk workflow process resource [ {} ] could not be found. Removing periodic job.",
                              resourcePath);
                        scheduler.removeJob(jobName);
                    } else if (workflowMap.isEmpty()) {
                        workflowMap = process(contentResource, workflowModel);
                    } else {
                        log.debug("Workflows for batch [ {} ] are still active.", contentResource.adaptTo(ValueMap
                                .class).get(PN_CURRENT_BATCH, "Missing batch"));
                    }
                } catch (Exception e) {
                    log.error("Error processing periodic execution: {}", e.getMessage());
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

            mvm.put(PN_STATE, STATE_RUNNING);
            mvm.put(PN_STARTED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();

        } catch (Exception e) {
            log.error("Error starting bulk workflow management. {}", e.getMessage());
        }
    }

    public void stop(final Resource resource) throws PersistenceException {
        this.stop(resource, STATE_STOPPED);
    }

    private void stopDeactivate(final Resource resource) throws PersistenceException {
        this.stop(resource, STATE_STOPPED_DEACTIVATED);
    }

    private void stop(final Resource resource, final String state) throws PersistenceException {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        final String jobName = mvm.get(PN_JOB_NAME, String.class);

        log.debug("Stopping job [ {} ]", jobName);

        if (StringUtils.isNotBlank(jobName)) {
            scheduler.removeJob(jobName);

            log.info("Bulk Workflow Manager stopped for [ {} ]", jobName);

            mvm.put(PN_STATE, state);
            mvm.put(PN_STOPPED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();
        } else {
            log.error("Trying to stop a job without a name from Bulk Workflow Manager resource [ {} ]",
                    resource.getPath());
        }
    }

    @Override
    public void resume(final Resource resource) {
        this.start(resource);
        log.info("Resumed bulk workflow for [ {} ]", resource.getPath());
    }

    public void complete(final Resource resource) throws PersistenceException {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        final String jobName = mvm.get(PN_JOB_NAME, String.class);

        if (StringUtils.isNotBlank(jobName)) {
            scheduler.removeJob(jobName);

            log.info("Bulk Workflow Manager completed for [ {} ]", jobName);

            mvm.put(PN_STATE, STATE_COMPLETE);
            mvm.put(PN_COMPLETED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();
        } else {
            log.error("Trying to complete a job without a name from Bulk Workflow Manager resource [ {} ]",
                    resource.getPath());
        }
    }

    private Map<String, String> process(final Resource resource, String workflowModel) throws
            WorkflowException, PersistenceException, RepositoryException {

        if(log.isDebugEnabled()) {
            log.debug("Processing batch [ {} ] with workflow model [ {} ]", this.getCurrentBatch(resource).getPath(),
                    workflowModel);
        }

        final boolean autoCleanupWorkflow = resource.adaptTo(ValueMap.class).get(PN_AUTO_PURGE_WORKFLOW, true);
        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(session);
        final WorkflowModel model = workflowSession.getModel(workflowModel);

        final Map<String, String> workflowMap = new LinkedHashMap<String, String>();

        if (autoCleanupWorkflow) {
            this.purge(this.getCurrentBatch(resource));
        }

        final Resource nextBatch = this.advance(resource);

        if(nextBatch != null) {
            for (final Resource child : nextBatch.getChildren()) {
                final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);

                final String state = properties.get(PN_STATE, "");
                final String payloadPath = properties.get(PN_PATH, String.class);

                if (StringUtils.isBlank(state)
                        && StringUtils.isNotBlank(payloadPath)) {

                    // Don't try to restart already processed batch items

                    final Workflow workflow = workflowSession.startWorkflow(model,
                            workflowSession.newWorkflowData("JCR_PATH", payloadPath));
                    properties.put(PN_WORKFLOW_ID, workflow.getId());
                    properties.put(PN_STATE, workflow.getState());

                    workflowMap.put(child.getPath(), workflow.getId());
                }
            }
        } else {
            log.debug("No batch to process (may be completed).");
        }

        resource.getResourceResolver().commit();

        return workflowMap;
    }

    /**
     * Advance to the next batch and update all properties on the current and next batch nodes accordingly.
     * <p/>
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

        // Current Batch
        final Resource currentBatch = this.getCurrentBatch(resource);
        final ModifiableValueMap currentProperties = currentBatch.adaptTo(ModifiableValueMap.class);

        // Next Batch
        final String nextBatchPath = currentProperties.get(PN_NEXT_BATCH, STATE_COMPLETE);

        if (StringUtils.equalsIgnoreCase(nextBatchPath, STATE_COMPLETE)) {

            // Last batch

            this.complete(resource);

            properties.put(PN_COMPLETE_COUNT,
                    properties.get(PN_COMPLETE_COUNT, 0) + this.getSize(currentBatch.getChildren()));

            return null;

        } else {

            // Not the last batch

            final Resource nextBatch = resourceResolver.getResource(nextBatchPath);
            final ModifiableValueMap nextProperties = nextBatch.adaptTo(ModifiableValueMap.class);

            currentProperties.put(PN_STATE, STATE_COMPLETE);
            currentProperties.put(PN_COMPLETED_AT, Calendar.getInstance());

            nextProperties.put(PN_STATE, STATE_RUNNING);
            nextProperties.put(PN_STARTED_AT, Calendar.getInstance());

            properties.put(PN_CURRENT_BATCH, nextBatch.getPath());
            properties.put(PN_COMPLETE_COUNT,
                    properties.get(PN_COMPLETE_COUNT, 0) + this.getSize(currentBatch.getChildren()));

            return nextBatch;
        }
    }

    private int purge(Resource batchResource) throws RepositoryException {
        final ResourceResolver resourceResolver = batchResource.getResourceResolver();
        final List<String> payloadPaths = new ArrayList<String>();

        for (final Resource child : batchResource.getChildren()) {
            final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);
            final String workflowId = properties.get(PN_WORKFLOW_ID, "Missing WorkflowId");
            final String path = properties.get(PN_PATH, "Missing Path");

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

    private Map<String, String> getActiveWorkflows(ResourceResolver resourceResolver,
                                                   final Map<String, String> workflowMap)
            throws RepositoryException, PersistenceException {

        final Map<String, String> activeWorkflowMap = new LinkedHashMap<String, String>();
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(resourceResolver.adaptTo
                (Session.class));

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

                if (!StringUtils.equals(mvm.get(PN_STATE, String.class), workflow.getState())) {
                    mvm.put(PN_STATE, workflow.getState());
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

    private int getSize(Iterable<Resource> resources) {
        int count = 0;
        for (final Resource resource : resources) {
            count++;
        }
        return count;
    }

    @Activate
    protected void activate(final Map<String, String> config) {
        jobs = new ConcurrentHashMap<String, String>();

        ResourceResolver adminResourceResolver = null;

        try {
            adminResourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            Iterator<Resource> resources = adminResourceResolver.findResources(
                    "SELECT * FROM [cq:PageContent] WHERE [sling:resourceType] = "
                            + "'" + BulkWorkflowManagerServlet.SLING_RESOURCE_TYPE + "'", Query.JCR_SQL2);

            while (resources.hasNext()) {
                final Resource resource = resources.next();
                final ValueMap properties = resource.adaptTo(ValueMap.class);

                if (StringUtils.equals(STATE_STOPPED_DEACTIVATED, properties.get(PN_STATE, ""))) {
                    this.resume(resource);
                }
            }
        } catch (LoginException e) {
            log.error("{}", e.getMessage());
        } finally {
            if (adminResourceResolver != null) {
                adminResourceResolver.close();
            }
        }
    }

    @Deactivate
    protected void deactivate(final Map<String, String> config) {
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