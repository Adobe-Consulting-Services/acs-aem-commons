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

import com.adobe.acs.commons.workflow.synthetic.cq.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.SyntheticMetaDataMap;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.Workflow;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.model.WorkflowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.UUID;

public class SyntheticWorkItem implements InvocationHandler, WrappedSyntheticWorkItem {
    private static final Logger LOG = LoggerFactory.getLogger(WrappedSyntheticWorkItem.class);
    private static final String CURRENT_ASSIGNEE = "Synthetic Workflow";
    private final UUID uuid = UUID.randomUUID();
    private Date timeStarted = null;
    private Date timeEnded = null;
    private Workflow workflow;

    private final WorkflowData workflowData;

    private MetaDataMap metaDataMap = new SyntheticMetaDataMap();

    private SyntheticWorkItem(final WorkflowData workflowData) {
        this.workflowData = workflowData;
        this.timeStarted = new Date();
    }

    public static WrappedSyntheticWorkItem createSyntheticWorkItem(WorkflowData workflowData) {
        InvocationHandler handler = new SyntheticWorkItem(workflowData);
        return (WrappedSyntheticWorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WrappedSyntheticWorkItem.class, WorkItem.class  }, handler);
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
            case "getMetaData":
                return getMetaData();
            case "getMetaDataMap":
                return getMetaDataMap();
            case "setWorkflow":
                this.setWorkflow((SyntheticWorkflow) args[0]);
                return new Object();
            case "setTimeEnded":
                this.setTimeEnded((Date) args[0]);
                return new Object();
            default:
                LOG.error("CQ SYNTHETICWORKFLOW ITEM >> NO IMPLEMENTATION FOR {}", methodName);
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

    /**
     * @deprecated deprecated in interface
     */
    @Deprecated
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

    /* Unimplemented Methods */

    public final WorkflowNode getNode() {
        return null;
    }

}
