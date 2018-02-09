package com.adobe.acs.commons.ondeploy.scripts;

import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptBase;

public class OnDeployScriptTestExampleFailExecute extends OnDeployScriptBase {
    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleFailExecute");
        throw new RuntimeException("Oops, this script failed");
    }
}
