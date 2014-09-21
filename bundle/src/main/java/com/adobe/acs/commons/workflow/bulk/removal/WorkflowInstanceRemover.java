package com.adobe.acs.commons.workflow.bulk.removal;

import org.apache.sling.api.resource.ResourceResolver;

import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;

public interface WorkflowInstanceRemover {

    /**
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param models WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regex; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> models,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan);


    /**
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param models WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regex; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @param limit
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> models,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan,
                                final int limit);

}
