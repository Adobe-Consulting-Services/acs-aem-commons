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


    /**
     * Initialize the Bulk Workflow Manager jcr:content node and build out the batch structure.
     *
     * @param resource jcr:content resource
     * @param query the query to collect all resources to undergo workflow
     * @param estimatedTotalSize estimate total size
     * @param batchSize batch size
     * @param interval number of seconds between checking is a batch has been completely processed
     * @param workflowModel workflow model to use
     * @throws PersistenceException
     * @throws RepositoryException
     */
    void initialize(Resource resource, String query, long estimatedTotalSize, int batchSize, int interval,
                    String workflowModel) throws PersistenceException, RepositoryException;

    /**
     * Start bulk workflow process.
     *
     * @param resource jcr:content configuration resource
     */
    void start(final Resource resource);

    /**
     * Stop bulk workflow process
     *
     * @param resource jcr:content configuration resource
     * @throws PersistenceException
     */
    void stop(Resource resource) throws PersistenceException;

    /**
     * Resume as stopped bulk workflow process
     *
     * @param resource jcr:content configuration resource
     * @throws PersistenceException
     */
    void resume(Resource resource);

    /**
     * Get the current batch to process
     *
     * @param resource jcr:content configuration resource
     * @return the resource that is the current batch to process
     */
    Resource getCurrentBatch(Resource resource);

}