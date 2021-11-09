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

import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticWorkflowData;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.filter.WorkItemFilter;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowModel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

public class SyntheticWorkflow implements Workflow {
    private final String id;

    private final Date timeStarted;

    private final SyntheticWorkflowData workflowData;

    private WorkItem activeWorkItem;

    public SyntheticWorkflow(final String id,
                             final SyntheticWorkflowData workflowData) {
        this.id = id;
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    final void setActiveWorkItem(final WorkItem workItem) {
        this.activeWorkItem = workItem;
    }

    @Override
    public final String getId() {
        return this.id;
    }

    @Override
    public final List<WorkItem> getWorkItems() {
        return Collections.singletonList((WorkItem) this.activeWorkItem);
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

    @Override
    public final MetaDataMap getMetaDataMap() {
        return this.getWorkflowData().getMetaDataMap();
    }
}
