/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import com.day.cq.wcm.resource.details.AssetDetails;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.List;


@Component(
        metatype = true,
        label = "ACS AEM Commons - Workflow Process - Set Image Orientation",
        description = "Determine image orientation from width/height and update image tags"
)
@Properties({
        @Property(
                label = "Workflow Label",
                name = "process.label",
                value = "Set Image Orientation",
                description = "Determine image orientation from width/height and update image tags"
        )
})
@Service
public class SetImageOrientationProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(SetImageOrientationProcess.class);

    @SuppressWarnings("unused")
    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @SuppressWarnings("unused")
    @Reference
    private WorkflowHelper workflowHelper;

    private static final String DEFAULT_CONFIG = ">1.1 properties:orientation/landscape\n" +
            "<0.9 properties:orientation/portrait\n" +
            "default properties:orientation/square";

    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap) {

        Configuration config = new Configuration(metaDataMap);

        try {

            ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession);

            TagManager tagManager = resourceResolver.adaptTo(TagManager.class);

            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver, (String) workItem.getWorkflowData().getPayload());
            for (final String payload : payloads) {

                log.debug("Processing {}", payload);

                Resource payloadResource = resourceResolver.resolve(payload);

                Asset asset = DamUtil.resolveToAsset(payloadResource);
                Resource assetResource = asset.adaptTo(Resource.class);
                if (assetResource != null) {
                    AssetDetails assetDetails = new AssetDetails(assetResource);

                    String tagId = getOrientation(assetDetails, config);

                    log.debug("Orientation tag is {}", tagId);

                    if (tagId != null) {
                        Resource metadataResource = getMetadataResource(assetResource);
                        Tag[] currentTags = tagManager.getTags(metadataResource);
                        Tag[] updatedTags = (Tag[]) ArrayUtils.add(currentTags, tagManager.resolve(tagId));
                        tagManager.setTags(metadataResource, updatedTags, true);
                        log.debug("Orientation tag set");
                    }
                }

            }

        } catch (RepositoryException re) {
            log.error("Repository Exception", re);
        }

    }

    private Resource getMetadataResource(Resource resource) {
        return resource.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER);
    }

    protected String getOrientation(AssetDetails assetDetails, Configuration config) {

        long width = 0;
        long height = 0;
        try {
            width = assetDetails.getWidth();
            height = assetDetails.getHeight();
        } catch (RepositoryException re) {
            log.warn("Unable to get width / height for asset: {}", assetDetails.getAsset().getPath());
        }

        log.debug("Width {} Height {}", width, height);

        if (width > 0 && height > 0) {
            float ratio = (float) width / height;

            log.debug("Ratio is {}", ratio);

            for (ConfigRule rule : config.getConfig()) {
                if (rule.operator.equals(">") && ratio > rule.limit) {
                    return rule.tagId;
                }
                if (rule.operator.equals("<") && ratio < rule.limit) {
                    return rule.tagId;
                }

                if (rule.operator.equals("default")) {
                    return rule.tagId;
                }

            }
        }

        // Asset doesn't have width/height so we can't decide correct orientation
        log.debug("Couldn't calculate orientation");
        return null;
    }


    /**
     * Inner class for parsing and storing workflow step configuration
     */
    static class Configuration {

        private final List<ConfigRule> config;

        public Configuration(MetaDataMap metaDataMap) {
            final String processArgs = StringUtils.trim(metaDataMap.get("PROCESS_ARGS", DEFAULT_CONFIG));

            // Create empty config
            config = new ArrayList<>();

            // Parse configuration line by line
            for (String arg : processArgs.split("\n")) {
                String tagId = arg.substring(arg.indexOf(" ") + 1);
                String firstPart = arg.substring(0, arg.indexOf(" "));
                float limit = 1;
                String operator;
                if (firstPart.equals("default")) {
                    operator = firstPart;
                } else {
                    operator = arg.substring(0, 1);
                    limit = Float.parseFloat(firstPart.substring(1));
                }

                ConfigRule rule = new ConfigRule(operator, limit, tagId);
                config.add(rule);

                // Default must be always the last config line
                if (operator.equals("default")) {
                    break;
                }
            }
        }

        public List<ConfigRule> getConfig() {
            return config;
        }
    }

    // Class to represent each config option (line of text) in parsed format
    static class ConfigRule {
        private final String operator; // Can be only '<' '>' 'default'
        private final float limit;
        private final String tagId;

        ConfigRule(String operator, float limit, String tagId) {
            this.operator = operator;
            this.limit = limit;
            this.tagId = tagId;
        }

    }
}
