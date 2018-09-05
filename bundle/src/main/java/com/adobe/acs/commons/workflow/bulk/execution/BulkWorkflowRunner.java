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

package com.adobe.acs.commons.workflow.bulk.execution;

import aQute.bnd.annotation.ProviderType;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.workflow.bulk.execution.model.SubStatus;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.commons.scheduler.ScheduleOptions;

import javax.jcr.RepositoryException;

@ProviderType
public interface BulkWorkflowRunner {
    /**
     * @param config the Config
     * @return the runnable object that will be used to perform the work.
     */
     Runnable getRunnable(Config config);

    /**
     * If a non-null value is returned, the result of run(..) will be scheduled to run w these options. If null the job will be immediately run in the same thread.
     * @param config the Config
     * @return The Sling Scheduler options or null;
     */
    ScheduleOptions getOptions(Config config);

    /**
     * Collects and initializes the Workspace JCR structure with the payload nodes.
     * @param queryHelper the QueryHelper
     * @param config the Config
     * @throws PersistenceException
     * @throws RepositoryException
     */
    void initialize(QueryHelper queryHelper, Config config) throws PersistenceException,
            RepositoryException;

    /**
     * Initialize the Bulk Workflow Manager workspace w the total count of items to process.
     * @param workspace the Workspace
     * @param totalCount total number of items to process
     * @throws PersistenceException
     */
    void initialize(Workspace workspace, int totalCount) throws PersistenceException;

    /**
     * Starts work on the Workspace.
     * @param workspace the Workspace
     * @throws PersistenceException
     */
    void start(Workspace workspace) throws PersistenceException;

    /**
     * Used to request stop of a Workspace however workspace may still continue to run in order to allow active payloads to complete.
     * @param workspace the Workspace
     * @throws PersistenceException
     */
    void stopping(Workspace workspace) throws PersistenceException;

    /**
     * Stops (or pauses) work in the Workspace.
     * @param workspace the Workspace
     * @throws PersistenceException
     */
    void stop(Workspace workspace) throws PersistenceException;

    /**
     * Stops (or pauses) work in the Workspace.
     * @param workspace the Workspace
     * @param subStatus SubStatus used to identify cause of stopping for corner cases
     * @throws PersistenceException
     */
    void stop(Workspace workspace, SubStatus subStatus) throws PersistenceException;

    /**
     * Stops (or pauses) work in the Workspace due to Error.
     * @param workspace the Workspace
     * @throws PersistenceException
     */
    void stopWithError(Workspace workspace) throws PersistenceException;

    /**
     * Marks a Workspace as being complete, indicating all work has been processed.
     * @param workspace the Workspace
     * @throws PersistenceException
     */
    void complete(Workspace workspace) throws PersistenceException;

    /**
     * Marks a payload as being completed.
     * @param workspace the Workspace
     * @param payload the completed Payload
     * @throws Exception
     */
    @SuppressWarnings("squid:S00112")
    void complete(Workspace workspace, Payload payload) throws Exception;

    /**
     * Processes a payload under the Workspace.
     * @param workspace the Workspace
     * @param payload the Payload to process
     */
    void run(Workspace workspace, Payload payload);

    /**
     * Marks a payload being force terminated.
     * @param workspace the Workspace
     * @param payload the completed Payload
     * @throws Exception
     */
    @SuppressWarnings("squid:S00112")
    void forceTerminate(Workspace workspace, Payload payload) throws Exception;
}

