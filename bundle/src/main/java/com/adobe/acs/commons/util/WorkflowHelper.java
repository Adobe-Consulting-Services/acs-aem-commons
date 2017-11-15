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
import com.day.cq.dam.api.Asset;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.metadata.MetaDataMap;
import com.day.cq.workflow.exec.WorkflowData;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import java.util.List;

@ProviderType
@SuppressWarnings("squid:S1214")
public interface WorkflowHelper {
    String PROCESS_ARGS = "PROCESS_ARGS";
    String TYPE_JCR_PATH = "JCR_PATH";
    String PAYLOAD_TYPE_JCR_PATH = "JCR_PATH";

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

    /**
     * Resolve the asset for the workflow's payload and return it, along with a resolved resource resolver.
     *
     * @param item the workflow workitem
     * @param workflowSession the workflow session
     * @return a tuple containing the asset and resource resolver or null if the asset cannot be resolved
     */
    WorkflowHelper.AssetResourceResolverPair getAssetFromPayload(WorkItem item, WorkflowSession workflowSession);

    /**
     * Return the extension corresponding to the mime type.
     *
     * @param mimetype the mimetype
     * @return the corresponding extension
     */
    String getExtension(String mimetype);

    /**
     * Build an arguments array from the metadata map.
     *
     * @param metaData the metadata maps
     * @return the values array
     */
    String[] buildArguments(MetaDataMap metaData);

    /**
     * Parse a workflow args string in the formaat &gt;name&lt;:&gt;value&lt;,&gt;name&lt;:&gt;value&lt; and
     * extract the values with the specified name.
     *
     * @param name the argument name
     * @param args the arguments array
     * @return the values list
     */
    List<String> getValuesFromArgs(String name, String[] args);

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
    double getQuality(double base, String qualityStr);

    /**
     * A simple tuple which contains a resolved asset and a resource resolver, so that the resource resolver
     * can later be closed.
     */
    final class AssetResourceResolverPair {

        public final Asset asset;
        public final ResourceResolver resourceResolver;

        public AssetResourceResolverPair(Asset asset, ResourceResolver resourceResolver) {
            this.asset = asset;
            this.resourceResolver = resourceResolver;
        }

    }

    /**
     * Derives either an Asset or Page resource (dam:Asset or cq:Page) that the provided path belongs to.
     * Example: When path = /content/dam/foo.png/jcr:content/renditions/original, this method will return the resource at /content/dam/foo.png
     * Example: When path = /content/site/bar/jcr:content/root/text, this method will return the resource at /content/site/bar
     * @param resourceResolver the resourceResolver to resolve the path to the appropriate resource
     * @param path the path to resolve to an Asset or Page
     * @return the resource representing the resolver dam:Asset or cq:Page, if neither can be resolved, null is returned.
     */
    Resource getPageOrAssetResource(ResourceResolver resourceResolver, String path);

    /**
     * Method for CQ Workflow APIs.
     * @param workflowData the Workflow data
     * @return true of the WorkflowData payload is of type JCR_PATH
     */
    boolean isPathTypedPayload(WorkflowData workflowData);

    /**
     * Method for Granite Workflow APIs.
     * @param workflowData the Workflow data
     * @return true of the WorkflowData payload is of type JCR_PATH
     */
    boolean isPathTypedPayload(com.adobe.granite.workflow.exec.WorkflowData workflowData);
}
