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
import org.apache.sling.api.resource.ValueMap;

import javax.jcr.RepositoryException;

public interface BulkWorkflowEngine {
    String SLING_RESOURCE_TYPE = "acs-commons/components/utilities/bulk-workflow-manager";

    String SLING_FOLDER = "sling:Folder";

    int DEFAULT_INTERVAL = 10;

    long DEFAULT_ESTIMATED_TOTAL = 1000000;

    int DEFAULT_BATCH_SIZE = 10;

    int DEFAULT_BATCH_TIMEOUT = 20;

    boolean DEFAULT_PURGE_WORKFLOW = false;

    String NN_BATCHES = "batches";

    String KEY_QUERY = "query";

    String KEY_TOTAL = "total";

    String KEY_COMPLETE_COUNT = "complete";

    String KEY_ESTIMATED_TOTAL = "estimatedTotal";

    String KEY_BATCH_TIMEOUT_COUNT = "batchTimeoutCount";

    String KEY_FORCE_TERMINATED_COUNT = "forceTerminatedCount";

    String KEY_BATCH_TIMEOUT = "batchTimeout";

    String KEY_INITIALIZED = "initialized";

    String KEY_PURGE_WORKFLOW = "purgeWorkflow";

    String KEY_BATCH_SIZE = "batchSize";

    String KEY_RELATIVE_PATH = "relativePath";

    String KEY_JOB_NAME = "jobName";

    String KEY_INTERVAL = "interval";

    String KEY_WORKFLOW_ID = "workflowId";

    String KEY_WORKFLOW_MODEL = "workflowModel";

    String KEY_CURRENT_BATCH = "currentBatch";

    String KEY_NEXT_BATCH = "nextBatch";

    String KEY_PATH = "path";

    String KEY_STARTED_AT = "startedAt";

    String KEY_STOPPED_AT = "stoppedAt";

    String KEY_COMPLETED_AT = "completedAt";

    String KEY_STATE = "state";

    String STATE_NOT_STARTED = "not started";

    String STATE_RUNNING = "running";

    String STATE_COMPLETE = "complete";

    String STATE_STOPPED = "stopped";

    String STATE_FORCE_TERMINATED = "force terminated";

    String STATE_STOPPED_DEACTIVATED = "stopped-deactivated";

    String STATE_STOPPED_ERROR = "stopped-error";


    /**
     * Initialize the Bulk Workflow Manager jcr:content node and build out the batch structure.
     *
     * @param resource   jcr:content resource
     * @param properties a valuemap containing all requisite properties
     * @throws PersistenceException
     * @throws RepositoryException
     */
    void initialize(Resource resource, ValueMap properties) throws PersistenceException,
            RepositoryException;

    /**
     * Start bulk workflow process.
     *
     * @param resource jcr:content configuration resource
     */
    void start(final Resource resource);

    /**
     * Stop bulk workflow process.
     *
     * @param resource jcr:content configuration resource
     * @throws PersistenceException
     */
    void stop(Resource resource) throws PersistenceException;

    /**
     * Resume as stopped bulk workflow process.
     *
     * @param resource jcr:content configuration resource
     * @throws PersistenceException
     */
    void resume(Resource resource);

    /**
     * Resume as stopped bulk workflow process.
     *
     * @param resource jcr:content configuration resource
     * @param interval in seconds
     */
    void resume(Resource resource, long interval) throws PersistenceException;

    /**
     * Get the current batch to process.
     *
     * @param resource jcr:content configuration resource
     * @return the resource that is the current batch to process
     */
    Resource getCurrentBatch(Resource resource);

}