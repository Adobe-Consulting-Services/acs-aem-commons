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
     * @param workflowRemovalConfig WF Models to remove
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final WorkflowRemovalConfig workflowRemovalConfig) throws PersistenceException, WorkflowRemovalException,
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
