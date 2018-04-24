package com.adobe.acs.commons.workflow.synthetic.impl;

import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class SyntheticWorkflowStepImplTest {
    @Test
    public void getId() throws Exception {
        assertEquals("test", new SyntheticWorkflowStepImpl("test", null).getId());
    }

    @Test
    public void getMetadataMap() throws Exception {
        Map<String, Object> map = new HashMap<>();
        assertEquals(map, new SyntheticWorkflowStepImpl("test", map).getMetadataMap());
    }

    @Test
    public void getIdType_Default() throws Exception {
        assertEquals(SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_NAME,
                new SyntheticWorkflowStepImpl("test", null).getIdType());
    }

    @Test
    public void getIdType_Specified() throws Exception {
        assertEquals(SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_LABEL,
                new SyntheticWorkflowStepImpl("test", SyntheticWorkflowRunner.WorkflowProcessIdType.PROCESS_LABEL, null).getIdType());
    }
}