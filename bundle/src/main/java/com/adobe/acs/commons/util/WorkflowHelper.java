/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.util;

import aQute.bnd.annotation.ProviderType;
import com.day.cq.workflow.WorkflowSession;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;

@ProviderType
public interface WorkflowHelper {
    String PROCESS_ARGS = "PROCESS_ARGS";

    /**
     * Convenience method for getting a ResourceResolver object from a Granite based Workflow Process.
     *
     * @param workflowSession the granite workflow session
     * @return the associated ResourceResolver object
     */
    ResourceResolver getResourceResolver(com.adobe.granite.workflow.WorkflowSession workflowSession);

    /**
     * Convenience method for getting a ResourceResolver object from a CQ based Workflow Process.
     *
     * @param workflowSession the CQ workflow session
     * @return the associated ResourceResolver object
     */
    ResourceResolver getResourceResolver(WorkflowSession workflowSession) throws LoginException;
}
