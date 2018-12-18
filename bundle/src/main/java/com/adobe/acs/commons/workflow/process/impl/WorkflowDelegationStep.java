/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.workflow.process.impl;

import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.model.WorkflowModel;
import com.day.cq.commons.inherit.HierarchyNodeInheritanceValueMap;
import com.day.cq.commons.inherit.InheritanceValueMap;

/**
 * This workflow steps invokes another workflow on the current workflow's payload.
 * <p>
 * The delegate workflow is determined by looking up Workflow Id on the
 * resource (or its ascendants). Works on cq:Page's and dam:Asset's.
 * <p>
 * In order to support multiple instances of this behaviour, the name of the property,
 * which determines the model of the delegate workflow, can be be configured as arguments on
 * the workflow step.
 * <p>
 * Arguments should be provided as this:
 * <p>
 *     workflowModelProperty=<propName>
 *     defaultWorkflowModel=<pathToDefaultWorkflowModel>
 *     terminateWorkflowOnDelegation=true|false
 * (eg: /etc/workflow/models/request_for_activation/jcr:content/model)
 * <p>
 * <propName> is the name of the property which contains the paths of the workflow models
 * <p>
 * and <pathToDefaultWorkflowModel> the path to the default Workflow Model, which is used as fallback
 * <p>
 * <terminateWorkflowOnDelegation> is `true` or `false` and dictates if the current workflow will continue executing after delegation.
 * This can be useful to avoid having a single workflow under multiple workflows (depending on how the workflows are setup).
 * <p>
 * If a Workflow Model Id can be resolved, via the content hierarchy (directly) or the the default workflow id param (fallback) but that Workflow Model cannot be resolved, then a WorkflowException is thrown.
 */
@Component
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Workflow Delegation",
                description = "Invokes a new workflow for this payload based on a content-hierarchy based configuration"
        )
})
@Service
public class WorkflowDelegationStep implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(WorkflowDelegationStep.class);

    @Reference
    WorkflowHelper workflowHelper;

    // Under this property the workflow model is stored
    private static final String WORKFLOW_PROPERTY_NAME = "workflowModelProperty";

    // A default workflow model can be provided as fallback
    private static final String DEFAULT_WORKFLOW_MODEL = "defaultWorkflowModel";

    // When set to true, this Workflow will terminate after successful delegation. This is useful if there is a use-case when this step has WF steps behind it.
    private static final String TERMINATE_ON_DELEGATION = "terminateWorkflowOnDelegation";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metadata)
            throws WorkflowException {

        Map<String, String> args = getProcessArgsMap(metadata);

        String propertyName = args.get(WORKFLOW_PROPERTY_NAME);
        String defaultWorkflowModelId = args.get(DEFAULT_WORKFLOW_MODEL);
        boolean terminateOnDelegation = Boolean.parseBoolean(StringUtils.lowerCase(args.get(TERMINATE_ON_DELEGATION)));


        if (StringUtils.isBlank(propertyName)) {
            throw new WorkflowException("PROCESS_ARG [ " + WORKFLOW_PROPERTY_NAME + " ] not defined");
        }

        log.debug("Provided PROCESS_ARGS: propertyName = [ {} ], Default Workflow Model = [ {} ]", propertyName, defaultWorkflowModelId);

        ResourceResolver resolver = null;
        WorkflowData wfData = workItem.getWorkflowData();

        if (!workflowHelper.isPathTypedPayload(wfData)) {
            log.warn("Could not locate a JCR_PATH payload for this workflow. Skipping delegation.");
            return;
        }

        final String path = wfData.getPayload().toString();

        // Get the resource resolver

        resolver = workflowHelper.getResourceResolver(workflowSession);
        if (resolver == null) {
            throw new WorkflowException("Could not adapt the WorkflowSession to a ResourceResolver. Something has gone terribly wrong!");
        }

        // Derive the Page or Asset resource so we have a normalized entry-point for finding the /jcr:content resource

        final Resource pageOrAssetResource = workflowHelper.getPageOrAssetResource(resolver, path);
        if (pageOrAssetResource == null) {
            log.warn("Could not resolve [ {} ] to an Asset or Page. Skipping delegation.", path);
            return;
        }

        // Get the Asset or Page's jcr:content resource, so we have a common inherited property look-up scheme

        final Resource jcrContentResource = pageOrAssetResource.getChild(JcrConstants.JCR_CONTENT);
        if (jcrContentResource == null) {
            throw new WorkflowException(String.format("Could not find a child jcr:content resource for [ %s ]", pageOrAssetResource.getPath()));
        }

        // Look up the content-hierarchy for the delegate workflow model

        final InheritanceValueMap inheritance = new HierarchyNodeInheritanceValueMap(jcrContentResource);
        final String foundWorkflowModelId = StringUtils.trim(inheritance.getInherited(propertyName, String.class));
        final WorkflowModel delegateWorkflowModel = getDelegateWorkflowModel(workflowSession, foundWorkflowModelId, defaultWorkflowModelId);

        if (delegateWorkflowModel != null) {
            workflowSession.startWorkflow(delegateWorkflowModel, wfData);
            log.info("Delegating payload [ {} ] to Workflow Model [ {} ]", wfData.getPayload(), delegateWorkflowModel.getId());

            if (terminateOnDelegation) {
                log.info("Terminating current workflow due to PROCESS_ARGS[ {} ] = [ {} ]", TERMINATE_ON_DELEGATION, terminateOnDelegation);
                workflowSession.terminateWorkflow(workItem.getWorkflow());
            }
        } else {
            log.warn("No valid delegate Workflow Model could be located. Skipping workflow delegation.");
        }
    }


    private WorkflowModel getDelegateWorkflowModel(WorkflowSession workflowSession,
                                                   String foundWorkflowModelId,
                                                   String defaultWorkflowModelId) throws WorkflowException {
        WorkflowModel workflowModel = null;
        if (StringUtils.isNotBlank(foundWorkflowModelId)) {
            workflowModel = getWorkflowModel(workflowSession, foundWorkflowModelId);

            if (workflowModel != null) {
                log.debug("Using configured delegate Workflow Model [ {} ]", workflowModel.getId());
            } else {
                throw new WorkflowException(String.format("Could not find configured Workflow Model at [ %s ]", foundWorkflowModelId));
            }
        } else if (StringUtils.isNotBlank(defaultWorkflowModelId)) {
            workflowModel = getWorkflowModel(workflowSession, defaultWorkflowModelId);

            if (workflowModel != null) {
                log.debug("Using default delegate Workflow Model [ {} ]", workflowModel.getId());
            } else {
                throw new WorkflowException(String.format("Could not find default Workflow Model at [ %s ]", defaultWorkflowModelId));
            }
        }

        return workflowModel;
    }

    private WorkflowModel getWorkflowModel(WorkflowSession workflowSession, String workflowModelId) {
        workflowModelId = StringUtils.stripToEmpty(workflowModelId);
        WorkflowModel workflowModel = null;

        if (StringUtils.isNotBlank(workflowModelId)) {
            if (!workflowModelId.endsWith("/jcr:content/model")) {
                ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession);
                Resource resource = resourceResolver.getResource(workflowModelId + "/jcr:content/model");
                if (resource != null
                        && StringUtils.equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class),"cq:WorkflowModel")) {
                    workflowModelId = resource.getPath();
                }
            }

            try {
                workflowModel = workflowSession.getModel(workflowModelId);
            } catch (WorkflowException e) {
                log.warn("Could not find Workflow Model for [ {} ]", workflowModelId);
            }
        }

        return workflowModel;
    }

    private Map<String, String> getProcessArgsMap(MetaDataMap metaDataMap) {
        final String processArgs = metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "");
        return ParameterUtil.toMap(StringUtils.split(processArgs, System.getProperty("line.separator")), "=");
    }
}