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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.day.cq.workflow.exec.WorkflowProcess;


public class SyntheticWorkflowProcess {

    private Type workflowType;
    private WorkflowProcess cqWorkflowProcess = null;
    private com.adobe.granite.workflow.exec.WorkflowProcess graniteWorkflowProcess = null;

    public SyntheticWorkflowProcess(WorkflowProcess workflowProcess) {
        this.cqWorkflowProcess = workflowProcess;
        this.workflowType = Type.CQ;
    }

    public SyntheticWorkflowProcess(com.adobe.granite.workflow.exec.WorkflowProcess workflowProcess) {
        this.graniteWorkflowProcess = workflowProcess;
        this.workflowType = Type.GRANITE;
    }

    public Type getWorkflowType() {
        return workflowType;
    }

    public WorkflowProcess getCqWorkflowProcess() {
        return cqWorkflowProcess;
    }

    public com.adobe.granite.workflow.exec.WorkflowProcess getGraniteWorkflowProcess() {
        return graniteWorkflowProcess;
    }

    public Object getProcessId() {
        if (Type.CQ.equals(workflowType)) {
            return this.getCqWorkflowProcess().getClass().getCanonicalName();
        } else if (Type.GRANITE.equals(workflowType)) {
            return this.getGraniteWorkflowProcess().getClass().getCanonicalName();
        } else {
            return null;
        }
    }

    public enum Type {
        CQ,
        GRANITE
    }
}
