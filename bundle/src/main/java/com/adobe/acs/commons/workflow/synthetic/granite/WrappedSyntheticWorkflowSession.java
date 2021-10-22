package com.adobe.acs.commons.workflow.synthetic.granite;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.collection.util.ResultSet;
import com.adobe.granite.workflow.exec.HistoryItem;
import com.adobe.granite.workflow.exec.InboxItem;
import com.adobe.granite.workflow.exec.Participant;
import com.adobe.granite.workflow.exec.Route;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.filter.InboxItemFilter;
import com.adobe.granite.workflow.exec.filter.WorkItemFilter;
import com.adobe.granite.workflow.model.VersionException;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.adobe.granite.workflow.model.WorkflowModelFilter;

import java.security.AccessControlException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public interface WrappedSyntheticWorkflowSession {
	void deployModel(WorkflowModel var1) throws WorkflowException;

	WorkflowModel createNewModel(String var1) throws WorkflowException;

	WorkflowModel createNewModel(String var1, String var2) throws WorkflowException;

	void deleteModel(String var1) throws WorkflowException;

	WorkflowModel[] getModels() throws WorkflowException;

	WorkflowModel[] getModels(WorkflowModelFilter var1) throws WorkflowException;

	ResultSet<WorkflowModel> getModels(long var1, long var3) throws WorkflowException;

	ResultSet<WorkflowModel> getModels(long var1, long var3, WorkflowModelFilter var5) throws WorkflowException;

	WorkflowModel getModel(String var1) throws WorkflowException;

	WorkflowModel getModel(String var1, String var2) throws WorkflowException, VersionException;

	Workflow startWorkflow(WorkflowModel var1, WorkflowData var2) throws WorkflowException;

	Workflow startWorkflow(WorkflowModel var1, WorkflowData var2, Map<String, Object> var3) throws WorkflowException;

	void terminateWorkflow(Workflow var1) throws WorkflowException;

	void resumeWorkflow(Workflow var1) throws WorkflowException;

	void suspendWorkflow(Workflow var1) throws WorkflowException;

	WorkItem[] getActiveWorkItems() throws WorkflowException;

	ResultSet<WorkItem> getActiveWorkItems(long var1, long var3) throws WorkflowException;

	ResultSet<WorkItem> getActiveWorkItems(long var1, long var3, WorkItemFilter var5) throws WorkflowException;

	ResultSet<InboxItem> getActiveInboxItems(long var1, long var3, InboxItemFilter var5) throws WorkflowException;

	ResultSet<InboxItem> getActiveInboxItems(long var1, long var3, String var5, InboxItemFilter var6) throws WorkflowException;

	WorkItem[] getAllWorkItems() throws WorkflowException;

	ResultSet<WorkItem> getAllWorkItems(long var1, long var3) throws WorkflowException;

	WorkItem getWorkItem(String var1) throws WorkflowException;

	Workflow[] getWorkflows(String[] var1) throws WorkflowException;

	ResultSet<Workflow> getWorkflows(String[] var1, long var2, long var4) throws WorkflowException;

	Workflow[] getAllWorkflows() throws WorkflowException;

	Workflow getWorkflow(String var1) throws WorkflowException;

	void complete(WorkItem var1, Route var2) throws WorkflowException;

	List<Route> getRoutes(WorkItem var1, boolean var2) throws WorkflowException;

	List<Route> getBackRoutes(WorkItem var1, boolean var2) throws WorkflowException;

	WorkflowData newWorkflowData(String var1, Object var2);

	Iterator<Participant> getDelegates(WorkItem var1) throws WorkflowException;

	void delegateWorkItem(WorkItem var1, Participant var2) throws WorkflowException, AccessControlException;

	List<HistoryItem> getHistory(Workflow var1) throws WorkflowException;

	void updateWorkflowData(Workflow var1, WorkflowData var2);

	void logout();

	boolean isSuperuser();

	void restartWorkflow(Workflow var1) throws WorkflowException;
}
