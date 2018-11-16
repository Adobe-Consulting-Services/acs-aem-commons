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

import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component(
        metatype = true,
        label = "ACS AEM Commons - Workflow Process - DAM Metadata Property Reset",
        description = "Replaces DAM Asset metadata properties with other values from the metadata node"
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "DAM Metadata Property Reset",
                description = "Replaces DAM Asset metadata properties with other values from the metadata node"
        )
})
@Service
public class DamMetadataPropertyResetProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(DamMetadataPropertyResetProcess.class);

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Override
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) throws WorkflowException {
        ResourceResolver resourceResolver = null;
        String wfPayload = null;

        try {
            resourceResolver = this.getResourceResolver(workflowSession.getSession());
            wfPayload = (String) workItem.getWorkflowData().getPayload();

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, wfPayload);
            final Map<String, String> srcDestMap = this.getProcessArgsMap(metaDataMap);

            for (final String payload : payloads) {

                final Asset asset = DamUtil.resolveToAsset(resourceResolver.getResource(payload));

                if (asset == null) {
                    log.debug("Payload path [ {} ] does not resolve to an asset", payload);
                    continue;
                }

                String metadataPath = String.format("%s/%s/%s",asset.getPath(), JcrConstants.JCR_CONTENT, DamConstants.METADATA_FOLDER);
                Resource metadataResource = resourceResolver.getResource(metadataPath);

                if (metadataResource == null) {
                    log.error("Could not find the metadata node for Asset [ " + asset.getPath() + " ]");
                    throw new WorkflowException("Could not find the metadata node for Asset [ " + asset.getPath() + " ]");
                }

                final ModifiableValueMap mvm = metadataResource.adaptTo(ModifiableValueMap.class);

                for (final Map.Entry<String, String> entry : srcDestMap.entrySet()) {
                    final String srcProperty = entry.getValue();
                    final String destProperty = entry.getKey();

                    if(mvm.get(srcProperty) != null) {
                        // Remove dest property first in case Types differ
                        mvm.remove(destProperty);

                        // If the src value is NOT null, update the dest property
                        mvm.put(destProperty, mvm.get(srcProperty));
                    } else if (mvm.containsKey(srcProperty)) {
                        // Else if the src value IS null, AND the src property exists on the node, remove the dest property
                        mvm.remove(destProperty);
                    } 
                    // Else leave the dest property alone since there is no source defined to overwrite it with

                    // Remove the source
                    mvm.remove(srcProperty);
                }
            }
        } catch (LoginException e) {
            throw new WorkflowException("Could not get a ResourceResolver object from the WorkflowSession", e);
        } catch (RepositoryException e) {
            throw new WorkflowException(String.format("Could not find the payload for '%s'", wfPayload), e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private Map<String, String> getProcessArgsMap(MetaDataMap metaDataMap) {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        final String processArgs = metaDataMap.get("PROCESS_ARGS", "");
        final String[] lines = StringUtils.split(processArgs, ",");

        for (final String line : lines) {
            final String[] entry = StringUtils.split(line, "=");

            if (entry.length == 2) {
                map.put(entry[0], entry[1]);
            }
        }

        return map;
    }

    private ResourceResolver getResourceResolver(Session session) throws LoginException {
        final Map<String, Object> authInfo = new HashMap<String, Object>();
        authInfo.put(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, session);
        return resourceResolverFactory.getResourceResolver(authInfo);
    }
}



