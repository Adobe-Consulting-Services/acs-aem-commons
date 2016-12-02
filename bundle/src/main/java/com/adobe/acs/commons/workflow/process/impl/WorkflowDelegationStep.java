package com.adobe.acs.commons.workflow.process.impl;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.jcr.Session;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
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
 * 
 * This workflow steps just calls another workflow as subworkflow.
 * 
 * The subworkflow is determined by looking up the correct workflow on the 
 * resource (or its ascendents). Works on pages and assets.
 * 
 * In order to support multiple instances of this behaviour, the name of the property,
 * which determines the model of the subworkflow, can be be configured as arguments on
 * the workflow step.
 * 
 * Arguments should be provided as this:
 * 
 * workflowModelProperty=<propName>,defaultWorkflowModel=<pathToDefaultWorkflowModel>
 *  ( eg: /etc/workflow/models/request_for_activation/jcr:content/model)
 * 
 * where <propName> is the name of the property which contains the paths of the workflow
 * models
 * 
 * and <pathToDefaultWorkflowModel> the path to the default Workflow Model, which is used
 * as fallback.
 * 
 * If no workflow model can be resolved (directly and as fallback), a workflow exception is thrown.
 * 
 *
 */

@Service()
@Component()
@Properties({
    @Property(
            label = "Workflow Label",
            name = "process.label",
            value = "Workflow Delegation",
            description = "Invokes a new workflow for this payload based on a content-hierarchy based configuration"
    )
})
public class WorkflowDelegationStep implements WorkflowProcess {
	
	
	@Reference
	ResourceResolverFactory rrf;
	
	@Reference
	WorkflowHelper wfHelper;

	private static final Logger log = LoggerFactory.getLogger(WorkflowDelegationStep.class);
	
	// Under this property the workflow model is stored
	private static final String WORKFLOW_PROPERTY_NAME = "workflowModelProperty";
	
	// A default workflow model can be provided as fallback
	private static final String DEFAULT_WORKFLOW_MODEL = "defaultWorkflowModel";
	
	@Override
	public void execute(WorkItem item, WorkflowSession session, MetaDataMap metadata)
			throws WorkflowException {
		
		Map<String,String> args = getProcessArgsMap(metadata);
		
		String propertyName = args.get(WORKFLOW_PROPERTY_NAME);
		if (StringUtils.isBlank(propertyName)) {
			throw new WorkflowException ("PROCESS_ARG " + WORKFLOW_PROPERTY_NAME + " not defined");
		}
		String defaultWorkflowModel = args.get(DEFAULT_WORKFLOW_MODEL);
		
		log.debug ("propertyName = {}, default workflow model = {}", 
				new Object[]{propertyName,defaultWorkflowModel});
		
		ResourceResolver resolver = null;
		WorkflowData wfData = item.getWorkflowData();
		if (wfData.getPayloadType().equals("JCR_PATH")) {
			
			String path = wfData.getPayload().toString();
			resolver = wfHelper.getResourceResolver(session);
			
			if (resolver != null) {
				Resource wfResource = resolver.getResource(path);
				InheritanceValueMap inheritance = new HierarchyNodeInheritanceValueMap(wfResource);
				String foundWorkflowModel = inheritance.getInherited(propertyName, String.class);
				
				if (foundWorkflowModel != null) {
					log.debug ("Using workflowmodel {}", foundWorkflowModel);
				} else {
					if (StringUtils.isBlank(defaultWorkflowModel)) {
						throw new WorkflowException 
							(String.format("Haven't found property %s on resource %s or higher, also defaultWorkflowModel not defined", 
								new Object[]{propertyName,defaultWorkflowModel}));
					} else {
						foundWorkflowModel = defaultWorkflowModel;
						log.debug("Falling back to configured default workflowmodel {}", foundWorkflowModel);
					}
				} 
				
				WorkflowModel newModel = session.getModel(foundWorkflowModel);
				session.startWorkflow(newModel, wfData);
				log.info("Delegating workflow with payload {} to workflow model {}", 
						new Object[]{wfData.getPayload() ,foundWorkflowModel});
				
			}
		}
	}

    private Map<String, String> getProcessArgsMap(MetaDataMap metaDataMap) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        final String processArgs = metaDataMap.get(WorkflowHelper.PROCESS_ARGS, "");
        return ParameterUtil.toMap(StringUtils.split(processArgs,","),"=");

        
    }
}
