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

import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.filter.WorkItemFilter;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowModel;

import java.util.*;

public class SyntheticWorkflow implements Workflow {
    private final String id;
    private final Date timeStarted;

    private final WorkflowData workflowData;
    private SyntheticWorkItem activeWorkItem;

    public SyntheticWorkflow(final String id,
                             final WorkflowData workflowData) {
        this.id = id;
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    public void setActiveWorkItem(final SyntheticWorkItem workItem) {
        this.activeWorkItem = workItem;
    }

    @Override
    public String getId() {
        return this.id;
    }

    @Override
    public List<WorkItem> getWorkItems() {
        return Arrays.asList(new WorkItem[] { this.activeWorkItem });
    }

    @Override
    public List<WorkItem> getWorkItems(final WorkItemFilter workItemFilter) {
        final List<WorkItem> filtered = new ArrayList<WorkItem>();

        for(final WorkItem workItem : this.getWorkItems()) {
            if(workItemFilter.doInclude(workItem)) {
                filtered.add(workItem);
            }
        }

        return filtered;
    }

    @Override
    public WorkflowModel getWorkflowModel() {
        return null;
    }

    @Override
    public boolean isActive() {
        return true;
    }

    @Override
    public String getState() {
        return "Synthetic Running";
    }

    @Override
    public String getInitiator() {
        return "Synthetic Workflow";
    }

    @Override
    public Date getTimeStarted() {
        return this.timeStarted;
    }

    @Override
    public Date getTimeEnded() {
        return null;
    }

    @Override
    public WorkflowData getWorkflowData() {
        return this.workflowData;
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

    @Override
    public MetaDataMap getMetaDataMap() {
        return this.getWorkflowData().getMetaDataMap();
    }
}
