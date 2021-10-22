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

import com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticMetaDataMap;
import com.adobe.granite.workflow.exec.InboxItem;
import com.adobe.granite.workflow.exec.Status;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.UUID;

public class SyntheticWorkItem implements InvocationHandler, WrappedSyntheticWorkItem {
    private static final Logger LOG = LoggerFactory.getLogger(SyntheticWorkItem.class);
    private static final String CURRENT_ASSIGNEE = "Synthetic Workflow";
    private final UUID uuid = UUID.randomUUID();
    private Date timeStarted = null;
    private Date timeEnded = null;
    private Date dueTime = null;
    private Date progressBeginTime = null;
    private Workflow workflow;

    private final WorkflowData workflowData;

    private MetaDataMap metaDataMap = new SyntheticMetaDataMap();
    private InboxItem.Priority priority = null;

    private SyntheticWorkItem(final WorkflowData workflowData) {
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    public static WrappedSyntheticWorkItem createSyntheticWorkItem(WorkflowData workflowData) {
        InvocationHandler handler = new SyntheticWorkItem(workflowData);
        return (WrappedSyntheticWorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WrappedSyntheticWorkItem.class, WorkItem.class }, handler);
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        WorkItem workItem = (WorkItem) proxy;
        switch (methodName) {
            case "getTimeStarted":
                return getTimeStarted();
            case "getTimeEnded":
                return getTimeEnded();
            case "getWorkflow":
                return getWorkflow();
            case "getNode":
                return getNode();
            case "getId":
                return getId();
            case "getWorkflowData":
                return getWorkflowData();
            case "getCurrentAssignee":
                return getCurrentAssignee();
            case "setDueTime":
                if (args.length > 0) {
                    setDueTime((Date) args[0]);
                }
                return new Object();
            case "setProgressBeginTime":
                setProgressBeginTime((Date) args[0]);
                return new Object();
            case "setPriority":
                setPriority((InboxItem.Priority) args[0]);
                return new Object();
            case "setWorkflow":
                this.setWorkflow((SyntheticWorkflow) args[0]);
                return new Object();
            case "setTimeEnded":
                this.setTimeEnded((Date) args[0]);
                return new Object();
            case "getMetaDataMap":
                return getMetaDataMap();
            default:
                LOG.error("GRANITE SYNTHETICWORKFLOW ITEM >> NO IMPLEMENTATION FOR {}", methodName);
                throw new UnsupportedOperationException();
        }
    }

    public final String getId() {
        return uuid.toString() + "_" + this.getWorkflowData().getPayload();
    }

    public final Date getTimeStarted() {
        return this.timeStarted == null ? null : (Date) this.timeStarted.clone();
    }

    public final Date getTimeEnded() {
        return this.timeEnded == null ? null : (Date) this.timeEnded.clone();
    }

    public Date getDueTime() {
        return dueTime;
    }

    public Date getProgressBeginTime() {
        return progressBeginTime;
    }

    public final void setTimeEnded(final Date timeEnded) {
        if (timeEnded == null) {
            this.timeEnded = null;
        } else {
            this.timeEnded = (Date) timeEnded.clone();
        }
    }

    public final WorkflowData getWorkflowData() {
        return this.workflowData;
    }

    public final String getCurrentAssignee() {
        return CURRENT_ASSIGNEE;
    }

    public void setDueTime(Date date) {
        dueTime = date;
    }

    public void setProgressBeginTime(Date date) {
        progressBeginTime = date;
    }

    public void setPriority(InboxItem.Priority priority) {
        this.priority = priority;
    }

    /**
     * This metadata map is local to this Workflow Item. This Map will change with each
     * WorkflowProcess step.
     *
     * @return the WorkItem's MetaDataMap
     */
    public final MetaDataMap getMetaDataMap() {
        return this.metaDataMap;
    }

    public final Workflow getWorkflow() {
        return this.workflow;
    }

    public final void setWorkflow(final SyntheticWorkflow workflow) {
        workflow.setActiveWorkItem(this);
        this.workflow = workflow;
    }

    public Status getStatus() {
        return Status.ACTIVE;
    }

    /* Unimplemented Methods */

    public final WorkflowNode getNode() {
        return null;
    }

    @SuppressWarnings("java:S1192")
    public String getItemType() {
        return "Synthetic Workflow";
    }

    public String getItemSubType() {
        return null;
    }

    public String getContentPath() {
        if ("JCR_PATH".equals(this.workflowData.getPayloadType())) {
            return (String) this.workflowData.getPayload();
        } else {
            return null;
        }
    }

    public InboxItem.Priority getPriority() {
        return priority;
    }
}
