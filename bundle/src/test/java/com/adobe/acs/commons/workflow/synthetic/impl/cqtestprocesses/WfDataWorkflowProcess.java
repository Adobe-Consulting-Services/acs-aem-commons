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

package com.adobe.acs.commons.workflow.synthetic.impl.cqtestprocesses;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WfDataWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(WfDataWorkflowProcess.class);

    public WfDataWorkflowProcess() {

    }

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        // Workflow Data
        Assert.assertEquals("JCR_PATH", workItem.getWorkflowData().getPayloadType());
        Assert.assertEquals("/content/test", workItem.getWorkflowData().getPayload());

        // Workitem
        Assert.assertTrue(workItem.getId().matches("[a-z0-9]{8}-([a-z0-9]{4}-){3}[a-z0-9]{12}_.+"));
        Assert.assertEquals(null, workItem.getNode());
        Assert.assertTrue(workItem.getTimeStarted() != null);
        Assert.assertTrue(workItem.getTimeEnded() == null);
        Assert.assertTrue(workItem.getWorkflow() != null);
        Assert.assertEquals("Synthetic Workflow", workItem.getCurrentAssignee());

    }
}
