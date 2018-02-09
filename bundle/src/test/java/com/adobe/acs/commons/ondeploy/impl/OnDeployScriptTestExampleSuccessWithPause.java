package com.adobe.acs.commons.ondeploy.impl;

public class OnDeployScriptTestExampleSuccessWithPause extends OnDeployScriptBase {
    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleSuccessWithPause");
        Thread.sleep(1000);
    }
}
