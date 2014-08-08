package com.adobe.acs.commons.workflow.bulk.removal.impl;

import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.commons.scheduler.Job;
import org.apache.sling.commons.scheduler.JobContext;

import java.io.Serializable;
import java.util.Map;

public class WorkflowRemovalJob implements Job {
    @Reference
    private WorkflowInstanceRemoverImpl

    @Override
    public void execute(JobContext jobContext) {
        final Map<String, Serializable> config = jobContext.getConfiguration();





    }
}
