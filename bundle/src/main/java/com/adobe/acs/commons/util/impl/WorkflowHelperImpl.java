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

package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.workflow.WorkflowSession;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.JcrResourceConstants;

import javax.jcr.Session;
import java.util.HashMap;
import java.util.Map;

@Component
@Service
public class WorkflowHelperImpl implements WorkflowHelper {

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    /**
     * @{inheritDoc}
     **/
    @Override
    public final ResourceResolver getResourceResolver(com.adobe.granite.workflow.WorkflowSession workflowSession) {
        return workflowSession.adaptTo(ResourceResolver.class);
    }

    /**
     * @{inheritDoc}
     **/
    @Override
    public final ResourceResolver getResourceResolver(WorkflowSession workflowSession) throws LoginException {
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        return resourceResolverFactory.getResourceResolver(authInfo);
    }
}