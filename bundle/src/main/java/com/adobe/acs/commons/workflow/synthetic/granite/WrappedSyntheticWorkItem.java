package com.adobe.acs.commons.workflow.synthetic.granite;

import com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow;
import com.adobe.granite.workflow.HasMetaData;
import com.adobe.granite.workflow.exec.InboxItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowNode;

import java.util.Date;

public interface WrappedSyntheticWorkItem  extends InboxItem, HasMetaData {
	Date getTimeStarted();

	Date getTimeEnded();

	Workflow getWorkflow();

	WorkflowNode getNode();

	String getId();

	WorkflowData getWorkflowData();

	String getCurrentAssignee();

	void setDueTime(Date var1);

	void setProgressBeginTime(Date var1);

	void setPriority(Priority var1);

	void setWorkflow(SyntheticWorkflow var1);

	void setTimeEnded(Date date);
}
