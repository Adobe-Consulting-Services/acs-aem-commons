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
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;

import com.day.cq.dam.api.DamConstants;
import com.day.cq.dam.commons.handler.StandardImageHandler;
import com.day.cq.tagging.Tag;
import com.day.cq.tagging.TagManager;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class SetImageOrientationProcessTest {

    // Need to use JCR_OAK because AssetDetails needs access to Node
    @Rule
    public final AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    @Mock
    WorkItem workItem;

    @Mock
    WorkflowData workflowData;

    @Mock
    WorkflowPackageManager workflowPackageManager;

    @Mock
    WorkflowHelper workflowHelper;

    @Mock
    WorkflowSession workflowSession;

    @InjectMocks
    SetImageOrientationProcess workflowProcess = new SetImageOrientationProcess();


    TagManager tagManager;
    MetaDataMap metadataMap;
    List<String> paths;
    final String assetPath = "/content/dam/asset.png";

    @Before
    public void setUp() throws Exception {



        metadataMap = new SimpleMetaDataMap();
        paths = new ArrayList<>();
        paths.add(assetPath);

        tagManager = context.resourceResolver().adaptTo(TagManager.class);

        tagManager.createTag("properties:orientation/landscape", "Landscape", "");
        tagManager.createTag("properties:orientation/portrait", "Portrait", "");
        tagManager.createTag("properties:orientation/square", "Square", "");

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(assetPath);

        when(workflowHelper.getResourceResolver(workflowSession)).thenReturn(context.resourceResolver());
        when(workflowPackageManager.getPaths(eq(context.resourceResolver()), anyString())).thenReturn(paths);


    }


    @Test
    public void testSquareOrientation() throws Exception {

        // Create test asset
        context.create().asset(assetPath, 10, 10, StandardImageHandler.JPEG_MIMETYPE);

        // Execute process
        workflowProcess.execute(workItem, workflowSession, metadataMap);

        // Check that tag has been set correctly
        assertEquals("properties:orientation/square", getTagId());
    }



    @Test
    public void testLandscapeOrientation() throws Exception {

        // Create test asset
        context.create().asset(assetPath, 100, 50, StandardImageHandler.JPEG_MIMETYPE);

        // Execute process
        workflowProcess.execute(workItem, workflowSession, metadataMap);

        // Check that tag has been set correctly
        assertEquals("properties:orientation/landscape", getTagId());
    }

    @Test
    public void testPortraitOrientation() throws Exception {

        // Create test asset
        context.create().asset(assetPath, 10, 100, StandardImageHandler.JPEG_MIMETYPE);

        // Execute process
        workflowProcess.execute(workItem, workflowSession, metadataMap);

        // Check that tag has been set correctly
        assertEquals("properties:orientation/portrait", getTagId());
    }

    @Test
    public void testCustomConfiguration_superwide_orientation() throws Exception {
        metadataMap.put("PROCESS_ARGS", ">5 properties:orientation/superwide\r\ndefault properties:orientation/default");
        tagManager.createTag("properties:orientation/superwide", "Superwide", "");
        tagManager.createTag("properties:orientation/default", "Default", "");

        // Create test asset
        context.create().asset(assetPath, 1000, 100, StandardImageHandler.JPEG_MIMETYPE);

        // Execute process
        workflowProcess.execute(workItem, workflowSession, metadataMap);

        // Check that tag has been set correctly
        assertEquals("properties:orientation/superwide", getTagId());
    }

    @Test
    public void testCustomConfiguration_default_orientation() throws Exception {
        metadataMap.put("PROCESS_ARGS", ">5 properties:orientation/superwide\r\ndefault properties:orientation/default");
        tagManager.createTag("properties:orientation/superwide", "Superwide", "");
        tagManager.createTag("properties:orientation/default", "Default", "");

        // Create test asset
        context.create().asset(assetPath, 100, 100, StandardImageHandler.JPEG_MIMETYPE);

        // Execute process
        workflowProcess.execute(workItem, workflowSession, metadataMap);

        // Check that tag has been set correctly
        assertEquals("properties:orientation/default", getTagId());
    }

    private String getTagId() {
        Resource resource = context.resourceResolver().resolve(assetPath);
        Tag[] tags = tagManager.getTags(resource.getChild(JcrConstants.JCR_CONTENT + "/" + DamConstants.METADATA_FOLDER));
        return tags[0].getTagID();
    }


}
