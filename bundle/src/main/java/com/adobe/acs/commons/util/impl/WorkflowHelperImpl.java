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
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkflowData;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.resource.api.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Service
public class WorkflowHelperImpl implements WorkflowHelper {
    private static final Logger log = LoggerFactory.getLogger(WorkflowHelperImpl.class);

    private static final int MAX_GENERIC_QUALITY = 100;

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private MimeTypeService mimeTypeService;

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

    @Override
    public AssetResourceResolverPair getAssetFromPayload(WorkItem item, WorkflowSession workflowSession) {
        Asset asset = null;

        if (item.getWorkflowData().getPayloadType().equals(TYPE_JCR_PATH)) {
            final String path = item.getWorkflowData().getPayload().toString();
            final ResourceResolver resourceResolver;
            try {
                resourceResolver = getResourceResolver(workflowSession);
            } catch (LoginException e) {
                log.warn("Unable to create ResourceResolver from workflow session", e);
                return null;
            }
            final Resource resource = resourceResolver.getResource(path);
            if (null != resource) {
                asset = DamUtil.resolveToAsset(resource);
                if (asset != null) {
                    return new AssetResourceResolverPair(asset, resourceResolver);
                }
            } else {
                log.error("getAssetFromPaylod: asset [{}] in payload of workflow [{}] does not exist.", path,
                        item.getWorkflow().getId());
            }
        }
        return null;
    }

    @Override
    public String getExtension(String mimetype) {
        return mimeTypeService.getExtension(mimetype);
    }

    @Override
    public String[] buildArguments(MetaDataMap metaData) {
        String processArgs = metaData.get(PROCESS_ARGS, String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return new String[0];
        }
    }

    @Override
    public List<String> getValuesFromArgs(String name, String[] args) {
        final String prefix = name + ":";
        final int prefixLength = prefix.length();
        final List<String> values = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                final String value = arg.substring(prefixLength).trim();
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }

    @Override
    public double getQuality(double base, String qualityStr) {
        int q = Integer.parseInt(qualityStr);
        double res = base * q / MAX_GENERIC_QUALITY;
        return res;
    }

    /**
     * @{inheritDoc}
     **/
    @Override
    public final Resource getPageOrAssetResource(ResourceResolver resourceResolver, String path) {
        Resource payloadResource = resourceResolver.getResource(path);

        if (payloadResource == null) {
            return null;
        }

        Asset asset = DamUtil.resolveToAsset(payloadResource);
        if (asset != null) {
            return asset.adaptTo(Resource.class);
        }

        PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(payloadResource);

        if (page != null) {
            return page.adaptTo(Resource.class);
        }

        return null;
    }

    /**
     * @{inheritDoc}
     **/
    @Override
    public boolean isPathTypedPayload(WorkflowData workflowData) {
        return PAYLOAD_TYPE_JCR_PATH.equals(workflowData.getPayloadType());
    }

    /**
     * @{inheritDoc}
     **/
    @Override
    public boolean isPathTypedPayload(com.adobe.granite.workflow.exec.WorkflowData workflowData) {
        return PAYLOAD_TYPE_JCR_PATH.equals(workflowData.getPayloadType());
    }
}