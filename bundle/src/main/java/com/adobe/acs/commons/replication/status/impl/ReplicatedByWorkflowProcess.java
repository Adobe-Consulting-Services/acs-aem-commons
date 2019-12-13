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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

/**
 * ACS AEM Commons - Workflow Process - Replicated By Workflow Initiator
 * Assigns the replicatedBy property to the Workflow Initiator
 */
@Component
@Property(
        label = "Workflow Label",
        name = "process.label",
        value = "Set Replicated By Property to Workflow Initiator",
        description = "Sets the Replicated By Property on the payload to the Workflow Initiator"
)
@Service
public class ReplicatedByWorkflowProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(ReplicatedByWorkflowProcess.class);

    private static final String AUTHENTICATION_INFO_SESSION = "user.jcr.session";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private WorkflowPackageManager workflowPackageManager;
    
    @Reference
    private ReplicationStatusManager replStatusManager;

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
        final String payloadPath = workflowData.getPayload().toString();

        // Get ResourceResolver
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(AUTHENTICATION_INFO_SESSION, workflowSession.getSession());

        try (ResourceResolver resourceResolver = resourceResolverFactory.getResourceResolver(authInfo)) {

            // Get replicated by value
            final String replicatedBy = StringUtils.defaultIfEmpty(workItem.getWorkflow().getInitiator(),
                    "Unknown Workflow User");

            final List<String> paths = workflowPackageManager.getPaths(resourceResolver, payloadPath);

            for (final String path : paths) {
                // For each item in the WF Package, or if not a WF Package, path = payloadPath

                Resource resource = replStatusManager.getReplicationStatusResource(path, resourceResolver);

                final ModifiableValueMap mvm = resource.adaptTo(ModifiableValueMap.class);

                if (StringUtils.isNotBlank(mvm.get(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY,
                        String.class))) {
                    mvm.put(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);
                    resourceResolver.commit();
                    log.trace("Set replicateBy to [ {} ] on resource  [ {} ]", replicatedBy, resource.getPath());
                } else {
                    log.trace("Skipping; Resource does not have replicateBy property set  [ {} ]",
                            resource.getPath());
                }
            }
        } catch (LoginException e) {
            log.error("Could not acquire a ResourceResolver object from the Workflow Session's JCR Session: {}", e);
        } catch (PersistenceException e) {
            log.error("Could not save replicateBy property for payload [ {} ] due to: {}", payloadPath, e);
        } catch (RepositoryException e) {
            log.error("Could not collect Workflow Package items for payload [ {} ] due to: {}", payloadPath, e);
        }
    }
}
