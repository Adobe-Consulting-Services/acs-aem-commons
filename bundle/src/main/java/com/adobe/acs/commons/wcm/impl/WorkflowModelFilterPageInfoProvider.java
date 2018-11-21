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
package com.adobe.acs.commons.wcm.impl;

import java.util.Iterator;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.PageInfoProvider;

/**
 * PageInfoProvider which filters the Workflow models available for a
 * page based on a regular expression in the "applyTo" property on a workflow
 * model's jcr:content node.
 * 
 * Must run <b>after</b> <code>com.day.cq.wcm.core.impl.DefaultPageStatusProvider</code>
 */
@Component
@Service
@SuppressWarnings( "deprecation" )
public class WorkflowModelFilterPageInfoProvider implements PageInfoProvider {

    private static final String KEY_MODELS = "models";

    private static final String KEY_MODEL_PATH = "wid";

    private static final String KEY_WORKFLOWS = "workflows";

    private static final String PN_ALLOWED_PATHS = "allowedPaths";

    private static final Logger log = LoggerFactory.getLogger(WorkflowModelFilterPageInfoProvider.class);

    @Override
    @SuppressWarnings( "deprecation" )
    public void updatePageInfo(SlingHttpServletRequest request, org.apache.sling.commons.json.JSONObject info, Resource resource)
            throws org.apache.sling.commons.json.JSONException {
        if (info.has(KEY_WORKFLOWS)) {
            final org.apache.sling.commons.json.JSONObject workflows = info.getJSONObject(KEY_WORKFLOWS);
            final String resourcePath = resource.getPath();
            final ResourceResolver resourceResolver = resource.getResourceResolver();
            for (final Iterator<String> types = workflows.keys(); types.hasNext();) {
                final String type = types.next();
                final org.apache.sling.commons.json.JSONObject typeObject = workflows.getJSONObject(type);
                filter(typeObject, resourcePath, resourceResolver);
            }
        } else {
            log.warn("No workflows found in existing page info. Check order of cq:infoProviders.");
        }
    }

    @SuppressWarnings("squid:S3776")
    private void filter(org.apache.sling.commons.json.JSONObject typeObject, String resourcePath, ResourceResolver resourceResolver) 
            throws org.apache.sling.commons.json.JSONException {
        final org.apache.sling.commons.json.JSONArray models = typeObject.getJSONArray(KEY_MODELS);
        final org.apache.sling.commons.json.JSONArray newModels = new org.apache.sling.commons.json.JSONArray();
        for (int i = 0; i < models.length(); i++) {
            final org.apache.sling.commons.json.JSONObject modelObject = models.getJSONObject(i);
            final String path = modelObject.getString(KEY_MODEL_PATH);
            final Resource modelResource = resourceResolver.getResource(path);
            if (modelResource != null) {
                // we're looking for the appliesTo property on the jcr:content node, the wid value
                // is the path to the jcr:content/model node.
                final ValueMap properties = modelResource.getParent().getValueMap();
                final String[] allowedPaths = properties.get(PN_ALLOWED_PATHS, String[].class);
                if (allowedPaths == null) {
                    newModels.put(modelObject);
                } else {
                    for (final String allowedPath : allowedPaths) {
                        if (resourcePath.matches(allowedPath)) {
                            newModels.put(modelObject);
                            break;
                        }
                    }
                }
            }
        }
        
        typeObject.put(KEY_MODELS, newModels);
    }

}
