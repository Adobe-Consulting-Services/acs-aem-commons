package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;

import java.util.Map;

public class SyntheticWorkflowStepImpl implements com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowStep {
    private final String id;
    public Map<String, Object> metadataMap;
    public SyntheticWorkflowRunner.WorkflowProcessIdType idType;


    public SyntheticWorkflowStepImpl(String id, Map<String, Object> metadataMap) {
        this.idType = SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_NAME;
        this.id = id;
        this.metadataMap = metadataMap;
    }

    public SyntheticWorkflowStepImpl(String id, SyntheticWorkflowRunner.WorkflowProcessIdType type, Map<String, Object> metadataMap) {
        this.idType = type;
        this.id = id;
        this.metadataMap = metadataMap;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public Map<String, Object> getMetadataMap() {
        return metadataMap;
    }

    @Override
    public SyntheticWorkflowRunner.WorkflowProcessIdType getIdType() {
        return idType;
    }
}
