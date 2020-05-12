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

    @Test
    public void copyProperties() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime => ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime -> ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/one.png/jcr:content");

        assertEquals("monday", ctx.resourceResolver().getResource("/content/dam/one.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertEquals("sunday", ctx.resourceResolver().getResource("/content/dam/one.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    @Test
    public void copyProperties_remove() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });
        metaDataMap.put("SKIP_EMPTY_SOURCE_PROPERTY", Boolean.FALSE);

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/two.png");

        assertNull(ctx.resourceResolver().getResource("/content/dam/two.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertNull(ctx.resourceResolver().getResource("/content/dam/two.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    @Test
    public void copyProperties_skipEmpty() {
        MetaDataMap metaDataMap = new SimpleMetaDataMap();
        metaDataMap.put("PROPERTY_MAP", new String[]{
                "./jcr:content/onTime -> ./jcr:content/metadata/dam:onTime",
                "./jcr:content/offTime => ./jcr:content/metadata/dam:offTime"
        });

        CopyPropertiesProcess workflowProcess = (CopyPropertiesProcess) ctx.getService(WorkflowProcess.class);

        workflowProcess.copyProperties(metaDataMap, ctx.resourceResolver(), "/content/dam/two.png");

        assertEquals("tuesday", ctx.resourceResolver().getResource("/content/dam/two.png/jcr:content/metadata").getValueMap().get("dam:onTime", String.class));
        assertNull(ctx.resourceResolver().getResource("/content/dam/two.png/jcr:content/metadata").getValueMap().get("dam:offTime", String.class));
    }

    @Test
    public void propertyResource_absolutePath_getValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "/content/dam/one.png/jcr:content/onTime", "/content/dam/two.png", ctx.resourceResolver());

        assertEquals("monday", actual.getValue());
    }

    @Test
    public void propertyResource_relativePath_getValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/one.png", ctx.resourceResolver());

        assertEquals("monday", actual.getValue());
    }

    @Test
    public void propertyResource_absolutePath_setValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "/content/dam/one.png/jcr:content/onTime", "/content/dam/two.png", ctx.resourceResolver());

        actual.setValue("friday", false);
        assertEquals("friday", ctx.resourceResolver().getResource("/content/dam/one.png/jcr:content").getValueMap().get("onTime", String.class));
    }

    @Test
    public void propertyResource_relativePath_setValue() throws WorkflowException {
        CopyPropertiesProcess.PropertyResource actual = new CopyPropertiesProcess.PropertyResource(
                "./jcr:content/onTime", "/content/dam/one.png", ctx.resourceResolver());

        actual.setValue("friday", false);
        assertEquals("friday", ctx.resourceResolver().getResource("/content/dam/one.png/jcr:content").getValueMap().get("onTime", String.class));
    }
}