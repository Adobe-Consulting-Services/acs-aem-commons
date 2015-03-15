package com.adobe.acs.commons.workflow.bulk.removal;

import com.adobe.acs.commons.workflow.bulk.removal.impl.exceptions.WorkflowRemovalException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.Calendar;
import java.util.Collection;
import java.util.regex.Pattern;

public interface WorkflowInstanceRemover {

    String WORKFLOW_REMOVAL_PAGE_PATH = "/etc/acs-commons/workflow-remover";

    String WORKFLOW_REMOVAL_STATUS_PATH = WORKFLOW_REMOVAL_PAGE_PATH + "/jcr:content/status";

    String WORKFLOW_INSTANCES_PATH = "/etc/workflow/instances";

    String PN_STATUS = "status";

    String PN_MODEL_ID = "modelId";

    String PN_STARTED_AT = "startedAt";

    String PN_COMPLETED_AT = "completedAt";

    String PN_INITIATED_BY = "initiatedBy";

    String PN_CHECKED_COUNT = "checkedCount";

    String PN_COUNT = "count";

    enum Status {
        RUNNING,
        COMPLETE,
        UNKNOWN
    }

    /**
     * Removes workflow instances that match the parameter criteria.
     *
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> modelIds,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan) throws PersistenceException, WorkflowRemovalException;


    /**
     * Removes workflow instances that match the parameter criteria.
     *
     * @param resourceResolver the resource resolver; must have access to read/delete workflow instances
     * @param modelIds WF Models to remove
     * @param statuses WF Statuses to remove
     * @param payloads Regexes; WF Payloads to remove
     * @param olderThan UTC time in milliseconds; only delete WF's started after this time
     * @param batchSize
     * @return the number of WF instances removed
     */
    int removeWorkflowInstances(final ResourceResolver resourceResolver,
                                final Collection<String> modelIds,
                                final Collection<String> statuses,
                                final Collection<Pattern> payloads,
                                final Calendar olderThan,
                                final int batchSize) throws PersistenceException, WorkflowRemovalException;

}
