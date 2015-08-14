package com.adobe.acs.commons.workflow.synthetic;

import java.util.Map;

public interface SyntheticWorkflowModel {

    /**
     *
     * @return
     */
    String[] getWorkflowModelNames();

    /**
     *
     * @return
     */
    Map<String, Map<String, Object>> getSyntheticWorkflowModelData();
}
