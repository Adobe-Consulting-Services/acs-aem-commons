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
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowStep;
import com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkflowSession;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.SyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.SyntheticWorkflow;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.SyntheticWorkflowSession;
import com.adobe.acs.commons.workflow.synthetic.cq.WrappedSyntheticWorkItem;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticCompleteWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticRestartWorkflowException;
import com.adobe.acs.commons.workflow.synthetic.impl.cq.exceptions.SyntheticTerminateWorkflowException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.ReferenceCardinality;
import org.apache.felix.scr.annotations.ReferencePolicy;
import org.apache.felix.scr.annotations.References;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * ACS AEM Commons - Synthetic Workflow Runner
 * Facilitates the execution of synthetic workflow.
 */
@Component(immediate = true)
@References({
        @Reference(
                referenceInterface = WorkflowProcess.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                bind = "bindCqWorkflowProcesses",
                unbind = "unbindCqWorkflowProcesses"
        ),
        @Reference(
                referenceInterface = com.adobe.granite.workflow.exec.WorkflowProcess.class,
                policy = ReferencePolicy.DYNAMIC,
                cardinality = ReferenceCardinality.OPTIONAL_MULTIPLE,
                bind = "bindGraniteWorkflowProcesses",
                unbind = "unbindGraniteWorkflowProcesses"
        )
})
// Explicitly register to the SyntheticWorkflowRunner interface (as this extends WorkflowService, which we do not want to register a service against)
@Service(value = SyntheticWorkflowRunner.class)
public class SyntheticWorkflowRunnerImpl implements SyntheticWorkflowRunner {
    private static final Logger log = LoggerFactory.getLogger(SyntheticWorkflowRunnerImpl.class);

    private static final String UNSUPPORTED_OPERATION_MESSAGE = "Operation not supported by Synthetic Workflow";

    private static final String WORKFLOW_PROCESS_LABEL = "process.label";

    private static final int MAX_RESTART_COUNT = 3;

    private Map<String, SyntheticWorkflowProcess> workflowProcessesByLabel = new ConcurrentHashMap<String, SyntheticWorkflowProcess>();

    private Map<String, SyntheticWorkflowProcess> workflowProcessesByProcessName =
            new ConcurrentHashMap<String, SyntheticWorkflowProcess>();

    @Reference
    private WorkflowService aemWorkflowService;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    private ServiceRegistration accessorReg;

    @Override
    public final void execute(final ResourceResolver resourceResolver, final String payloadPath,
                              final String[] workflowProcessLabels) throws WorkflowException {
        this.execute(resourceResolver, payloadPath, workflowProcessLabels, null, false, false);
    }

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

    public void execute(ResourceResolver resourceResolver,
                        String payloadPath,
                        List<SyntheticWorkflowStep> workflowSteps,
                        boolean autoSaveAfterEachWorkflowProcess,
                        boolean autoSaveAtEnd) throws WorkflowException {
        final long start = System.currentTimeMillis();

        int count = 0;
        do {
            count++;

            try {
                run(resourceResolver, payloadPath, workflowSteps,
                        autoSaveAfterEachWorkflowProcess, autoSaveAtEnd);
                if (log.isInfoEnabled()) {
                    long duration = System.currentTimeMillis() - start;
                    log.info("Synthetic workflow execution of payload [ {} ] completed in [ {} ] ms",
                            payloadPath, duration);
                }

                return;

            } catch (SyntheticRestartWorkflowException ex) {
                if (count < MAX_RESTART_COUNT) {
                    log.info("Restarting CQ synthetic workflow for [ {} ]", payloadPath);
                } else {
                    log.warn("Synthetic CQ workflow execution of payload [ {} ] reached max restart rate of [ {} ]",
                            payloadPath, count);
                }
            }

        } while (count < MAX_RESTART_COUNT);


    }

    public final void execute(final ResourceResolver resourceResolver,
                              final String payloadPath,
                              final WorkflowProcessIdType workflowProcessIdType,
                              final String[] workflowProcessIds,
                              Map<String, Map<String, Object>> processArgs,
                              final boolean autoSaveAfterEachWorkflowProcess,
                              final boolean autoSaveAtEnd) throws WorkflowException {

        if (processArgs == null) {
            processArgs = new HashMap<String, Map<String, Object>>();
        }

        List<SyntheticWorkflowStep> workflowSteps = convertToSyntheticWorkflowSteps(workflowProcessIds, workflowProcessIdType, processArgs);

        execute(resourceResolver, payloadPath, workflowSteps, autoSaveAfterEachWorkflowProcess, autoSaveAtEnd);
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

    @SuppressWarnings({"squid:S3776", "squid:S1163", "squid:S1143"})
    private void run(final ResourceResolver resourceResolver,
                     final String payloadPath,
                     final List<SyntheticWorkflowStep> workflowSteps,
                     final boolean autoSaveAfterEachWorkflowProcess,
                     final boolean autoSaveAtEnd) throws WorkflowException {

        final Session session = resourceResolver.adaptTo(Session.class);

        // Create the WorkflowData obj; This will persist through all WF Process Steps
        // This must be remained defined once to it can be shared by reference across CQ and Granite SyntheticWorkflow isntances
        final SyntheticWorkflowData workflowData = new SyntheticWorkflowData("JCR_PATH", payloadPath);

        // Create the Workflow obj; This will persist through all WF Process Steps
        // The Workflow MetadataMap will leverage the WorkflowData's MetadataMap as these two maps should be in sync
        final SyntheticWorkflow cqWorkflow =
                new SyntheticWorkflow("Synthetic Workflow ( " + payloadPath + " )", workflowData);
        final com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow graniteWorkflow =
                new com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow("Synthetic Workflow ( " + payloadPath + " )", workflowData);

        boolean terminated = false;

        for (final SyntheticWorkflowStep workflowStep : workflowSteps) {
            SyntheticWorkflowProcess workflowProcess;

            if (WorkflowProcessIdType.PROCESS_LABEL.equals(workflowStep.getIdType())) {
                workflowProcess = this.workflowProcessesByLabel.get(workflowStep.getId());
            } else {
                workflowProcess = this.workflowProcessesByProcessName.get(workflowStep.getId());
            }

            if (workflowProcess != null) {
                final long start = System.currentTimeMillis();

                try {
                    final SyntheticMetaDataMap workflowProcessMetaDataMap = new SyntheticMetaDataMap(workflowStep.getMetadataMap());

                    if (SyntheticWorkflowProcess.Type.GRANITE.equals(workflowProcess.getWorkflowType())) {
                        runGraniteWorkflowProcess(session, graniteWorkflow, workflowProcessMetaDataMap, workflowProcess);
                    } else if (SyntheticWorkflowProcess.Type.CQ.equals(workflowProcess.getWorkflowType())) {
                        runCqWorkflowProcess(session, cqWorkflow, workflowProcessMetaDataMap, workflowProcess);
                    } else {
                        log.warn("Workflow process step is of an unknown type [ {} ]. Skipping.", workflowProcess.getWorkflowType());
                    }
                } catch (SyntheticTerminateWorkflowException ex) {
                    // Terminate entire Workflow execution for this payload
                    terminated = true;
                    log.info("Synthetic CQ workflow execution stopped via terminate for [ {} ]", payloadPath);
                    break;
                } catch (com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticTerminateWorkflowException ex) {
                    // Terminate entire Workflow execution for this payload
                    terminated = true;
                    log.info("Synthetic Granite workflow execution stopped via terminate for [ {} ]", payloadPath);
                    break;
                } catch (SyntheticRestartWorkflowException ex) {
                    // Handle CQ Restart Workflow; catch/throw for clarity in whats happening
                    throw ex;
                } catch (com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticRestartWorkflowException ex) {
                    // Handle Granite Restart Exceptions by transforming them into CQ Worlflow Restart Exceptions which the rest of this API leverages
                    throw new SyntheticRestartWorkflowException(ex.getMessage());
                } catch (WorkflowException ex) {
                    // Handle CQ Workflow Exception; catch/throw for clarity in whats happening
                    throw ex;
                } catch (com.adobe.granite.workflow.WorkflowException ex) {
                    // Handle Granite Workflow Exceptions by transforming them into CQ Workflow Exceptions which the rest of this API leverages
                    throw new WorkflowException(ex);
                } finally {
                    try {
                        if (!terminated && autoSaveAfterEachWorkflowProcess && session.hasPendingChanges()) {
                            session.save();
                        }

                        log.debug("Executed synthetic workflow process [ {} ] on [ {} ] in [ {} ] ms", //NOPMD - Flagged as false positive
                                new Object[]{workflowStep.getId(), payloadPath, String.valueOf(System.currentTimeMillis() - start)});
                    } catch (RepositoryException e) {
                        String msg = String.format("Could not save at end of synthetic workflow process execution"
                                + " [ %s ] for payload path [ %s ]", workflowStep.getId(), payloadPath);
                        log.error("Synthetic workflow process save failed: {}",msg, e);
                        throw new WorkflowException(msg,e);
                    }
                }
            } else {
                log.error("Synthetic workflow runner retrieved a null Workflow Process for process.label [ {} ]",
                        workflowStep.getId());
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


    private void runCqWorkflowProcess(Session session,
                                      SyntheticWorkflow workflow,
                                      SyntheticMetaDataMap workflowProcessMetaDataMap,
                                      SyntheticWorkflowProcess workflowProcess) throws WorkflowException {
        final WorkflowSession workflowSession = this.getCqWorkflowSession(session);

        // Each Workflow Process Step gets its own workItem whose life starts and ends w the WF Process
        final SyntheticWorkItem workItem = SyntheticWorkItem.createSyntheticWorkItem(workflow.getWorkflowData());

        log.trace("Executing CQ synthetic workflow process [ {} ] on [ {} ]",
                workflowProcess.getProcessId(),
                workflow.getWorkflowData().getPayload());

        // Execute the Workflow Process
        try {
            WorkItem wrappedWorkItem = (WorkItem) Proxy.newProxyInstance(WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { WorkItem.class, WrappedSyntheticWorkItem.class  }, workItem);
            workItem.setWorkflow(wrappedWorkItem, workflow);
            workflowProcess.getCqWorkflowProcess().execute(wrappedWorkItem, workflowSession, workflowProcessMetaDataMap);
            workItem.setTimeEnded(new Date());
        } catch (SyntheticCompleteWorkflowException ex) {
            // Workitem force-completed via a call to workflowSession.complete(..)
            workItem.setTimeEnded(new Date());
            log.trace(ex.getMessage());
        } catch (SyntheticTerminateWorkflowException ex) {
            workItem.setTimeEnded(new Date());
            log.trace(ex.getMessage());
            throw ex;
        }
    }

    private void runGraniteWorkflowProcess(Session session,
                                           com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflow workflow,
                                           SyntheticMetaDataMap workflowProcessMetaDataMap,
                                           SyntheticWorkflowProcess workflowProcess) throws com.adobe.granite.workflow.WorkflowException {
       final com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflowSession syntheticWorkflowSession = this.getGraniteWorkflowSession(session);

        // Each Workflow Process Step gets its own workItem whose life starts and ends w the WF Process
        final com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkItem workItem =
                com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkItem.createSyntheticWorkItem(workflow.getWorkflowData());

        log.trace("Executing Granite synthetic workflow process [ {} ] on [ {} ]",
                workflowProcess.getProcessId(),
                workflow.getWorkflowData().getPayload());
        // Execute the Workflow Process
        try {
            com.adobe.granite.workflow.WorkflowSession workflowSession = (com.adobe.granite.workflow.WorkflowSession) Proxy.newProxyInstance(WrappedSyntheticWorkflowSession.class.getClassLoader(), new Class[] { com.adobe.granite.workflow.WorkflowSession.class, WrappedSyntheticWorkflowSession.class  }, syntheticWorkflowSession);
            com.adobe.granite.workflow.exec.WorkItem wrappedWorkItem = (com.adobe.granite.workflow.exec.WorkItem) Proxy.newProxyInstance(com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkItem.class.getClassLoader(), new Class[] { com.adobe.granite.workflow.exec.WorkItem.class, com.adobe.acs.commons.workflow.synthetic.granite.WrappedSyntheticWorkItem.class  }, workItem);
            workItem.setWorkflow(wrappedWorkItem, workflow);
            workflowProcess.getGraniteWorkflowProcess().execute(wrappedWorkItem, workflowSession, workflowProcessMetaDataMap);
            workItem.setTimeEnded(new Date());
        } catch (com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticCompleteWorkflowException ex) {
            // Workitem force-completed via a call to workflowSession.complete(..)
            workItem.setTimeEnded(new Date());
            log.trace(ex.getMessage());
        } catch (com.adobe.acs.commons.workflow.synthetic.impl.granite.exceptions.SyntheticTerminateWorkflowException ex) {
            workItem.setTimeEnded(new Date());
            log.trace(ex.getMessage());
            throw ex;
        }
    }


    /**
     * Converts the legacy Workflow Process IDs and Process Args maps into the SyntheticWorkflowStep abstraction that allows for
     * multiple Processes of the same type to be executed in the same WF.
     * @param workflowProcessIds the ordered list of workflow process id or labels
     * @param processArgs the metadata map args, indexed by workflowProcessId
     * @return the ordered list of SyntheticWorkflowSteps that represent the parameter data
     */
    private List<SyntheticWorkflowStep> convertToSyntheticWorkflowSteps(String[] workflowProcessIds,
                                                                        WorkflowProcessIdType idType,
                                                                        Map<String, Map<String, Object>> processArgs) {
        final List<SyntheticWorkflowStep> workflowSteps = new ArrayList<>();

        for (String workflowProcessId : workflowProcessIds) {
            workflowSteps.add(getSyntheticWorkflowStep(workflowProcessId, idType, processArgs.get(workflowProcessId)));
        }

        return workflowSteps;
    }

    @Override
    public final SyntheticWorkflowModel getSyntheticWorkflowModel(final ResourceResolver resourceResolver,
                                                                  final String workflowModelId,
                                                                  final boolean ignoreIncompatibleTypes)
            throws WorkflowException {

        final WorkflowSession workflowSession = aemWorkflowService.getWorkflowSession(resourceResolver.adaptTo(Session.class));
        return new SyntheticWorkflowModelImpl(workflowSession, workflowModelId, ignoreIncompatibleTypes);
    }

    @Override
    public SyntheticWorkflowStep getSyntheticWorkflowStep(String id, WorkflowProcessIdType type) {
        return getSyntheticWorkflowStep(id, type, Collections.EMPTY_MAP);
    }

    @Override
    public SyntheticWorkflowStep getSyntheticWorkflowStep(String id, WorkflowProcessIdType type, Map<String, Object> metadataMap) {
        return new SyntheticWorkflowStepImpl(id, type, metadataMap);
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
     * Deprecated. Please use getCqWorkflowSession(..)
     * Creates a Synthetic Workflow Session from a JCR Session.
     *
     * @param session the JCR Session to create the Synthetic Workflow Session from
     * @return the Synthetic Workflow Session
     */
    @Override
    public final WorkflowSession getWorkflowSession(final Session session) {
        return getCqWorkflowSession(session);
    }

    /**
     * Creates a CQ Synthetic Workflow Session from a JCR Session.
     *
     * @param session the JCR Session to create the Synthetic Workflow Session from
     * @return the CQ Synthetic Workflow Session
     */
    public final WorkflowSession getCqWorkflowSession(final Session session) {
        return new SyntheticWorkflowSession(this, session);
    }

    /**
     * Creates a Granite Synthetic Workflow Session from a JCR Session.
     *
     * @param session the JCR Session to create the Synthetic Workflow Session from
     * @return the Granite Synthetic Workflow Session
     */
    public final com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflowSession getGraniteWorkflowSession(final Session session) {
        return com.adobe.acs.commons.workflow.synthetic.impl.granite.SyntheticWorkflowSession.createSyntheticWorkflowSession(this, session);
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


    public final ResourceResolver getResourceResolver(Session session) throws LoginException {
        Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
        return resourceResolverFactory.getResourceResolver(authInfo);
    }

    @Override
    public final Dictionary<String, Object> getConfig() {
        return new Hashtable<String, Object>();
    }

    @Activate
    protected final void activate(BundleContext bundleContext) {
        this.accessorReg = bundleContext.registerService(SyntheticWorkflowRunnerAccessor.class.getName(), new SyntheticWorkflowRunnerAccessor() {
            @Override
            public SyntheticWorkflowRunner getSyntheticWorkflowRunner() {
                return SyntheticWorkflowRunnerImpl.this;
            }
        }, new Hashtable());
    }

    @Deactivate
    protected final void deactivate(final Map<String, Object> config) {
        log.trace("Deactivating Synthetic Workflow Runner");
        this.workflowProcessesByLabel = new ConcurrentHashMap<String, SyntheticWorkflowProcess>();
        this.workflowProcessesByProcessName = new ConcurrentHashMap<String, SyntheticWorkflowProcess>();
        if (accessorReg != null) {
            accessorReg.unregister();
            accessorReg = null;
        }
    }


    protected final void bindCqWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        bindSyntheticWorkflowProcesses(new SyntheticWorkflowProcess(service), props);
    }

    protected final void unbindCqWorkflowProcesses(final WorkflowProcess service, final Map<Object, Object> props) {
        unbindSyntheticWorkflowProcesses(new SyntheticWorkflowProcess(service), props);
    }

    protected final void bindGraniteWorkflowProcesses(final com.adobe.granite.workflow.exec.WorkflowProcess service, final Map<Object, Object> props) {
        bindSyntheticWorkflowProcesses(new SyntheticWorkflowProcess(service), props);
    }

    protected final void unbindGraniteWorkflowProcesses(final com.adobe.granite.workflow.exec.WorkflowProcess service, final Map<Object, Object> props) {
        unbindSyntheticWorkflowProcesses(new SyntheticWorkflowProcess(service), props);
    }

    protected final void bindSyntheticWorkflowProcesses(final SyntheticWorkflowProcess process, final Map<Object, Object> props) {
        if (process != null) {
            // Workflow Process Labels
            final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
            if (label != null) {
                this.workflowProcessesByLabel.put(label, process);
                log.trace("Synthetic {} Workflow Runner added Workflow Process by Label [ {} ]", process.getWorkflowType(), label);
            }

            // Workflow Process Name
            String processName = (String) process.getProcessId();

            if (processName != null) {
                this.workflowProcessesByProcessName.put(processName, process);
                log.trace("Synthetic {} Workflow Runner added Workflow Process by Process Name [ {} ]", process.getWorkflowType(), processName);
            } else {
                log.trace("Process name is null for [ {} ]", label);
            }
        }
    }

    protected final void unbindSyntheticWorkflowProcesses(final SyntheticWorkflowProcess process, final Map<Object, Object> props) {
        if (process != null) {
            // Workflow Process Labels
            final String label = PropertiesUtil.toString(props.get(WORKFLOW_PROCESS_LABEL), null);
            if (label != null) {
                this.workflowProcessesByLabel.remove(label);
                log.trace("Synthetic {} Workflow Runner removed Workflow Process by Label [ {} ]", process.getWorkflowType(), label);
            }

            // Workflow Process Name
            String processName = (String) process.getProcessId();

            if (processName != null) {
                this.workflowProcessesByProcessName.remove(processName);
                log.trace("Synthetic {} Workflow Runner removed Workflow Process by Process Name [ {} ]", process.getWorkflowType(), processName);
            } else {
                log.trace("Process name is null for [ {} ]", label);
            }
        }
    }
}
