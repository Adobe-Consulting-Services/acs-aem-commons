/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.workflow.process.impl;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.cq.dam.mac.sync.api.DAMSyncService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationActionType;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;

@Component(
        metatype = true,
        label = "ACS AEM Commons - Workflow Process - Brand Portal Sync",
        description = "Syncs assets with AEM Assets Brand Portal."
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Brand Portal Sync",
                description = "Syncs (publish/unpublish) assets with AEM Assets Brand Portal"
        )
})
@Service
public class BrandPortalSyncProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(BrandPortalSyncProcess.class);

    @Reference
    private WorkflowHelper workflowHelper;

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @Reference
    private DAMSyncService damSyncService;

    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        final List<String> assetPaths = new ArrayList<String>();

        final ReplicationActionType replicationActionType = getReplicationActionType(metaDataMap);

        try (ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession)) {

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, (String) workItem.getWorkflowData().getPayload());

            for (final String payload : payloads) {
                // Convert the payloads to Assets, in preparation for Brand Portal publication
                // Note that this only supports Assets as payloads and NOT Asset Folders
                final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(payload));

                if (asset == null) {
                    log.debug("Payload path [ {} ] does not resolve to an asset", payload);
                } else {
                    assetPaths.add(asset.getPath());
                }
            }

            // Based on the WF Process activation/deactivation directive; leverage the DamSyncService to publish the the Asset
            if (ReplicationActionType.ACTIVATE.equals(replicationActionType)) {
                damSyncService.publishResourcesToMP(assetPaths, resourceResolver);
            } else if (ReplicationActionType.DEACTIVATE.equals(replicationActionType)) {
                damSyncService.unpublishResourcesFromMP(assetPaths, resourceResolver);
            } else {
                log.warn("Unknown replication action type [ {} ] for AEM Assets Brand Portal Sync", replicationActionType);
            }
        } catch (RepositoryException e) {
            log.error("Could not find the payload", e);
            throw new WorkflowException("Could not find the payload");
        }
    }

    protected final ReplicationActionType getReplicationActionType(MetaDataMap metaDataMap) {
        final String processArgs = StringUtils.trim(metaDataMap.get("PROCESS_ARGS", ReplicationActionType.ACTIVATE.getName()));

        if (StringUtils.equalsIgnoreCase(processArgs, ReplicationActionType.ACTIVATE.getName())) {
           return ReplicationActionType.ACTIVATE;
        } else if (StringUtils.equalsIgnoreCase(processArgs, ReplicationActionType.DEACTIVATE.getName())) {
            return ReplicationActionType.DEACTIVATE;
        }

        return null;
    }
}