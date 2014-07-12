package com.adobe.acs.commons.workflow.bulk;


import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;

import javax.jcr.RepositoryException;

public interface BulkWorkflowManager {

    String SLING_FOLDER = "sling:Folder";

    int DEFAULT_INTERVAL = 10;

    String NN_BATCHES = "batches";

    String PN_TOTAL = "total";

    String PN_INITIALIZED = "initialized";

    String PN_AUTO_PURGE_WORKFLOW = "autoPurgeWorkflows";

    String PN_COMPLETE_COUNT = "complete";

    String PN_QUERY = "query";

    String PN_BATCH_SIZE = "batchSize";

    String PN_JOB_NAME = "jobName";

    String PN_INTERVAL = "interval";

    String PN_WORKFLOW_ID = "workflowId";

    String PN_WORKFLOW_MODEL = "workflowModel";

    String PN_CURRENT_BATCH = "currentBatch";

    String PN_NEXT_BATCH = "nextBatch";

    String PN_PATH = "path";

    String PN_STARTED_AT = "startedAt";

    String PN_STOPPED_AT = "stoppedAt";

    String PN_COMPLETED_AT = "completedAt";

    String PN_STATE = "state";

    String STATE_NOT_STARTED = "not started";

    String STATE_RUNNING = "running";

    String STATE_COMPLETE = "complete";

    String STATE_STOPPED = "stopped";

    String STATE_STOPPED_DEACTIVATED = "stopped [ deactivated ]";


    void initialize(Resource resource, String query, long estimatedTotalSize, int batchSize, int interval,
                   String workflowModel) throws PersistenceException, RepositoryException;

    void start(final Resource resource);

    void stop(Resource resource) throws PersistenceException;

    void resume(Resource resource);

    public Resource getCurrentBatch(Resource resource);

}