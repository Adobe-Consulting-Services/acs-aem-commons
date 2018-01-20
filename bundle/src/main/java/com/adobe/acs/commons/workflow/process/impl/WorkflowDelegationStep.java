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
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.granite.confmgr.Conf;
import com.adobe.granite.confmgr.ConfMgr;
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
 * The delegate workflow is determined by looking up Workflow Id in /conf, the
 * resource (or its ascendants) or a default model id. Works on cq:Page's and dam:Asset's.
 * <p>
 * In order to support multiple instances of this behaviour, the name of the property,
 * which determines the model of the delegate workflow, can be be configured as arguments on
 * the workflow step.
 * <p>
 * Arguments should be provided as this:
 * <p>
 *     confWorkflowModelProperty=<confPropName>
 *     workflowModelProperty=<propName>
 *     defaultWorkflowModel=<pathToDefaultWorkflowModel>
 *     terminateWorkflowOnDelegation=true|false
 * (eg: /etc/workflow/models/request_for_activation/jcr:content/model)
 * <p>
 * <confPropName> is the name of the property in the configuration workflowDelegation which contains the paths of the workflow models
 * <p>
 * <propName> is the name of the property which contains the paths of the workflow models
 * <p>
 * and <pathToDefaultWorkflowModel> the path to the default Workflow Model, which is used as fallback
 * <p>
 * <terminateWorkflowOnDelegation> is `true` or `false` and dictates if the current workflow will continue executing after delegation.
 * This can be useful to avoid having a single workflow under multiple workflows (depending on how the workflows are setup).
 * <p>
 * The deletation step tries to resolve the delegation workflow model id in the following order
 * <p>
 * 1. confWorkflowModelProperty in the configuration workflowDelegation under /conf (only if assigned to the current content structure)
 * <p>
 * 2. workflowModelProperty in /content
 * <p>
 * 3. defaultWorkflowModel
 * 
 * <p>
 * If no Workflow Model Id can be found, the workflow just proceeds.
 * <p>
 * If a Workflow Model Id can be resolved, but that Workflow Model cannot be resolved, then a WorkflowException is thrown.
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
    private ConfMgr confMgr;
    
    @Reference
    WorkflowHelper workflowHelper;
    
    private static final String CONFIGURATION = "workflowDelegation";
    
    // Under this property the workflow model is stored in /conf
    private static final String CONF_WORKFLOW_PROPERTY_NAME = "confWorkflowModelProperty";

    // Under this property the workflow model is stored in /content
    private static final String CONTENT_WORKFLOW_PROPERTY_NAME = "workflowModelProperty";

    // A default workflow model can be provided as fallback
    private static final String DEFAULT_WORKFLOW_MODEL = "defaultWorkflowModel";

    // When set to true, this Workflow will terminate after successful delegation. This is useful if there is a use-case when this step has WF steps behind it.
    private static final String TERMINATE_ON_DELEGATION = "terminateWorkflowOnDelegation";

    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metadata) throws WorkflowException {

      // Get Configuration
      Map<String, String> args = getProcessArgsMap(metadata);
      String confPropertyName = args.get(CONF_WORKFLOW_PROPERTY_NAME);
      String contentPropertyName = args.get(CONTENT_WORKFLOW_PROPERTY_NAME);
      String defaultWorkflowModelId = args.get(DEFAULT_WORKFLOW_MODEL);
      boolean terminateOnDelegation = Boolean.parseBoolean(StringUtils.lowerCase(args.get(TERMINATE_ON_DELEGATION)));
      log.debug(String.format(
          "Provided PROCESS_ARGS: confWorkflowModelProperty = [ %s ],confWorkflowModelProperty = [ %s ], defaultWorkflowModel = [ %s ], terminateOnDelegation = [ %s ]",
          confPropertyName, contentPropertyName, defaultWorkflowModelId, terminateOnDelegation));

      // Get resource from payload
      final Resource jcrContentResource = this.getContentResourceFromPayload(workflowSession, workItem);
      if (jcrContentResource == null) {
        // Case we can not find it just proceed
        return;
      }

      // Look up conf for the delegated workflow model
      final String confWorkflowModelId = this.getWorkflowModelfromConf(jcrContentResource, confPropertyName);

      // Look up the content-hierarchy for the delegate workflow model
      final String contentWorkflowModelId = this.getWorflowModelFromContent(jcrContentResource, contentPropertyName);

      // Fallback
      final WorkflowModel delegateWorkflowModel = getDelegateWorkflowModel(workflowSession, confWorkflowModelId,
          contentWorkflowModelId, defaultWorkflowModelId);

      this.doDelegate(delegateWorkflowModel, workflowSession, workItem, terminateOnDelegation);
    }

    /**
     * Does fallback from 1. conf property 2. content property 3. default property
     * 
     * @throws WorkflowException
     *           if found model id can not be located
     */
    private WorkflowModel getDelegateWorkflowModel(WorkflowSession workflowSession, String confWorkflowModelId,
        String contentWorkflowModelId, String defaultWorkflowModelId) throws WorkflowException {
      WorkflowModel workflowModel = null;
      final String debugText = "Using configured delegate Workflow Model [ {} ] found in {}";
      final String exceptionText = "Could not find configured Workflow Model [ %s ] in %s";

      // Check /conf
      if (StringUtils.isNotBlank(confWorkflowModelId)) {
        workflowModel = getWorkflowModel(workflowSession, confWorkflowModelId);

        if (workflowModel != null) {
          log.debug(debugText, workflowModel.getId(), "/conf");
        } else {
          throw new WorkflowException(String.format(exceptionText, confWorkflowModelId, "/conf"));
        }
        // Check Content
      } else if (StringUtils.isNotBlank(contentWorkflowModelId)) {

        workflowModel = getWorkflowModel(workflowSession, contentWorkflowModelId);

        if (workflowModel != null) {
          log.debug(debugText, workflowModel.getId(), "/content");
        } else {
          throw new WorkflowException(String.format(exceptionText, confWorkflowModelId, "/content"));
        }

        // Check default
      } else if (StringUtils.isNotBlank(defaultWorkflowModelId)) {
        workflowModel = getWorkflowModel(workflowSession, defaultWorkflowModelId);

        if (workflowModel != null) {
          log.debug(debugText, workflowModel.getId(), "parameter defaultworkflowmodelid");
        } else {
          throw new WorkflowException(
              String.format(exceptionText, confWorkflowModelId, "parameter defaultworkflowmodelid"));
        }
      }

      return workflowModel;
    }

    /**
     * Ensures that workflow model id points to $model$/jcr:content/model
     */
    private WorkflowModel getWorkflowModel(WorkflowSession workflowSession, String workflowModelId) {
      String id = StringUtils.stripToEmpty(workflowModelId);
      WorkflowModel workflowModel = null;

      if (StringUtils.isNotBlank(id)) {
        if (!id.endsWith("/jcr:content/model")) {
          ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession);
          Resource resource = resourceResolver.getResource(id + "/jcr:content/model");
          if (resource != null && StringUtils
              .equals(resource.getValueMap().get(JcrConstants.JCR_PRIMARYTYPE, String.class), "cq:WorkflowModel")) {
            id = resource.getPath();
          }
        }

        try {
          workflowModel = workflowSession.getModel(id);
        } catch (WorkflowException e) {
          log.warn("Could not find Workflow Model for [ {} ]", id, e);
        }
      }

      return workflowModel;
    }
    
    /**
 * Extracts
 * @param metaDataMap
 * @return
 */
    private Map<String, String> getProcessArgsMap(MetaDataMap metaDataMap) {
        final String processArgs = metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "");
        return ParameterUtil.toMap(StringUtils.split(processArgs, System.getProperty("line.separator")), "=");
    }
    
    /**
     * Tries to start found model
     * 
     */
    private void doDelegate(WorkflowModel delegateWorkflowModel, WorkflowSession workflowSession, WorkItem workItem,
        boolean terminateOnDelegation) throws WorkflowException {

      if (delegateWorkflowModel != null) {
        try{
        workflowSession.startWorkflow(delegateWorkflowModel, workItem.getWorkflowData());
        }catch (WorkflowException e){
          log.error("Workflow exception occourd while trying to start workflow {} ",delegateWorkflowModel.getId(),e);
          throw e;
        }
        log.debug("Delegating payload [ {} ] to Workflow Model [ {} ]", workItem.getWorkflowData().getPayload(),
            delegateWorkflowModel.getId());

        if (terminateOnDelegation) {
          log.debug("Terminating current workflow due to PROCESS_ARGS[ {} ] = [ {} ]", TERMINATE_ON_DELEGATION,
              terminateOnDelegation);
          workflowSession.terminateWorkflow(workItem.getWorkflow());
        }
      } else {
        log.debug("No valid delegate Workflow Model could be located. Skipping workflow delegation.");
      }
    }
    
    /**
     * Extracts and checkes (including nullchecks) the wf payload and retuns it as
     * resource
     */
    private Resource getContentResourceFromPayload(WorkflowSession workflowSession, WorkItem workItem)
        throws WorkflowException {
      // Get Payload
  	  
      WorkflowData wfData = workItem.getWorkflowData();

      if (!workflowHelper.isPathTypedPayload(wfData)) {
        log.warn("Could not locate a JCR_PATH payload for this workflow. Skipping delegation.");
        return null;
      }
      final String path = wfData.getPayload().toString();

      // Get the resource resolver
      ResourceResolver resolver = workflowHelper.getResourceResolver(workflowSession);
      if (resolver == null) {
        throw new WorkflowException(
            "Could not adapt the WorkflowSession to a ResourceResolver. Something has gone terribly wrong!");
      }

      // Derive the Page or Asset resource so we have a normalized entry-point for
      // finding the /jcr:content resource
      final Resource pageOrAssetResource = workflowHelper.getPageOrAssetResource(resolver, path);
      if (pageOrAssetResource == null) {
        log.warn("Could not resolve [ {} ] to an Asset or Page. Skipping delegation.", path);
        return null;
      }

      // Get the Asset or Page's jcr:content resource, so we have a common
      // inherited property look-up scheme
      final Resource jcrContentResource = pageOrAssetResource.getChild(JcrConstants.JCR_CONTENT);
      if (jcrContentResource == null) {
        throw new WorkflowException(
            String.format("Could not find a child jcr:content resource for [ %s ]", pageOrAssetResource.getPath()));
      }
      return jcrContentResource;
    }
    
    /**
     * Loads the workflow model from /conf 
     * For backward compatiblity with AEM 6.2 this is using com.adobe.granite.confmgr.ConfMgr and should be replaced by Apache Sling Context-Aware Configuration
     */
    private String getWorkflowModelfromConf(Resource jcrContentResource, String propertyName) {
      Conf config = confMgr.getConf(jcrContentResource);
      if (config != null && StringUtils.isNotBlank(propertyName)) {
        ValueMap configSettings = config.getItem(CONFIGURATION);

        if (configSettings != null && configSettings.containsKey(propertyName)) {
          return (String) configSettings.get(propertyName);
        }
      }
      return null;

    }
    
    
    /**
     * Looks for the workflow model in content
     */
    private String getWorflowModelFromContent(Resource jcrContentResource, String contentPropertyName) {
      if (StringUtils.isNotBlank(contentPropertyName)) {
        final InheritanceValueMap inheritance = new HierarchyNodeInheritanceValueMap(jcrContentResource);
        return StringUtils.trim(inheritance.getInherited(contentPropertyName, String.class));
      }
      return null;
    }
    
}