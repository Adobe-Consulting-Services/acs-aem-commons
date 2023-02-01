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

import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.Resource;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;

public class SyncSmartTagsToXmpMetadataNodeProcessTest {

    @Rule
    public AemContext ctx = new AemContext();

    final MetaDataMap metaDataMap = new SimpleMetaDataMap();

    final SyncSmartTagsToXmpMetadataNodeProcess workflowProcess = new SyncSmartTagsToXmpMetadataNodeProcess();

    @Before
    public void setUp() throws Exception {
        ctx.load().json("/com/adobe/acs/commons/workflow/process/impl/SyncSmartTagsToXmpMetadataNodeTest.json", "/content/dam");
    }

    @Test
    public void testSyncSmartTagsToMetadata() throws Exception {
        SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs processArgs = new SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs(metaDataMap);

        Asset asset = DamUtil.resolveToAsset(ctx.resourceResolver().getResource("/content/dam/asset.png"));
        workflowProcess.syncSmartTagsToMetadata(asset, processArgs);

        Resource sequenceResource = ctx.resourceResolver().getResource("/content/dam/asset.png/jcr:content/metadata/dam:predictedTags");

        assertNotNull(sequenceResource);
        assertEquals(3L, (long)sequenceResource.getValueMap().get("xmpArraySize", Long.class));

        assertNotNull(sequenceResource.getChild("1"));
        assertEquals("apple", sequenceResource.getChild("1").getValueMap().get("dam:predictedTagName", String.class));
        assertEquals(1.0D, sequenceResource.getChild("1").getValueMap().get("dam:predictedTagConfidence", Double.class), 0.01);

        assertNotNull(sequenceResource.getChild("2"));
        assertEquals("orange", sequenceResource.getChild("2").getValueMap().get("dam:predictedTagName", String.class));
        assertEquals(0.5D, sequenceResource.getChild("2").getValueMap().get("dam:predictedTagConfidence", Double.class), 0.01);

        assertNotNull(sequenceResource.getChild("3"));
        assertEquals("banana", sequenceResource.getChild("3").getValueMap().get("dam:predictedTagName", String.class));
        assertEquals(0.1D, sequenceResource.getChild("3").getValueMap().get("dam:predictedTagConfidence", Double.class), 0.01);
    }

    @Test
    public void testSyncSmartTagsToMetadata_WithSequenceName() throws Exception {
        metaDataMap.put("PROCESS_ARGS", "sequenceName=dam:custom");

        SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs processArgs = new SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs(metaDataMap);

        Asset asset = DamUtil.resolveToAsset(ctx.resourceResolver().getResource("/content/dam/asset.png"));
        workflowProcess.syncSmartTagsToMetadata(asset, processArgs);

        Resource sequenceResource = ctx.resourceResolver().getResource("/content/dam/asset.png/jcr:content/metadata/dam:custom");
        assertNotNull(sequenceResource);
        assertNotNull(sequenceResource.getChild("1"));
        assertNotNull(sequenceResource.getChild("2"));
        assertNotNull(sequenceResource.getChild("3"));
    }

    @Test
    public void testSyncSmartTagsToMetadata_WithMinimumConfidence() throws Exception {
        metaDataMap.put("PROCESS_ARGS", "minimumConfidence=0.8");

        SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs processArgs = new SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs(metaDataMap);

        Asset asset = DamUtil.resolveToAsset(ctx.resourceResolver().getResource("/content/dam/asset.png"));
        workflowProcess.syncSmartTagsToMetadata(asset, processArgs);

        Resource sequenceResource = ctx.resourceResolver().getResource("/content/dam/asset.png/jcr:content/metadata/dam:predictedTags");

        assertNotNull(sequenceResource);
        assertEquals(1L, (long)sequenceResource.getValueMap().get("xmpArraySize", Long.class));

        assertNotNull(sequenceResource.getChild("1"));
        assertEquals("apple", sequenceResource.getChild("1").getValueMap().get("dam:predictedTagName", String.class));
        assertEquals(1.0D, sequenceResource.getChild("1").getValueMap().get("dam:predictedTagConfidence", Double.class), 0.01);

        assertNull(sequenceResource.getChild("2"));
        assertNull(sequenceResource.getChild("3"));
    }


    @Test
    public void testSyncSmartTagsToMetadata_WithNameProperty() throws Exception {
        metaDataMap.put("PROCESS_ARGS", "nameProperty=dam:customName");

        SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs processArgs = new SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs(metaDataMap);

        Asset asset = DamUtil.resolveToAsset(ctx.resourceResolver().getResource("/content/dam/asset.png"));
        workflowProcess.syncSmartTagsToMetadata(asset, processArgs);

        Resource sequenceResource = ctx.resourceResolver().getResource("/content/dam/asset.png/jcr:content/metadata/dam:predictedTags");

        assertNotNull(sequenceResource);
        assertEquals(3L, (long)sequenceResource.getValueMap().get("xmpArraySize", Long.class));

        assertNotNull(sequenceResource.getChild("1"));
        assertEquals("apple", sequenceResource.getChild("1").getValueMap().get("dam:customName", String.class));

        assertNotNull(sequenceResource.getChild("2"));
        assertEquals("orange", sequenceResource.getChild("2").getValueMap().get("dam:customName", String.class));

        assertNotNull(sequenceResource.getChild("3"));
        assertEquals("banana", sequenceResource.getChild("3").getValueMap().get("dam:customName", String.class));
    }


    @Test
    public void testSyncSmartTagsToMetadata_WithConfidenceProperty() throws Exception {
        metaDataMap.put("PROCESS_ARGS", "confidenceProperty=dam:customConfidence");
        SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs processArgs = new SyncSmartTagsToXmpMetadataNodeProcess.ProcessArgs(metaDataMap);

        Asset asset = DamUtil.resolveToAsset(ctx.resourceResolver().getResource("/content/dam/asset.png"));
        workflowProcess.syncSmartTagsToMetadata(asset, processArgs);

        Resource sequenceResource = ctx.resourceResolver().getResource("/content/dam/asset.png/jcr:content/metadata/dam:predictedTags");

        assertNotNull(sequenceResource);
        assertEquals(3L, (long)sequenceResource.getValueMap().get("xmpArraySize", Long.class));

        assertNotNull(sequenceResource.getChild("1"));
        assertEquals(1.0D, sequenceResource.getChild("1").getValueMap().get("dam:customConfidence", Double.class), 0.01);

        assertNotNull(sequenceResource.getChild("2"));
        assertEquals(0.5D, sequenceResource.getChild("2").getValueMap().get("dam:customConfidence", Double.class), 0.01);

        assertNotNull(sequenceResource.getChild("3"));
        assertEquals(0.1D, sequenceResource.getChild("3").getValueMap().get("dam:customConfidence", Double.class), 0.01);
    }
}