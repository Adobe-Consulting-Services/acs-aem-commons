package com.adobe.acs.commons.workflow.synthetic;

import java.util.Map;

/**
 * Created by dgonzale on 6/25/17.
 */
public interface SyntheticWorkflowStep {
    String getId();

    Map<String, Object> getMetadataMap();

    SyntheticWorkflowRunner.WorkflowProcessIdType getIdType();
}
