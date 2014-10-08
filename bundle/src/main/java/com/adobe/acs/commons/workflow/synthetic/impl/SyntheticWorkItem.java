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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.day.cq.workflow.exec.*;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowNode;

import java.util.*;

public class SyntheticWorkItem implements WorkItem {
    private static final String CURRENT_ASSIGNEE = "Synthetic Workflow";

    private Date timeStarted = null;
    private Date timeEnded = null;
    private final UUID uuid = UUID.randomUUID();

    private Workflow workflow;
    private WorkflowData workflowData;
    private MetaDataMap metaDataMap = new SyntheticMetaDataMap();

    public SyntheticWorkItem(final WorkflowData workflowData) {
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    public void setWorkflow(final SyntheticWorkflow workflow) {
        workflow.setActiveWorkItem(this);
        this.workflow = workflow;
    }

    public void setTimeEnded(final Date timeEnded) {
        this.timeEnded = timeEnded;
    }

    @Override
    public String getId() {
        return uuid.toString() + "_" + this.getWorkflowData().getPayload();
    }

    @Override
    public Date getTimeStarted() {
        return this.timeStarted;
    }

    @Override
    public Date getTimeEnded() {
        return this.timeEnded;
    }

    @Override
    public WorkflowData getWorkflowData() {
        return this.workflowData;
    }

    @Override
    public String getCurrentAssignee() {
        return CURRENT_ASSIGNEE;
    }

    @Deprecated
    @Override
    public Dictionary<String, String> getMetaData() {
        final Dictionary<String, String> dictionary = new Hashtable<String, String>();

        for(String key : this.getMetaDataMap().keySet()) {
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
    public MetaDataMap getMetaDataMap() {
        return this.metaDataMap;
    }

    @Override
    public Workflow getWorkflow() {
        return this.workflow;
    }

    /* Unimplemented Methods */

    @Override
    public WorkflowNode getNode() {
        return null;
    }

}
