package com.adobe.acs.commons.workflow.bulk.impl;


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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
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

    public Resource getCurrentBatch(Resource resource) {
        final ValueMap properties = resource.adaptTo(ValueMap.class);
        final String currentBatch = properties.get(PN_CURRENT_BATCH, "");
        return resource.getResourceResolver().getResource(currentBatch);
    }

    public void initialize(Resource resource, String query, int batchSize, String workflowModel) throws
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
        mvm.put(PN_AUTO_PURGE_WORKFLOW, true);

        // Query for all candidate resources

        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        final QueryManager queryManager = session.getWorkspace().getQueryManager();
        final NodeIterator nodes = queryManager.createQuery(query, Query.JCR_SQL2).execute().getNodes();

        // Create the structure
        int batchCount = 0, total = 0;
        Node previousBatchNode = null, node = null;
        while (nodes.hasNext()) {

            final String path = this.getOrCreateBatchesResource(resource).getPath() + "/" + batchCount + "/" + total++;
            node = JcrUtil.createPath(path, SLING_FOLDER, JcrConstants.NT_UNSTRUCTURED, session, false);

            JcrUtil.setProperty(node, PN_PATH, nodes.nextNode().getPath());

            if (total % batchSize == 0) {
                previousBatchNode = node.getParent();
                batchCount++;
            } else if (total % batchSize == 1) {
                if (previousBatchNode != null) {
                    JcrUtil.setProperty(previousBatchNode, PN_NEXT_BATCH, node.getParent().getPath());
                }
            }

            if (total % SAVE_THRESHOLD == 0) {
                session.save();
            }
        }

        JcrUtil.setProperty(node.getParent(), PN_NEXT_BATCH, "DONE");

        if (total % SAVE_THRESHOLD != 0) {
            session.save();
        }

        mvm.put(PN_CURRENT_BATCH, resource.getChild(NN_BATCHES).getPath() + "/0");
        mvm.put(PN_TOTAL, total);
        mvm.put(PN_INITIALIZED, true);
        mvm.put(PN_STATE, STATE_NOT_STARTED);

        resource.getResourceResolver().commit();
    }

    public void start(final Resource resource, long period) {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

        final String jobName = mvm.get(PN_JOB_NAME, String.class);
        final String workflowModel = mvm.get(PN_WORKFLOW_MODEL, String.class);
        final String resourcePath = resource.getPath();

        final Runnable job = new Runnable() {

            Map<String, String> workflowMap = new LinkedHashMap<String, String>();

            public void run() {
                ResourceResolver resourceResolver = null;

                try {
                    resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);
                    workflowMap = getActiveWorkflows(resourceResolver, workflowMap);
                    final Resource contentResource = resourceResolver.getResource(resourcePath);

                    if (workflowMap.isEmpty()) {
                        workflowMap = process(contentResource, workflowModel);
                    } else {
                        log.info("Workflows for batch [ {} ] are still active", contentResource.adaptTo(ValueMap
                                .class).get(PN_CURRENT_BATCH, "Missing batch"));
                    }
                } catch (Exception e) {
                    log.error("Error processing periodic execution: {}", e.getMessage());
                } finally {
                    if (resourceResolver != null) {
                        resourceResolver.close();
                    }
                }
            }
        };

        try {
            scheduler.addPeriodicJob(jobName, job, null, period, false);

            log.info("Periodic job added for [ {} ] every [ {} seconds ]", jobName, period);

            mvm.put(PN_STATE, STATE_RUNNING);
            mvm.put(PN_STARTED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();

            jobs.put(resource.getPath(), jobName);
            log.debug("Added tracking for job [ {} , {} ]", resource.getPath(), jobName);
        } catch (Exception e) {
            log.error("Error starting bulk workflow management. {}", e.getMessage());
        }
    }

    public void stop(final Resource resource) throws PersistenceException {
        final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);
        final String jobName = mvm.get(PN_JOB_NAME, String.class);

        log.debug("Stopping job [ {} ]", jobName);

        if (StringUtils.isNotBlank(jobName)) {
            scheduler.removeJob(jobName);

            log.info("Bulk Workflow Manager stopped for [ {} ]", jobName);

            mvm.put(PN_STATE, STATE_STOPPED);
            mvm.put(PN_STOPPED_AT, Calendar.getInstance());

            resource.getResourceResolver().commit();
        } else {
            log.error("Trying to stop a job without a name from Bulk Workflow Manager resource [ {} ]",
                    resource.getPath());
        }
    }

    @Override
    public void resume(final Resource resource, final long period) {
        this.start(resource, period);
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

        log.debug("Processing batch...");

        final boolean autoCleanupWorkflow = resource.adaptTo(ValueMap.class).get(PN_AUTO_PURGE_WORKFLOW, true);

        final Session session = resource.getResourceResolver().adaptTo(Session.class);
        final WorkflowSession workflowSession = workflowService.getWorkflowSession(session);
        final WorkflowModel model = workflowSession.getModel(workflowModel + "/jcr:content/model");
        final Map<String, String> workflowMap = new LinkedHashMap<String, String>();

        if (autoCleanupWorkflow) {
            this.purge(this.getCurrentBatch(resource));
        }

        final Resource batchResource = this.advance(resource);

        for (final Resource child : batchResource.getChildren()) {
            final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);
            final String payloadPath = properties.get(PN_PATH, String.class);

            if (StringUtils.isNotBlank(payloadPath)) {
                final Workflow workflow = workflowSession.startWorkflow(model,
                        workflowSession.newWorkflowData("JCR_PATH", payloadPath));
                properties.put(PN_WORKFLOW_ID, workflow.getId());
                properties.put(PN_STATE, workflow.getState());

                workflowMap.put(child.getPath(), workflow.getId());

                //log.info("Added [ {} ] to workflow [ {} ]", payloadPath, workflowModel);
            }
        }

        resource.getResourceResolver().commit();

        return workflowMap;
    }

    /**
     * Advance to the next batch and update all properties on the current and next batch nodes accordingly.
     * <p/>
     * This method assumes the current batch has been verified as complete.
     *
     * @param resource
     * @return
     * @throws PersistenceException
     * @throws RepositoryException
     */
    private Resource advance(Resource resource) throws PersistenceException, RepositoryException {
        final ResourceResolver resourceResolver = resource.getResourceResolver();
        final ModifiableValueMap properties = resource.adaptTo(ModifiableValueMap.class);

        final Resource currentBatch = this.getCurrentBatch(resource);
        final ModifiableValueMap currentProperties = currentBatch.adaptTo(ModifiableValueMap.class);

        final String nextBatchPath = currentProperties.get(PN_NEXT_BATCH, "ERROR");

        if(StringUtils.equalsIgnoreCase(nextBatchPath, "DONE")) {

            this.complete(resource);

        } else {
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

        return null;
    }

    private int purge(Resource batchResource) throws RepositoryException {
        final ResourceResolver resourceResolver = batchResource.getResourceResolver();
        final List<String> payloadPaths = new ArrayList<String>();

        for (final Resource child : batchResource.getChildren()) {
            final ModifiableValueMap properties = child.adaptTo(ModifiableValueMap.class);
            final String workflowId = properties.get(PN_WORKFLOW_ID, "Missing WorkflowId");
            final String path = properties.get(PN_PATH, "Missing Path");

            final Resource resource = resourceResolver.getResource(workflowId);
            if(resource != null) {
                final Node node = resource.adaptTo(Node.class);
                node.remove();
                payloadPaths.add(path);
            } else {
                log.warn("Could not find workflowId at [ {} ] to purge.", workflowId);
            }
        }

        if(payloadPaths.size() > 0) {
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

    private Resource getOrCreateBatchesResource(Resource contentResource) throws RepositoryException {
        Resource resource = contentResource.getChild(NN_BATCHES);

        if (resource == null) {
            final Session session = contentResource.getResourceResolver().adaptTo(Session.class);
            final String path = contentResource.getPath() + "/" + NN_BATCHES;

            JcrUtil.createPath(path, SLING_FOLDER, session);

            resource = contentResource.getResourceResolver().getResource(path);
        }

        return resource;
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
        log.debug("Activating...");
        jobs = new ConcurrentHashMap<String, String>();
    }

    @Deactivate
    protected void deactivate(final Map<String, String> config) {
        log.debug("Deactivating...");
        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            for (final Map.Entry<String, String> entry : jobs.entrySet()) {
                final String path = entry.getKey();
                final String jobName = entry.getValue();

                log.debug("Stopping scheduled job at resource [ {} ] and job name [ {} ] by way of de-activation",
                        path, jobName);

                try {
                    this.stop(resourceResolver.getResource(path));
                } catch (Exception e) {
                    log.error("Could not stop job via normal means due to: {}", e.getMessage());
                    this.scheduler.removeJob(jobName);
                    log.error("Performed a hard stop for [ {} ]", jobName);
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