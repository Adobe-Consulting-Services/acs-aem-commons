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

package com.adobe.acs.commons.workflow.synthetic;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Map;

public interface SyntheticWorkflowRunner extends WorkflowService {
    /**
     * Process a payload path using using the provided Workflow Processes.
     *
     * @param resourceResolver the resourceResolver object that provides access to the JCR for WF operations
     * @param payloadPath the path to execute the workflow against
     * @param workflowProcessLabels the process.labels of the workflow to execute in order against the payloadPath
     *                              resource
     * @param metaDataMaps the WF args metadata maps; each workflowProcessLabel can have one map
     * @param autoSaveAfterEachWorkflowProcess persist changes to JCR after each Workflow Process completes
     * @param autoSaveAtEnd persist changes to JCR after all Workflow Process complete
     *
     * @throws com.day.cq.workflow.WorkflowException
     */
    void execute(ResourceResolver resourceResolver,
               String payloadPath,
               String[] workflowProcessLabels,
               Map<String, Map<String, Object>> metaDataMaps,
               boolean autoSaveAfterEachWorkflowProcess,
               boolean autoSaveAtEnd) throws WorkflowException;


    /**
     * Process a payload path using using the provided Workflow Processes.
     *
     * Convenience method for calling:
     *
     *  > execute(resourceResolver, payloadPath, workflowProcessLabels, null, false, false);
     *
     * @param resourceResolver the resourceResolver object that provides access to the JCR for WF operations
     * @param payloadPath the path to execute the workflow against
     * @param workflowProcessLabels the process.labels of the workflow to execute in order against the payloadPath
     *                              resource
     *
     * @throws com.day.cq.workflow.WorkflowException
     */
    void execute(ResourceResolver resourceResolver,
               String payloadPath,
               String[] workflowProcessLabels) throws WorkflowException;
}
