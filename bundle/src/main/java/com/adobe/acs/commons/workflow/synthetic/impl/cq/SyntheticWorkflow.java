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

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.filter.WorkItemFilter;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowModel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

public class SyntheticWorkflow implements Workflow {
    private final String id;

    private final Date timeStarted;

    private SyntheticWorkflowData workflowData;

    private SyntheticWorkItem activeWorkItem;

    public SyntheticWorkflow(final String id,
                             final SyntheticWorkflowData workflowData) {
        this.id = id;
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    public final void setActiveWorkItem(final SyntheticWorkItem workItem) {
        this.activeWorkItem = workItem;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final List<WorkItem> getWorkItems() {
        return Arrays.asList(new WorkItem[]{this.activeWorkItem});
    }

    @Override
    public final List<WorkItem> getWorkItems(final WorkItemFilter workItemFilter) {
        final List<WorkItem> filtered = new ArrayList<WorkItem>();

        for (final WorkItem workItem : this.getWorkItems()) {
            if (workItemFilter.doInclude(workItem)) {
                filtered.add(workItem);
            }
        }

        return filtered;
    }

    @Override
    public final WorkflowModel getWorkflowModel() {
        return null;
    }

    @Override
    public final boolean isActive() {
        return true;
    }

    @Override
    public final String getState() {
        return "Synthetic Running";
    }

    @Override
    public final String getInitiator() {
        return "Synthetic Workflow";
    }

    @Override
    public final Date getTimeStarted() {
        return (Date) this.timeStarted.clone();
    }

    @Override
    public final Date getTimeEnded() {
        return null;
    }

    @Override
    public final WorkflowData getWorkflowData() {
        return this.workflowData;
    }

    public final void setWorkflowData(final WorkflowData workflowData) {
        this.workflowData.resetTo(workflowData);
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

    @Override
    public final MetaDataMap getMetaDataMap() {
        return this.getWorkflowData().getMetaDataMap();
    }
}
