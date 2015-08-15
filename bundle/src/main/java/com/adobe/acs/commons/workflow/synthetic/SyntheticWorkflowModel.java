package com.adobe.acs.commons.workflow.synthetic;

import java.util.Map;

public interface SyntheticWorkflowModel {

    /**
     *
     * @return
     */
    String[] getWorkflowProcessNames();

    /**
     *
     * @return
     */
    Map<String, Map<String, Object>> getSyntheticWorkflowModelData();
}
