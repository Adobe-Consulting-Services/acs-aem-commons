package com.adobe.acs.commons.workflow.bulk.execution.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientWorkflowUtil {
    private static final Logger log  = LoggerFactory.getLogger(TransientWorkflowUtil.class);

    public static boolean isTransient(ResourceResolver resourceResolver, String workflowModelId) {
            Resource resource = resourceResolver.getResource(workflowModelId);
            boolean transientValue = resource.getValueMap().get("metaData/transient", resource.getValueMap().get("transient", false));;

            log.debug("Getting transient state for [ {} ]  at [ {} ]", resource.getPath() + "/metaData/transient", transientValue);

            return transientValue;
    }
}
