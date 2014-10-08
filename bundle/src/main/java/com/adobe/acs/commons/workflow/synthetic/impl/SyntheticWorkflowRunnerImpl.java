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
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;


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

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(final ResourceResolver resourceResolver,
                      final String payloadPath,
                      final String[] workflowProcessLabels,
                      final Map<String, Map<String, Object>> metaDataMaps,
                      final boolean autoSaveAfterEachWorkflowProcess,
                      final boolean autoSaveAtEnd) throws WorkflowException {

        final long start = System.currentTimeMillis();

        final Session session = resourceResolver.adaptTo(Session.class);
        final WorkflowSession workflowSession = this.getWorkflowSession(session);

        // Create the WorkflowData obj; This will persist through all WF Process Steps
        final WorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", payloadPath);

        // Create the Workflow obj; This will persist through all WF Process Steps
        // The Workflow MetadataMap will leverage the WorkflowData's MetadataMap as these two maps should be in sync
        final SyntheticWorkflow workflow = new SyntheticWorkflow("Synthetic Workflow: " + payloadPath, workflowData);

        try {
            for (final String workflowProcessLabel : workflowProcessLabels) {
                final WorkflowProcess workflowProcess = workflowProcesses.get(workflowProcessLabel);

                if(workflowProcess != null) {

                    // Each Workflow Process Step gets its own workItem whose life starts and ends w the WF Process
                    final SyntheticWorkItem workItem = new SyntheticWorkItem(workflowData);
                    workItem.setWorkflow(workflow);

                    // Used to pass in per-Step parameters
                    final MetaDataMap workflowProcessMetaDataMap = new SyntheticMetaDataMap(metaDataMaps.get(workflowProcessLabel));

                    if (workflowProcess == null) {
                        log.warn("Synthetic Workflow could not find a Workflow Model with label [ {} ]", workflowProcessLabel);
                    } else {
                        try {
                            // Execute the WF
                            workflowProcess.execute(workItem, workflowSession, workflowProcessMetaDataMap);
                            workItem.setTimeEnded(new Date());

                            log.trace("Synthetic workflow execution of [ {} ] executed in [ {} ] ms",
                                    workflowProcessLabel,
                                    workItem.getTimeEnded().getTime() - workItem.getTimeStarted().getTime());
                        } catch (SyntheticCompleteWorkflowException ex) {
                            log.info("Synthetic workflow execution stopped via complete() for [ {} ]", payloadPath);
                        } catch (SyntheticTerminateWorkflowException ex) {
                            log.info("Synthetic workflow execution stopped via terminate() for [ {} ]", payloadPath);
                        } finally {
                            try {
                                if(autoSaveAtEnd && session.hasPendingChanges()) {
                                    session.save();
                                }
                            } catch (RepositoryException e) {
                                log.error("Could not save at end of synthetic workflow execution process"
                                        + " [ {} ] for payload path [ {} ]", workflowProcessLabel, payloadPath);
                                log.error("Synthetic Workflow process save failed.", e);
                                throw new WorkflowException(e);
                            }
                        }
                    }
                } else {
                    log.error("Synthetic workflow runner retrieved a null Workflow Process for process.label [ {} ]",
                            workflowProcessLabel);
                }
            } // end for loop

            try {
                if(autoSaveAtEnd && session.hasPendingChanges()) {
                    session.save();
                }
            } catch (RepositoryException e) {
                log.error("Could not complete save at end of synthetic workflow execution process"
                        + " [ {} ]", payloadPath, e);
                throw new WorkflowException(e);
            }
        } catch(SyntheticRestartWorkflowException ex) {
            log.info("Restarting synthetic workflow for [ {} ]", payloadPath);
            this.start(resourceResolver, payloadPath, workflowProcessLabels, metaDataMaps,
                    autoSaveAfterEachWorkflowProcess, autoSaveAtEnd);
        }

        log.info("Synthetic workflow execution of payload [ {} ] completed in [ {} ] ms",
                payloadPath,
                System.currentTimeMillis() - start);
    }

    /**
     * Unsupported operation
     *
     * @throws WorkflowException
     */
    @Override
    public void start() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Unsupported operation
     */
    @Override
    public void stop() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Creates a Synthetic Workflow Session from a JCR Session
     *
     * @param session the JCR Session to create the Synthetic Workflow Session from
     * @return the Synthetic Workflow Session
     */
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
            log.debug("Synthetic Workflow Runner added Workflow Process [ {} ]", label);
        }
    }

    protected final void unbindWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
        if (label != null) {
            workflowProcesses.remove(label);
            log.debug("Synthetic Workflow Runner removed Workflow Process [ {} ]", label);
        }
    }
}
