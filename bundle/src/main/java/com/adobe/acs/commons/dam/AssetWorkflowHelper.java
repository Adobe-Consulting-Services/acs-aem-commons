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
package com.adobe.acs.commons.dam;

import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.mime.MimeTypeService;
import org.apache.sling.jcr.resource.JcrResourceConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Utility methods which support the writing of asset-related workflow processes, largely to avoid a
 * dependency on AbstractAssetWorkflowProcess which tends to change between AEM versions.
 */
public final class AssetWorkflowHelper {
    private static final Logger log = LoggerFactory.getLogger(AssetWorkflowHelper.class);

    private static final int MAX_GENERIC_QUALITY = 100;

    static final String TYPE_JCR_PATH = "JCR_PATH";

    private AssetWorkflowHelper() {}


    /**
     * Return the extension corresponding to the mime type.
     *
     * @param mimetype the mimetype
     * @param mimeTypeService the mimetype service
     * @return the corresponding extension
     */
    public static String getExtension(String mimetype, MimeTypeService mimeTypeService) {
        return mimeTypeService.getExtension(mimetype);
    }

    /**
     * Parse a workflow args string in the formaat &gt;name&lt;:&gt;value&lt;,&gt;name&lt;:&gt;value&lt; and
     * extract the values with the specified name.
     *
     * @param name the argument name
     * @param args the arguments array
     * @return the values list
     */
    public static List<String> getValuesFromArgs(String name, String args[]) {
        final String prefix = name + ":";
        final int prefixLength = name.length();
        final List<String> values = new ArrayList<String>();
        for (String arg : args) {
            if (arg.startsWith(prefix)) {
                final String value = arg.substring(prefixLength).trim();
                values.add(value);
            }
        }
        return Collections.unmodifiableList(values);
    }


    /**
     * Parse the provided quality string, from 1 to 100, and
     * apply it to the base. Allows for a constant scale to be used
     * and applied to different image types which support different
     * quality scales.
     *
     * @param base the maximal quality value
     * @param qualityStr the string to parse
     * @return a usable quality value
     */
    public static double getQuality(double base, String qualityStr) {
        int q = Integer.parseInt(qualityStr);
        double res = base * q / MAX_GENERIC_QUALITY;
        return res;
    }

    /**
     * Resolve the asset for the workflow's payload and return it, along with a resolved resource resolver.
     *
     * @param item the workflow workitem
     * @param workflowSession the workflow session
     * @param resourceResolverFactory a resource resolver factory
     * @return a tuple containing the asset and resource resolver or null if the asset cannot be resolved
     */
    public static AssetResourceResolverPair getAssetFromPayload(final WorkItem item, final WorkflowSession workflowSession, final ResourceResolverFactory resourceResolverFactory) {
        Asset asset = null;

        if (item.getWorkflowData().getPayloadType().equals(TYPE_JCR_PATH)) {
            final String path = item.getWorkflowData().getPayload().toString();
            final ResourceResolver resourceResolver = getResourceResolver(workflowSession, resourceResolverFactory);
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

    /**
     * Obtain a resource resolver from a worklfow session.
     *
     * @param workflowSession the workflow session
     * @param resourceResolverFactory a resource resolver factory
     * @return a resource resolver
     */
    public static ResourceResolver getResourceResolver(final WorkflowSession workflowSession, final ResourceResolverFactory resourceResolverFactory) {
        try {
            Map<String, Object> authInfo = Collections.singletonMap(JcrResourceConstants.AUTHENTICATION_INFO_SESSION, (Object) workflowSession.getSession());
            return resourceResolverFactory.getResourceResolver(authInfo);
        } catch (LoginException e) {
            log.error("failed to get resource resolver", e);
            return null;
        }
    }

    /**
     * Build an arguments array from the metadata map.
     *
     * @param metaData the metadata maps
     * @return the values array
     */
    public static String[] buildArguments(MetaDataMap metaData) {
        String processArgs = metaData.get("PROCESS_ARGS", String.class);
        if (processArgs != null && !processArgs.equals("")) {
            return processArgs.split(",");
        } else {
            return new String[0];
        }
    }

    /**
     * A simple tuple which contains a resolved asset and a resource resolver, so that the resource resolver
     * can later be closed.
     */
    public static final class AssetResourceResolverPair {

        public final Asset asset;
        public final ResourceResolver resourceResolver;

        private AssetResourceResolverPair(Asset asset, ResourceResolver resourceResolver) {
            this.asset = asset;
            this.resourceResolver = resourceResolver;
        }

    }

}
