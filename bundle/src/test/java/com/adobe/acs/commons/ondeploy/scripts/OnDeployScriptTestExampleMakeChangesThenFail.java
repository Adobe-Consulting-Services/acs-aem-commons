package com.adobe.acs.commons.ondeploy.scripts;

import java.util.Collections;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

public class OnDeployScriptTestExampleMakeChangesThenFail extends OnDeployScriptBase {

    private static final String CREATED_NAME = "should-not-see-me";
    public static final String CREATED_PATH = "/" + CREATED_NAME;

    @Override
    protected void execute() throws Exception {
        ResourceResolver resolver = getResourceResolver();
        Resource parent = resolver.getResource("/");
        resolver.create(parent, CREATED_NAME, Collections.emptyMap());

        throw new RuntimeException("Oops, this script failed");
    }

}
