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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticWorkflowModelException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import com.day.cq.workflow.model.WorkflowNode;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.jcr.RepositoryException;

public class SyntheticWorkflowModelImpl implements SyntheticWorkflowModel {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowModelImpl.class);

    private static final String[] WORKFLOW_MODEL_PATH_PREFIXES = new String[]{
        "",
        "/etc/workflow/models"
    };
    
    private static final String WORKFLOW_MODEL_PATH_SUFFIX = "/jcr:content/model";

    private Map<String, Map<String, Object>> syntheticWorkflowModel = new LinkedHashMap<String, Map<String, Object>>();

    public SyntheticWorkflowModelImpl(WorkflowSession workflowSession,
                                      String modelId,
                                      boolean ignoredIncompatibleTypes) throws WorkflowException {

        try {
            modelId = findWorkflowModel(workflowSession, modelId);
        } catch (RepositoryException ex) {
            log.error("Unable to locate workflow with id " + modelId, ex);
            throw new WorkflowException(ex.getMessage(), ex);
        }

        WorkflowModel model = workflowSession.getModel(modelId);

        if (model == null) {
            log.error("Unable to locate workflow starting node for " + modelId);
            throw new WorkflowException("Unable to locate workflow starting node for " + modelId);            
        }
        
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

    private String findWorkflowModel(WorkflowSession workflowSession, String modelId) throws RepositoryException {
        String separator = modelId.startsWith("/") ? "" : "/";
        for (String prefix : WORKFLOW_MODEL_PATH_PREFIXES) {
            String testPath = prefix + separator + modelId;
            String olderWorkflowStyle = testPath + WORKFLOW_MODEL_PATH_SUFFIX;
            if (workflowSession.getSession().nodeExists(olderWorkflowStyle)) {
                return olderWorkflowStyle;
            } else if (workflowSession.getSession().nodeExists(testPath)) {
                return testPath;
            }
        }
        throw new RepositoryException("Unable to locate workflow model with id "+modelId);
    }
}