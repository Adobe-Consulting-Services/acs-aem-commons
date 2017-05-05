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
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.sling.api.resource.LoginException;
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
                description = "Syncs assets with AEM Assets Brand Portal"
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
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private DAMSyncService damSyncService;

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resourceResolver = null;
        final List<String> assetPaths = new ArrayList<>();

        final ReplicationActionType replicationActionType = getReplicationActionType(metaDataMap);

        try {
            resourceResolver = workflowHelper.getResourceResolver(workflowSession);

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, (String) workItem.getWorkflowData().getPayload());

            for (final String payload : payloads) {

                final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(payload));

                if (asset == null) {
                    log.debug("Payload path [ {} ] does not resolve to an asset", payload);
                } else {
                    assetPaths.add(asset.getPath());
                }
            }

            if (ReplicationActionType.ACTIVATE.equals(replicationActionType)) {
                damSyncService.publishResourcesToMP(assetPaths, resourceResolver);
            } else if (ReplicationActionType.DEACTIVATE.equals(replicationActionType)) {
                damSyncService.unpublishResourcesFromMP(assetPaths, resourceResolver);
            } else {
                log.warn("Unknown replication action type [ {} ] for AEM Assets Brand Portal Sync", replicationActionType);
            }
        } catch (LoginException e) {
            log.error("Could not get a ResourceResolver object from the WorkflowSession", e);
            throw new WorkflowException("Could not get a ResourceResolver object from the WorkflowSession");
        } catch (RepositoryException e) {
            log.error("Could not find the payload", e);
            throw new WorkflowException("Could not find the payload");
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private ReplicationActionType getReplicationActionType(MetaDataMap metaDataMap) {
        final String processArgs = StringUtils.trim(metaDataMap.get("PROCESS_ARGS", ReplicationActionType.ACTIVATE.getName()));

        if (StringUtils.equalsIgnoreCase(processArgs, ReplicationActionType.ACTIVATE.getName())) {
           return ReplicationActionType.ACTIVATE;
        } else if (StringUtils.equalsIgnoreCase(processArgs, ReplicationActionType.DEACTIVATE.getName())) {
            return ReplicationActionType.DEACTIVATE;
        }

        return null;
    }
}