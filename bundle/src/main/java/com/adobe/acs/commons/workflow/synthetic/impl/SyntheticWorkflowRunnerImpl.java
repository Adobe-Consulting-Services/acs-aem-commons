/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
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
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACS AEM Commons - Synthetic Workflow Runner
 * Facilitates the execution of synthetic workflow.
 */
@Component
@Reference(
        name = "workflowProcesses",
        referenceInterface = WorkflowProcess.class,
        policy = ReferencePolicy.DYNAMIC,
        cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE
)
@Service(value = SyntheticWorkflowRunner.class)
public class SyntheticWorkflowRunnerImpl implements SyntheticWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowRunnerImpl.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";

    private static final String WORKFLOW_PROCESS_LABEL = "process.label";

    private static final int MAX_RESTART_COUNT = 3;

    private Map<String, WorkflowProcess> workflowProcessesByLabel = new ConcurrentHashMap<String, WorkflowProcess>();

    private Map<String, WorkflowProcess> workflowProcessesByProcessName =
            new ConcurrentHashMap<String, WorkflowProcess>();

    @Reference
    private WorkflowService aemWorkflowService;

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute(final ResourceResolver resourceResolver, final String payloadPath,
                              final String[] workflowProcessLabels) throws WorkflowException {
        this.execute(resourceResolver, payloadPath, workflowProcessLabels, null, false, false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute(final ResourceResolver resourceResolver,
                              final String payloadPath,
                              final String[] workflowProcessLabels,
                              Map<String, Map<String, Object>> processArgs,
                              final boolean autoSaveAfterEachWorkflowProcess,
                              final boolean autoSaveAtEnd) throws WorkflowException {

        this.execute(resourceResolver,
                payloadPath,
                WorkflowProcessIdType.PROCESS_LABEL,
                workflowProcessLabels,
                processArgs,
                autoSaveAfterEachWorkflowProcess,
                autoSaveAtEnd);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void execute(final ResourceResolver resourceResolver,
                              final String payloadPath,
                              final WorkflowProcessIdType workflowProcessIdType,
                              final String[] workflowProcessIds,
                              Map<String, Map<String, Object>> processArgs,
                              final boolean autoSaveAfterEachWorkflowProcess,
                              final boolean autoSaveAtEnd) throws WorkflowException {

        final long start = System.currentTimeMillis();

        if (processArgs == null) {
            processArgs = new HashMap<String, Map<String, Object>>();
        }

        int count = 0;
        do {
            count++;

            try {
                run(resourceResolver, payloadPath, workflowProcessIdType, workflowProcessIds, processArgs,
                        autoSaveAfterEachWorkflowProcess, autoSaveAtEnd);

                if (log.isInfoEnabled()) {
                    long duration = System.currentTimeMillis() - start;
                    log.info("Synthetic workflow execution of payload [ {} ] completed in [ {} ] ms",
                            payloadPath, duration);
                }

                return;

            } catch (SyntheticRestartWorkflowException ex) {
                if (count < MAX_RESTART_COUNT) {
                    log.info("Restarting synthetic workflow for [ {} ]", payloadPath);
                } else {
                    log.warn("Synthetic workflow execution of payload [ {} ] reached max restart rate of [ {} ]",
                            payloadPath, count);
                }
            }
        } while (count < MAX_RESTART_COUNT);
    }

    private void run(final ResourceResolver resourceResolver,
                     final String payloadPath,
                     final WorkflowProcessIdType workflowProcessIdType,
                     final String[] workflowProcessIds,
                     final Map<String, Map<String, Object>> metaDataMaps,
                     final boolean autoSaveAfterEachWorkflowProcess,
                     final boolean autoSaveAtEnd) throws WorkflowException {

        final Session session = resourceResolver.adaptTo(Session.class);
        final WorkflowSession workflowSession = this.getWorkflowSession(session);

        // Create the WorkflowData obj; This will persist through all WF Process Steps
        final WorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", payloadPath);

        // Create the Workflow obj; This will persist through all WF Process Steps
        // The Workflow MetadataMap will leverage the WorkflowData's MetadataMap as these two maps should be in sync
        final SyntheticWorkflow workflow = new SyntheticWorkflow("Synthetic Workflow: " + payloadPath, workflowData);

        for (final String workflowProcessId : workflowProcessIds) {
            WorkflowProcess workflowProcess;

            if (WorkflowProcessIdType.PROCESS_LABEL.equals(workflowProcessIdType)) {
                workflowProcess = this.workflowProcessesByLabel.get(workflowProcessId);
            } else {
                workflowProcess = this.workflowProcessesByProcessName.get(workflowProcessId);
            }

            if (workflowProcess != null) {

                // Each Workflow Process Step gets its own workItem whose life starts and ends w the WF Process
                final SyntheticWorkItem workItem = new SyntheticWorkItem(workflowData);
                workItem.setWorkflow(workflow);

                // Used to pass in per-Step parameters
                final MetaDataMap workflowProcessMetaDataMap =
                        new SyntheticMetaDataMap(metaDataMaps.get(workflowProcessId));


                try {
                    // Execute the WF
                    workflowProcess.execute(workItem, workflowSession, workflowProcessMetaDataMap);
                    workItem.setTimeEnded(new Date());

                    log.trace("Synthetic workflow execution of [ {} ] executed in [ {} ] ms",
                            workflowProcessId,
                            workItem.getTimeEnded().getTime() - workItem.getTimeStarted().getTime());
                } catch (SyntheticTerminateWorkflowException ex) {
                    // Terminate entire Workflow execution for this payload
                    log.info("Synthetic workflow execution stopped via terminate() for [ {} ]", payloadPath);
                    break;
                } finally {
                    try {
                        if (autoSaveAfterEachWorkflowProcess && session.hasPendingChanges()) {
                            session.save();
                        }
                    } catch (RepositoryException e) {
                        log.error("Could not save at end of synthetic workflow execution process"
                                + " [ {} ] for payload path [ {} ]", workflowProcessId, payloadPath);
                        log.error("Synthetic Workflow process save failed.", e);
                        throw new WorkflowException(e);
                    }
                }
            } else {
                log.error("Synthetic workflow runner retrieved a null Workflow Process for process.label [ {} ]",
                        workflowProcessId);
            }
        } // end for loop

        try {
            if (autoSaveAtEnd && session.hasPendingChanges()) {
                session.save();
            }
        } catch (RepositoryException e) {
            log.error("Could not complete save at end of synthetic workflow execution process"
                    + " [ {} ]", payloadPath, e);
            throw new WorkflowException(e);
        }

    }

    @Override
    public final void execute(final ResourceResolver resourceResolver,
                              final String payloadPath,
                              final SyntheticWorkflowModel syntheticWorkflowModel,
                              final boolean autoSaveAfterEachWorkflowProcess,
                              final boolean autoSaveAtEnd) throws WorkflowException {

        final String[] processNames = syntheticWorkflowModel.getWorkflowProcessNames();
        final Map<String, Map<String, Object>> processConfigs = syntheticWorkflowModel.getSyntheticWorkflowModelData();

        execute(resourceResolver,
                payloadPath,
                WorkflowProcessIdType.PROCESS_NAME,
                processNames,
                processConfigs,
                autoSaveAfterEachWorkflowProcess,
                autoSaveAtEnd);
    }

    @Override
    public final SyntheticWorkflowModel getSyntheticWorkflowModel(final ResourceResolver resourceResolver,
                                                                  final String workflowModelId,
                                                                  final boolean ignoreIncompatibleTypes)
            throws WorkflowException {

        final WorkflowSession workflowSession = this.getWorkflowSession(resourceResolver.adaptTo(Session.class));
        return new SyntheticWorkflowModelImpl(workflowSession, workflowModelId, ignoreIncompatibleTypes);
    }


    /**
     * Unsupported operation.
     *
     * @throws WorkflowException
     */
    @Override
    public final void start() throws WorkflowException {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Unsupported operation.
     */
    @Override
    public final void stop() {
        throw new UnsupportedOperationException(UNSUPPORTED_OPERATION_MESSAGE);
    }

    /**
     * Creates a Synthetic Workflow Session from a JCR Session.
     *
     * @param session the JCR Session to create the Synthetic Workflow Session from
     * @return the Synthetic Workflow Session
     */
    public final WorkflowSession getWorkflowSession(final Session session) {
        return new SyntheticWorkflowSession(this, session);
    }

    /**
     * Getter for the AEM Workflow Service; This is only available at the Impl level and not part of the public
     * SyntheticWorkflowRunner interface.
     * The use of this service should be well understand as to prevent potential overhead of the the non-synthetic
     * aspects of WF.
     *
     * @return the AEM Workflow Service
     */
    public final WorkflowService getAEMWorkflowService() {
        return this.aemWorkflowService;
    }

    @Deprecated
    @Override
    public final Dictionary<String, Object> getConfig() {
        return new Hashtable<String, Object>();
    }

    @Deactivate
    protected final void deactivate(final Map<String, Object> config) {
        log.trace("Deactivating Synthetic Workflow Runner");
        this.workflowProcessesByLabel = new ConcurrentHashMap<String, WorkflowProcess>();
    }

    protected final void bindWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        // Workflow Process Labels
        final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
        if (label != null) {
            this.workflowProcessesByLabel.put(label, service);
            log.debug("Synthetic Workflow Runner added Workflow Process by Label [ {} ]", label);
        }

        // Workflow Process Name
        if (service != null) {
            final String processName = service.getClass().getCanonicalName();
            if (processName != null) {
                this.workflowProcessesByProcessName.put(processName, service);
                log.debug("Synthetic Workflow Runner added Workflow Process by Process Name [ {} ]", processName);
            }
        }
    }

    protected final void unbindWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        // Workflow Process Labels
        final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
        if (label != null) {
            this.workflowProcessesByLabel.remove(label);
            log.debug("Synthetic Workflow Runner removed Workflow Process by Label [ {} ]", label);
        }

        // Workflow Process Name
        if (service != null) {
            final String processName = service.getClass().getCanonicalName();
            if (processName != null) {
                this.workflowProcessesByProcessName.remove(processName);
                log.debug("Synthetic Workflow Runner removed Workflow Process by Process Name [ {} ]", processName);
            }
        }
    }
}
