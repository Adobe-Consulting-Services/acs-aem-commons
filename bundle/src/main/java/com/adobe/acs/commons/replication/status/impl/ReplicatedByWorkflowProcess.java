/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.replication.status.impl;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Component(
        label = "ACS AEM Commons - Workflow Process - Replicated By Workflow Initiator",
        description = "Assigns the replicatedBy property to the Workflow Initiator"
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Set Replicated By Property to Workflow Initiator",
                description = "Sets the Replicated By Property on the payload to the Workflow Initiator"
        )
})
public class ReplicatedByWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(ReplicatedByWorkflowProcess.class);

    private static final String AUTHENTICATION_INFO_SESSION = "user.jcr.session";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession,
                         MetaDataMap args) throws WorkflowException {
        final WorkflowData workflowData = workItem.getWorkflowData();

        final String type = workflowData.getPayloadType();

        // Check if the payload is a path in the JCR
        if (!StringUtils.equals(type, "JCR_PATH")) {
            return;
        }
        // Get the path to the JCR resource from the payload
        final String path = workflowData.getPayload().toString();

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(AUTHENTICATION_INFO_SESSION, workflowSession.getSession());
        final ResourceResolver resourceResolver;

        try {
            resourceResolver = resourceResolverFactory.getResourceResolver(authInfo);

            // Get replicated by value
            final String replicatedBy = StringUtils.defaultIfEmpty(workItem.getWorkflow().getInitiator(),
                    "Unknown Workflow User");

            final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
            final Page page = pageManager.getContainingPage(path);
            final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(path));

            Resource resource = null;

            if (page != null) {
                // Page
                resource = page.getContentResource();
                log.trace("Candidate Page for setting replicateBy is [ {} ]", resource.getPath());
            } else if (asset != null) {
                // DAM Asset
                final Resource assetResource = resourceResolver.getResource(asset.getPath());
                resource = assetResource.getChild(JcrConstants.JCR_CONTENT);
                log.trace("Candidate Asset for setting replicateBy is [ {} ]", resource.getPath());
            } else {
                // Some other resource
                resource = resourceResolver.getResource(path);
                log.trace("Candidate Resource for setting replicateBy is [ {} ]", resource.getPath());
            }

            if (resource != null) {
                final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

                if (StringUtils.isNotBlank(mvm.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, String.class))) {
                    mvm.put(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);
                    resourceResolver.commit();
                    log.trace("Set replicateBy to [ {} ] on resource  [ {} ]", replicatedBy, resource.getPath());
                } else {
                    log.trace("Skipping; Resource does not have replicateBy property set  [ {} ]",
                            resource.getPath());
                }
            }
        } catch (LoginException e) {
            log.error("Could not acquire a ResourceResolver object from the Workflow Session's JCR Session: {}",
                    e.getMessage());
            e.printStackTrace();
        } catch (PersistenceException e) {
            log.error("Could not save replicateBy property for payload [ {} ] due to: {}", path, e.getMessage());
        }
    }
}
