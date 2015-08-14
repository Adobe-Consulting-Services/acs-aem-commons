package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticWorkflowModelException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.model.WorkflowModel;
import com.day.cq.workflow.model.WorkflowNode;
import org.apache.commons.lang.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SyntheticWorkflowModelImpl implements SyntheticWorkflowModel {

    private Map<String, Map<String, Object>> syntheticWorkflowModel = new LinkedHashMap<String, Map<String, Object>>()

    public SyntheticWorkflowModelImpl(WorkflowSession workflowSession, String modelId, boolean
            ignoredIncompatibleTypes) throws WorkflowException {

        final WorkflowModel model = workflowSession.getModel(modelId);
        final List<WorkflowNode> nodes = model.getNodes();

        for (final WorkflowNode node : nodes) {
            if(!ignoredIncompatibleTypes && !this.isValidType(node)) {
                // Only Process Steps are allowed
                throw new SyntheticWorkflowModelException(node.getId()
                        + " is of incompatible type " + node.getType());
            } else if(node.getTransitions().size() != 1) {
                throw new SyntheticWorkflowModelException(node.getId()
                        + " has unsupported decision based execution (multiple transitions are not allowed)");
            }

            // No issues with Workflow Model; Collect the Process type

            if (this.isProcessType(node)) {
                final String processName = node.getMetaDataMap().get("PROCESS", "");

                if (StringUtils.isNotBlank(processName)) {
                    syntheticWorkflowModel.put(processName, node.getMetaDataMap());
                }
            }
        }
    }


    public String[] getWorkflowModelNames() {
        return this.syntheticWorkflowModel.keySet().toArray(new String[this.syntheticWorkflowModel.keySet().size()]);
    }


    public Map<String, Map<String, Object>> getSyntheticWorkflowModelData() {
        return this.syntheticWorkflowModel;
    }

    private boolean isValidType(WorkflowNode node) {
        return WorkflowNode.TYPE_START.equals(node.getType())
                || WorkflowNode.TYPE_START.equals(node.getType())
                ||  WorkflowNode.TYPE_PROCESS.equals(node.getType());
    }

    private boolean isProcessType(WorkflowNode node) {
        return WorkflowNode.TYPE_PROCESS.equals(node.getType();
    }
}