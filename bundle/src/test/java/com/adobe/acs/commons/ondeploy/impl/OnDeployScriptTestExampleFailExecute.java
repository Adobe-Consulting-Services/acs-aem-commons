package com.adobe.acs.commons.ondeploy.impl;

public class OnDeployScriptTestExampleFailExecute extends OnDeployScriptBase {
    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleFailExecute");
        throw new RuntimeException("Oops, this script failed");
    }
}
