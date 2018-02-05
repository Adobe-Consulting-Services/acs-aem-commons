package com.adobe.acs.commons.ondeploy.impl;

/**
 * Created by brett on 2/2/18.
 */
public class OnDeployScriptTestExampleSuccessWithPause extends OnDeployScriptBase {
    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExample2");
        Thread.sleep(1000);
    }
}
