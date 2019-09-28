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

package com.adobe.acs.commons.workflow.bulk.removal;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;

@SuppressWarnings("squid:S1214")
public interface WorkflowInstanceRemover {

    /**
     * @deprecated please use the multi-value WORKFLOW_INSTANCES_PATHS instead.
     */
    @Deprecated
    String WORKFLOW_INSTANCES_PATH = "/etc/workflow/instances";

    @SuppressWarnings("squid:S2386") // cannot be moved for backwards compatibility
    String[] WORKFLOW_INSTANCES_PATHS = {
            "/etc/workflow/instances",
            "/var/workflow/instances"
    };

    String MODEL_ID = "modelId";

    /**
     * Removes workflow instances that match the parameter criteria.
     *
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> modelIds,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan) throws PersistenceException, WorkflowRemovalException, InterruptedException, WorkflowRemovalForceQuitException;


    /**
     * Removes workflow instances that match the parameter criteria.
     *
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @param batchSize number of workflow instances to delete per JCR save
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> modelIds,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan,
                                final int batchSize) throws PersistenceException, WorkflowRemovalException, InterruptedException, WorkflowRemovalForceQuitException;


    /**
     * Removes workflow instances that match the parameter criteria.
     *
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @param batchSize number of workflow instances to delete per JCR save
     * @param maxDurationInMins max number of mins the workflow removal process is allowed to run
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> modelIds,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan,
                                final int batchSize,
                                final int maxDurationInMins) throws PersistenceException, WorkflowRemovalException,
            InterruptedException, WorkflowRemovalForceQuitException;


    /**
     * Gets the Workflow Remover's status.
     * *
     * @return the workflow remover's status object 
     */
    WorkflowRemovalStatus getStatus();

    /**
     * Forces an interruption of the Workflow removal process.
     * Any uncommited changes will be lost.
     */
    void forceQuit();
}
