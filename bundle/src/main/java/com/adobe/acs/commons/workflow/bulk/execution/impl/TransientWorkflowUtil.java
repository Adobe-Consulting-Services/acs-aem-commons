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

package com.adobe.acs.commons.workflow.bulk.execution.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TransientWorkflowUtil {
    private static final Logger log  = LoggerFactory.getLogger(TransientWorkflowUtil.class);

    private TransientWorkflowUtil() {}

    public static boolean isTransient(ResourceResolver resourceResolver, String workflowModelId) {
            Resource resource = resourceResolver.getResource(workflowModelId);
            boolean transientValue = resource.getValueMap().get("metaData/transient", resource.getValueMap().get("transient", false));;

            log.debug("Getting transient state for [ {} ]  at [ {} ]", resource.getPath() + "/metaData/transient", transientValue);

            return transientValue;
    }
}
