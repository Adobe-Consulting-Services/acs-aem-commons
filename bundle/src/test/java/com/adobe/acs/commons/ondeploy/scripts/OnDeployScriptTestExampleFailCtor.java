package com.adobe.acs.commons.ondeploy.scripts;

import com.adobe.acs.commons.ondeploy.scripts.OnDeployScriptBase;

public class OnDeployScriptTestExampleFailCtor extends OnDeployScriptBase {
    private OnDeployScriptTestExampleFailCtor() {

    }

    @Override
    protected void execute() throws Exception {
        logger.info("Executing test script: OnDeployScriptTestExampleFailCtor");
    }
}
