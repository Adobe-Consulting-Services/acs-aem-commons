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

import com.adobe.acs.commons.util.impl.WorkflowHelperImpl;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.acs.commons.workflow.impl.WorkflowPackageManagerImpl;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.exec.Workflow;
import com.adobe.granite.workflow.exec.WorkflowProcess;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;
import com.day.cq.workflow.exec.WorkItem;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.resource.collection.ResourceCollectionManager;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;

@RunWith(MockitoJUnitRunner.class)
public class CopyPropertiesProcessTest {

    @Rule
    public AemContext ctx = new AemContext();

    @Mock
    WorkflowPackageManager workflowPackageManager;

    @Before
    public void setUp() throws Exception {
        ctx.load().json(getClass().getResourceAsStream("CopyPropertiesProcessTest.json"), "/content/dam");

        ctx.registerService(new WorkflowHelperImpl());
        ctx.registerService(WorkflowPackageManager.class, workflowPackageManager);
        ctx.registerInjectActivateService(new CopyPropertiesProcess());
    }


    /**
     * IF the property doesn’t exist on the source, THEN do nothing (AEM will never remove a property entirely, so empty values are allowed after a property is created)
     */
    @Test
    public void copyProperties_sourceWithMissingProperty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime => ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime -> ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-without-destination-with.png/jcr:content");

        assertFalse("dam:onTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-without-destination-with.png/jcr:content/metadata").getValueMap().containsKey("dam:onTime"));
        assertFalse("dam:offTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-without-destination-with.png/jcr:content/metadata").getValueMap().containsKey("dam:offTime"));
    }

    /**
     * IF the property exists on the source AND is empty on the source AND the property exists on the destination THEN set the destination property to empty.
     */
    @Test
    public void copyProperties_sourceWithEmptyProperty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-empty-destination-with.png");

        assertFalse("dam:onTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-with.png/jcr:content/metadata").getValueMap().containsKey("dam:onTime"));
        assertFalse("dam:offTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-with.png/jcr:content/metadata").getValueMap().containsKey("dam:offTime"));

        assertNull(ctx.resourceResolver().getResource("/content/dam/source-empty-destination-with.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertNull(ctx.resourceResolver().getResource("/content/dam/source-empty-destination-with.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    /**
     * IF the property doesn’t exist on the source, THEN remove the property from the destination
     */
    @Test
    public void copyProperties_sourceWithMissingProperty_destinationWithProperty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-empty-destination-without.png");

        assertFalse("dam:onTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-without.png/jcr:content/metadata").getValueMap().containsKey("dam:onTime"));
        assertFalse("dam:offTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-without.png/jcr:content/metadata").getValueMap().containsKey("dam:offTime"));
    }

    /**
     * IF the property exists on the source AND is empty on the source AND the property does not exist on the destination THEN do nothing (leave the destination alone)
     */
    @Test
    public void copyProperties_sourceWithProperty_destinationWithoutProperty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-empty-destination-without.png");

        assertFalse("dam:onTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-without.png/jcr:content/metadata").getValueMap().containsKey("dam:onTime"));
        assertFalse("dam:offTime property should be removed completely.", ctx.resourceResolver().getResource("/content/dam/source-empty-destination-without.png/jcr:content/metadata").getValueMap().containsKey("dam:offTime"));
    }

    @Test
    public void copyProperties_sourceWithPropertyValue_destinationWithoutProperty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-with-destination-without.png");

        assertEquals("monday", ctx.resourceResolver().getResource("/content/dam/source-with-destination-without.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertEquals("thursday", ctx.resourceResolver().getResource("/content/dam/source-with-destination-without.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    @Test
    public void copyProperties_sourceWithPropertyValue_destinationWithPropertyValue() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/source-with-destination-with.png/jcr:content");

        assertEquals("monday", ctx.resourceResolver().getResource("/content/dam/source-with-destination-with.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertEquals("thursday", ctx.resourceResolver().getResource("/content/dam/source-with-destination-with.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    @Test
    public void propertyResource_absolutePath_getValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "/content/dam/copy-properties.png/jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertEquals("monday", actual.getValue());
    }

    @Test
    public void propertyResource_relativePath_getValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertEquals("monday", actual.getValue());
    }

    @Test
    public void propertyResource_absolutePath_setValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "/content/dam/copy-properties.png/jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        actual.setValue("friday");
        assertEquals("friday", ctx.resourceResolver().getResource("/content/dam/copy-properties.png/jcr:content").getValueMap().get("onTime", String.class));
    }

    @Test
    public void propertyResource_relativePath_setValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        actual.setValue("friday");
        assertEquals("friday", ctx.resourceResolver().getResource("/content/dam/copy-properties.png/jcr:content").getValueMap().get("onTime", String.class));
    }

    @Test
    public void propertyResource_propertyExists_True() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertTrue(actual.propertyExists());
    }

    @Test
    public void propertyResource_propertyExists_False() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/missingTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertFalse(actual.propertyExists());
    }

    @Test
    public void hasValue_True() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertTrue(actual.hasValue());
    }

    @Test
    public void hasValue_BlankFalse() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/unknownTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertFalse(actual.hasValue());
    }

    @Test
    public void hasValue_MissingFalse() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/missingTime", "/content/dam/copy-properties.png", ctx.resourceResolver());

        assertFalse(actual.hasValue());
    }
}