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

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticCompleteWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.*;


@Component(
        label = "ACS AEM Commons - Synthetic Workflow Runner",
        description = "Facilitates the execution of synthetic workflow."
)
@Reference(
        name = "workflowProcesses",
        referenceInterface = WorkflowProcess.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
@Service
public class SyntheticWorkflowRunnerImpl implements SyntheticWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowRunnerImpl.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";
    private static final String WORKFLOW_PROCESS_LABEL = "process.label";

    private Map<String, WorkflowProcess> workflowProcesses = new HashMap<String, WorkflowProcess>();

    @Override
    public void start(final ResourceResolver resourceResolver, String path, String[] workflowProcessLabels,
                      Map<String, Map<String, Object>> metaDataMaps) throws WorkflowException {

        final Session session = resourceResolver.adaptTo(Session.class);
        final WorkflowSession workflowSession = this.getWorkflowSession(session);

        final SyntheticWorkItem workItem = new SyntheticWorkItem(
                new SyntheticWorkflowData(path, new HashMap<String, Object>()));

        workItem.setTimeStarted(new Date());

        final List<WorkItem> workItems = new ArrayList<WorkItem>();
        workItems.add(workItem);

        try {
            for (final String label : workflowProcessLabels) {
                final WorkflowProcess workflowProcess = workflowProcesses.get(label);

                final SyntheticWorkflow workflow = new SyntheticWorkflow(label, workItems, workItem.getWorkflowData());
                workItem.setWorkflow(workflow);

                // Used to pass in per-model parameters
                final SyntheticMetaDataMap workflowProcessMetaDataMap = new SyntheticMetaDataMap(metaDataMaps.get(label));

                if (workflowProcess == null) {
                    log.warn("Synthetic Workflow could not find a Workflow Model with label [ {} ]", label);
                } else {
                    try {
                        workflowProcess.execute(workItem, workflowSession, workflowProcessMetaDataMap);
                    } catch (SyntheticCompleteWorkflowException ex) {
                        log.info("Synthetic workflow execution stopped via complete() for [ {} ]", path);
                    } catch (SyntheticTerminateWorkflowException ex) {
                        log.info("Synthetic workflow execution stopped via terminate() for [ {} ]", path);
                    }
                }
            }
        } catch(SyntheticRestartWorkflowException ex) {
            this.start(resourceResolver, path, workflowProcessLabels, metaDataMaps);
        }

        workItem.setTimeEnded(new Date());
    }

    @Override
    public void start() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public void stop() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    @Override
    public WorkflowSession getWorkflowSession(final Session session) {
        return new SyntheticWorkflowSession(this, session);
    }

    @Deprecated
    @Override
    public Dictionary<String, Object> getConfig() {
        return new Hashtable<String, Object>();
    }

    @Activate
    protected void activate(Map<String, Object> config) {
        workflowProcesses = new HashMap<String, WorkflowProcess>();
    }

    protected final void bindWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
        if (label != null) {
            workflowProcesses.put(label, service);
        }
    }

    protected final void unbindWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
        if (label != null) {
            workflowProcesses.remove(label);
        }
    }
}
