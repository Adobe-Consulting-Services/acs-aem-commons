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

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticMetaDataMap;
import com.adobe.granite.workflow.exec.Status;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowNode;

import java.util.Date;
import java.util.UUID;

public class SyntheticWorkItem implements WorkItem {
    private static final String CURRENT_ASSIGNEE = "Synthetic Workflow";
    private final UUID uuid = UUID.randomUUID();
    private Date timeStarted = null;
    private Date timeEnded = null;
    private Date dueTime = null;
    private Date progressBeginTime = null;
    private Workflow workflow;

    private final WorkflowData workflowData;

    private MetaDataMap metaDataMap = new SyntheticMetaDataMap();
    private Priority priority = null;

    public SyntheticWorkItem(final WorkflowData workflowData) {
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    @Override
    public final String getId() {
        return uuid.toString() + "_" + this.getWorkflowData().getPayload();
    }

    @Override
    public final Date getTimeStarted() {
        return this.timeStarted == null ? null : (Date) this.timeStarted.clone();
    }

    @Override
    public final Date getTimeEnded() {
        return this.timeEnded == null ? null : (Date) this.timeEnded.clone();
    }

    @Override
    public Date getDueTime() {
        return dueTime;
    }

    @Override
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

    @Override
    public final WorkflowData getWorkflowData() {
        return this.workflowData;
    }

    @Override
    public final String getCurrentAssignee() {
        return CURRENT_ASSIGNEE;
    }

    @Override
    public void setDueTime(Date date) {
        dueTime = date;
    }

    @Override
    public void setProgressBeginTime(Date date) {
        progressBeginTime = date;
    }

    @Override
    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    /**
     * This metadata map is local to this Workflow Item. This Map will change with each
     * WorkflowProcess step.
     *
     * @return the WorkItem's MetaDataMap
     */
    @Override
    public final MetaDataMap getMetaDataMap() {
        return this.metaDataMap;
    }

    @Override
    public final Workflow getWorkflow() {
        return this.workflow;
    }

    public final void setWorkflow(final SyntheticWorkflow workflow) {
        workflow.setActiveWorkItem(this);
        this.workflow = workflow;
    }

    @Override
    public Status getStatus() {
        return Status.ACTIVE;
    }

    /* Unimplemented Methods */

    @Override
    public final WorkflowNode getNode() {
        return null;
    }

    @Override
    public String getItemType() {
        return "Synthetic Workflow";
    }

    @Override
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

    @Override
    public Priority getPriority() {
        return priority;
    }
}
