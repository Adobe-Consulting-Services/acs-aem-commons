/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.workflow.process.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.util.DamUtil;
import com.google.common.collect.ImmutableMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Component(service = WorkflowProcess.class,
        property = "process.label=Synchronize Smart Tags to XMP Metadata Node")
public class SyncSmartTagsToXmpMetadataNodeProcess implements WorkflowProcess {
    private static final Logger log = LoggerFactory.getLogger(SyncSmartTagsToXmpMetadataNodeProcess.class);

    // OOTB Predicate Tags nodes and properties
    private static final String NN_PREDICTED_TAGS = "predictedTags";
    private static final String PN_SMART_TAG_NAME = "name";
    private static final String PN_SMART_TAG_CONFIDENCE = "confidence";

    // Default nodes and properties to write into
    private static final String DEFAULT_NN_SEQUENCE = "dam:predictedTags";
    private static final String DEFAULT_PN_PREDICATED_TAGS_NAME = "dam:predictedTagName";
    private static final String DEFAULT_PN_PREDICATED_TAGS_CONFIDENCE = "dam:predictedTagConfidence";

    private static final Double DEFAULT_MINIMUM_CONFIDENCE = 0.0;

    @Reference
    private WorkflowHelper workflowHelper;

    @Reference
    private WorkflowPackageManager workflowPackageManager;

    @SuppressWarnings("squid:S1141")
    public final void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metaDataMap)
            throws WorkflowException {

        log.debug("Invoked syncSmartTagsToMetadata Workflow Process step for payload [ {} ]", workItem.getWorkflowData().getPayload());

        final ProcessArgs processArgs = new ProcessArgs(metaDataMap);

        try (ResourceResolver resourceResolver = workflowHelper.getResourceResolver(workflowSession)) {
            final List<String> payloads = workflowPackageManager.getPaths(resourceResolver,
                    (String) workItem.getWorkflowData().getPayload());

            final List<Asset> assets = payloads.stream()
                    .map(payload -> DamUtil.resolveToAsset(resourceResolver.getResource(payload)))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());

            assets.stream().forEach(asset -> {
                try {
                    syncSmartTagsToMetadata(asset, processArgs);
                } catch (PersistenceException e) {
                    log.error("Unable to sync Smart Tags to XMP Metadata structure for asset [ {} ]", asset.getPath(), e);
                }
            });

        } catch (RepositoryException e) {
            log.error("Could not find the payload", e);
            throw new WorkflowException("Could not find the payload");
        }
    }

    protected void syncSmartTagsToMetadata(final Asset asset, ProcessArgs processArgs) throws PersistenceException {
        final Resource assetResource = asset.adaptTo(Resource.class);
        final ResourceResolver resourceResolver = assetResource.getResourceResolver();

        final Resource metadataResource = assetResource.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER);
        final Resource smartTagsResource = assetResource.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER + "/" + NN_PREDICTED_TAGS);

            if (metadataResource != null) {
                Resource childResource = metadataResource.getChild(processArgs.getSequenceName());
                if (childResource != null) {
                    // Remove existing, as they will be re-created
                    resourceResolver.delete(childResource);
                }
            }

        final Resource parentResource = resourceResolver.create(metadataResource, processArgs.getSequenceName(),
                new ImmutableMap.Builder<String, Object>()
                        .put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED)
                        .put("xmpArrayType", "rdf:Seq")
                        .put("xmpNodeType", "xmpArray")
                        .put("xmpArraySize", 0L)
                        .build());

        final AtomicInteger count = new AtomicInteger(0);
        if (smartTagsResource != null) {
            StreamSupport.stream(smartTagsResource.getChildren().spliterator(), false)
                    .map(Resource::getValueMap)
                    .filter(properties -> properties.get(PN_SMART_TAG_CONFIDENCE, 0D) >= processArgs.getMinimumConfidence())
                    .filter(properties -> StringUtils.isNotBlank(properties.get(PN_SMART_TAG_NAME, String.class)))
                    .forEach(properties -> {
                        createSequenceItemResource(asset, processArgs, resourceResolver, parentResource, count, properties);
                    });
        }

        parentResource.adaptTo(ModifiableValueMap.class).put("xmpArraySize", count.get());

        log.info("Synced [ {} ] Smart Tags to Asset XMP Metadata structure: [ {} ] ",
                count.get(),
                asset.getPath() + "/jcr:content/metadata/" + processArgs.getSequenceName());
    }

    private void createSequenceItemResource(Asset asset, ProcessArgs processArgs, ResourceResolver resourceResolver,
                                            Resource parentResource, AtomicInteger count, ValueMap properties) {
        try {
            resourceResolver.create(parentResource, String.valueOf(count.incrementAndGet()),
                    new ImmutableMap.Builder<String, Object>()
                            .put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED)
                            .put("xmpNodeType", "xmpStruct")
                            .put(processArgs.getNameProperty(), properties.get(PN_SMART_TAG_NAME, String.class))
                            .put(processArgs.getConfidenceProperty(), properties.get(PN_SMART_TAG_CONFIDENCE, Double.class))
                            .build());
        } catch (PersistenceException e) {
            log.error("Unable to sync Smart Tag [ {} ] to XMP Metadata structure for asset [ {} ]",
                    properties.get("name", String.class), asset.getPath(), e);
        }
    }

    protected static class ProcessArgs {
        private static final String ARG_SEQUENCE_NAME = "sequenceName";
        private static final String ARG_NAME_PROPERTY = "nameProperty";
        private static final String ARG_CONFIDENCE_PROPERTY = "confidenceProperty";
        private static final String ARG_MINIMUM_CONFIDENCE = "minimumConfidence";

        private String sequenceName;
        private String nameProperty;
        private String confidenceProperty;
        private Double minimumConfidence;

        public ProcessArgs(MetaDataMap map) {

            String[] lines = org.apache.commons.lang3.StringUtils.split(map.get(WorkflowHelper.PROCESS_ARGS, ""), System.lineSeparator());
            final Map<String, String> data = ParameterUtil.toMap(lines, "=");

            sequenceName = StringUtils.defaultIfEmpty(data.get(ARG_SEQUENCE_NAME), DEFAULT_NN_SEQUENCE);
            nameProperty = StringUtils.defaultIfEmpty(data.get(ARG_NAME_PROPERTY), DEFAULT_PN_PREDICATED_TAGS_NAME);
            confidenceProperty = StringUtils.defaultIfEmpty(data.get(ARG_CONFIDENCE_PROPERTY), DEFAULT_PN_PREDICATED_TAGS_CONFIDENCE);

            try {
                String tmp = data.get(ARG_MINIMUM_CONFIDENCE);
                if (tmp == null) {
                    minimumConfidence = DEFAULT_MINIMUM_CONFIDENCE;
                } else {
                    minimumConfidence = Double.parseDouble(tmp);
                }
            } catch (NumberFormatException | NullPointerException e) {
                log.warn("Could not parse Double from [ {} ] defaulting to [ {} ]", data.get(ARG_MINIMUM_CONFIDENCE), DEFAULT_MINIMUM_CONFIDENCE);
                minimumConfidence = DEFAULT_MINIMUM_CONFIDENCE;
            }

            if (minimumConfidence < 0 || minimumConfidence > 1) {
                log.warn("Minimum confidence score [ {} ] outside of range [ 0 to 1 ] defaulting to [ {} ]", data.get(ARG_MINIMUM_CONFIDENCE), DEFAULT_MINIMUM_CONFIDENCE);
                minimumConfidence = DEFAULT_MINIMUM_CONFIDENCE;
            }
        }

        public double getMinimumConfidence() {
            return minimumConfidence;
        }

        public String getSequenceName() {
            return sequenceName;
        }

        public String getNameProperty() {
            return nameProperty;
        }

        public String getConfidenceProperty() {
            return confidenceProperty;
        }
    }
}