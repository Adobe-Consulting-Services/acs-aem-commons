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

package com.adobe.acs.commons.workflow.synthetic.impl.granitetestprocesses;

import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SetDataWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(SetDataWorkflowProcess.class);

    public SetDataWorkflowProcess() {

    }

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        workItem.getWorkflowData().getMetaDataMap().put("workflowdata", "set on workflowdata");
        workItem.getWorkflow().getMetaDataMap().put("workflow", "set on workflow");
        workItem.getMetaDataMap().put("workitem", "local to work item");

        // WorkItem map is scoped only to this WorkItem step
        String actual = workItem.getMetaDataMap().get("workitem", String.class);
        Assert.assertEquals("local to work item", actual);
    }
}
