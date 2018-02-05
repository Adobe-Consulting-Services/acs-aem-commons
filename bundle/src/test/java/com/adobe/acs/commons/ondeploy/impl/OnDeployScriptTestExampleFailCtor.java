package com.adobe.acs.commons.ondeploy.impl;

/**
 * Created by brett on 2/2/18.
 */
public class OnDeployScriptTestExampleFailCtor extends OnDeployScriptBase {
    private OnDeployScriptTestExampleFailCtor() {

    }

    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleFailCtor");
    }
}
