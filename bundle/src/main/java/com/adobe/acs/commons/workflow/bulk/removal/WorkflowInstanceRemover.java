package com.adobe.acs.commons.workflow.bulk.removal;

import org.apache.sling.api.resource.ResourceResolver;

public interface WorkflowInstanceRemover {

    public int removeWorkflowInstance(ResourceResolver resourceResolver, String... statuses);

}
