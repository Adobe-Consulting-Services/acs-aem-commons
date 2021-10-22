package com.adobe.acs.commons.workflow.synthetic.cq;

import com.adobe.acs.commons.workflow.synthetic.impl.cq.SyntheticWorkflow;
import com.day.cq.workflow.HasMetaData;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.model.WorkflowNode;

import java.util.Date;

public interface WrappedSyntheticWorkItem extends HasMetaData {
	Date getTimeStarted();

	Date getTimeEnded();

	Workflow getWorkflow();

	WorkflowNode getNode();

	String getId();

	WorkflowData getWorkflowData();

	String getCurrentAssignee();

	void setWorkflow(SyntheticWorkflow workflow);

	void setTimeEnded(Date timeEnded);
}
