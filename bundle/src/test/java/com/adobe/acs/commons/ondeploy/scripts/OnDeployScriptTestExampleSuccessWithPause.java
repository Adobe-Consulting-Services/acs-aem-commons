package com.adobe.acs.commons.ondeploy.scripts;

import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptBase;

public class OnDeployScriptTestExampleSuccessWithPause extends OnDeployScriptBase {
    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleSuccessWithPause");
        Thread.sleep(1000);
    }
}
