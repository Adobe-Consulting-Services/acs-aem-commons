package com.adobe.acs.commons.workflow.synthetic;

import aQute.bnd.annotation.ProviderType;

import java.util.Map;

/**
 * Created by dgonzale on 6/25/17.
 */
@ProviderType
public interface SyntheticWorkflowStep {
    String getId();

    Map<String, Object> getMetadataMap();

    SyntheticWorkflowRunner.WorkflowProcessIdType getIdType();
}
