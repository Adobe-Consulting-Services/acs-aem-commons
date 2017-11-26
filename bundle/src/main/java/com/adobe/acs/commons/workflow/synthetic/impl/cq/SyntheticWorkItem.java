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

package com.adobe.acs.commons.workflow.synthetic.impl.cq;

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticMetaDataMap;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowNode;

import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

public class SyntheticWorkItem implements WorkItem {
    private static final String CURRENT_ASSIGNEE = "Synthetic Workflow";
    private final UUID uuid = UUID.randomUUID();
    private Date timeStarted = null;
    private Date timeEnded = null;
    private Workflow workflow;

    private final WorkflowData workflowData;

    private MetaDataMap metaDataMap = new SyntheticMetaDataMap();

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

    /**
     * @deprecated deprecated in interface
     */
    @Deprecated
    @Override
    @SuppressWarnings("squid:S1149")
    public final Dictionary<String, String> getMetaData() {
        final Dictionary<String, String> dictionary = new Hashtable<String, String>();

        for (String key : this.getMetaDataMap().keySet()) {
            dictionary.put(key, this.getMetaDataMap().get(key, String.class));
        }

        return dictionary;
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

    /* Unimplemented Methods */

    @Override
    public final WorkflowNode getNode() {
        return null;
    }

}
