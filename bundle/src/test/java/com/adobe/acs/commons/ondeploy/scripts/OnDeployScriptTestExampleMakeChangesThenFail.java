/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
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
