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

package com.adobe.acs.commons.workflow.synthetic.impl.granite;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticWorkflowModelException;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.adobe.granite.workflow.model.WorkflowNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SyntheticWorkflowModelImpl implements SyntheticWorkflowModel {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowModelImpl.class);

    private static final String WORKFLOW_MODEL_PATH_PREFIX = "/etc/workflow/models/";
    private static final String WORKFLOW_MODEL_PATH_SUFFIX = "/jcr:content/model";

    private Map<String, Map<String, Object>> syntheticWorkflowModel = new LinkedHashMap<String, Map<String, Object>>();

    public SyntheticWorkflowModelImpl(WorkflowSession workflowSession,
                                      String modelId,
                                      boolean ignoredIncompatibleTypes) throws WorkflowException {

        if (!StringUtils.startsWith(modelId, WORKFLOW_MODEL_PATH_PREFIX)) {
            modelId = WORKFLOW_MODEL_PATH_PREFIX + modelId;
        }

        if (!StringUtils.endsWith(modelId, WORKFLOW_MODEL_PATH_SUFFIX)) {
            modelId = modelId + WORKFLOW_MODEL_PATH_SUFFIX;
        }

        final WorkflowModel model = workflowSession.getModel(modelId);

        log.debug("Located Workflow Model [ {} ] with modelId [ {} ]", model.getTitle(), modelId);

        final List<WorkflowNode> nodes = model.getNodes();

        for (final WorkflowNode node : nodes) {
            if (!ignoredIncompatibleTypes && !this.isValidType(node)) {
                // Only Process Steps are allowed
                throw new SyntheticWorkflowModelException(node.getId()
                        + " is of incompatible type " + node.getType());
            } else if (node.getTransitions().size() > 1) {
                throw new SyntheticWorkflowModelException(node.getId()
                        + " has unsupported decision based execution (more than 1 transitions is not allowed)");
            }

            // No issues with Workflow Model; Collect the Process type
            log.debug("Workflow node title [ {} ]", node.getTitle());

            if (this.isProcessType(node)) {
                final String processName = node.getMetaDataMap().get("PROCESS", "");

                if (StringUtils.isNotBlank(processName)) {
                    log.debug("Adding Workflow Process [ {} ] to Synthetic Workflow", processName);
                    syntheticWorkflowModel.put(processName, node.getMetaDataMap());
                }
            }
        }
    }

    public final String[] getWorkflowProcessNames() {
        return this.syntheticWorkflowModel.keySet().toArray(new String[this.syntheticWorkflowModel.keySet().size()]);
    }

    public final Map<String, Map<String, Object>> getSyntheticWorkflowModelData() {
        return this.syntheticWorkflowModel;
    }

    private boolean isValidType(WorkflowNode node) {
        return WorkflowNode.TYPE_START.equals(node.getType())
                || WorkflowNode.TYPE_PROCESS.equals(node.getType());
    }

    private boolean isProcessType(WorkflowNode node) {
        return WorkflowNode.TYPE_PROCESS.equals(node.getType());
    }
}