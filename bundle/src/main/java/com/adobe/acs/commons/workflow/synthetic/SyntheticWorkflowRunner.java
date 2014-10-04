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
     * Process a payload path using using the provided Workflow Processes in order
     *
     * @param resourceResolver
     * @param payloadPath
     * @param workflowProcessLabels
     * @param metaDataMaps
     * @throws com.day.cq.workflow.WorkflowException
     */
    void start(ResourceResolver resourceResolver,
               String payloadPath,
               String[] workflowProcessLabels,
               Map<String, Map<String, Object>> metaDataMaps) throws WorkflowException;
}
