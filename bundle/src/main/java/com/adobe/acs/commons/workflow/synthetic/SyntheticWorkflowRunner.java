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

package com.adobe.acs.commons.workflow.synthetic;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

public interface SyntheticWorkflowRunner extends WorkflowService {
    String PROCESS_ARGS = "PROCESS_ARGS";

    /**
     * Process a payload path using using the provided Workflow Processes.
     *
     * @param resourceResolver                 the resourceResolver object that provides access to the JCR for WF ops
     * @param payloadPath                      the path to execute the workflow against
     * @param workflowProcessIdType            defines is the WF ProcessIds are process.labels or process class names
     * @param workflowProcessIds               the process.labels or process names of the WF to execute in order against the payloadPath
     *                                         resource
     * @param processArgs                      the WF args metadata maps; each workflowProcessLabel can have one map
     * @param autoSaveAfterEachWorkflowProcess persist changes to JCR after each Workflow Process completes
     * @param autoSaveAtEnd                    persist changes to JCR after all Workflow Process complete
     * @throws com.day.cq.workflow.WorkflowException
     */
    void execute(ResourceResolver resourceResolver,
                 String payloadPath,
                 WorkflowProcessIdType workflowProcessIdType,
                 String[] workflowProcessIds,
                 Map<String, Map<String, Object>> processArgs,
                 boolean autoSaveAfterEachWorkflowProcess,
                 boolean autoSaveAtEnd) throws WorkflowException;

    /**
     * Process a payload path using using the provided Workflow Processes.
     *
     * @param resourceResolver                 the resourceResolver object that provides access to the JCR for WF operations
     * @param payloadPath                      the path to execute the workflow against
     * @param workflowProcessLabels            the process.labels of the workflow to execute in order against the payloadPath
     *                                         resource
     * @param processArgs                      the WF args metadata maps; each workflowProcessLabel can have one map
     * @param autoSaveAfterEachWorkflowProcess persist changes to JCR after each Workflow Process completes
     * @param autoSaveAtEnd                    persist changes to JCR after all Workflow Process complete
     * @throws com.day.cq.workflow.WorkflowException
     */
    @Deprecated
    void execute(ResourceResolver resourceResolver,
                 String payloadPath,
                 String[] workflowProcessLabels,
                 Map<String, Map<String, Object>> processArgs,
                 boolean autoSaveAfterEachWorkflowProcess,
                 boolean autoSaveAtEnd) throws WorkflowException;

    /**
     * Process a payload path using using the provided Workflow Processes.
     * Convenience method for calling:
     * > execute(resourceResolver, payloadPath, workflowProcessLabels, null, false, false);
     *
     * @param resourceResolver      the resourceResolver object that provides access to the JCR for WF operations
     * @param payloadPath           the path to execute the workflow against
     * @param workflowProcessLabels the process.labels of the workflow to execute in order against the payloadPath
     *                              resource
     * @throws com.day.cq.workflow.WorkflowException
     */
    void execute(ResourceResolver resourceResolver,
                 String payloadPath,
                 String[] workflowProcessLabels) throws WorkflowException;

    /**
     * Execute the provided Synthetic Workflow Model in the context of Synthetic Workflow.
     *
     * @param resourceResolver                 the resourceResolver object that provides access to the JCR for WF operations
     * @param payloadPath                      the path to execute the workflow against
     * @param syntheticWorkflowModel           the Synthetic Workflow Model to execute
     * @param autoSaveAfterEachWorkflowProcess persist changes to JCR after each Workflow Process completes
     * @param autoSaveAtEnd                    persist changes to JCR after all Workflow Process complete
     * @throws WorkflowException
     */
    void execute(ResourceResolver resourceResolver,
                 String payloadPath,
                 SyntheticWorkflowModel syntheticWorkflowModel,
                 boolean autoSaveAfterEachWorkflowProcess,
                 boolean autoSaveAtEnd) throws WorkflowException;

    /**
     * Generates the SyntheticWorkflowModel that represents the AEM Workflow Model to execute in the context of Synthetic Workflow.
     *
     * @param resourceResolver        the resourceResolver object that provides access to the JCR for WF operations
     * @param workflowModelId         the AEM Workflow Model ID
     * @param ignoreIncompatibleTypes ignore incompatible workflow node types to the best of Synthetic Workflow ability
     * @return the Synthetic Workflow Model
     * @throws WorkflowException
     */
    SyntheticWorkflowModel getSyntheticWorkflowModel(ResourceResolver resourceResolver,
                                                     String workflowModelId,
                                                     boolean ignoreIncompatibleTypes) throws WorkflowException;

    enum WorkflowProcessIdType {
        PROCESS_LABEL,
        PROCESS_NAME
    }
}




