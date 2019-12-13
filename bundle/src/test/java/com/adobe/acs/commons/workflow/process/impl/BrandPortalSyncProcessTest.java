/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.cq.dam.mac.sync.api.DAMSyncService;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.replication.ReplicationActionType;

//@RunWith(PowerMockRunner.class)
//@PrepareForTest(DamUtil.class)
@RunWith(MockitoJUnitRunner.class)
public class BrandPortalSyncProcessTest {



    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Mock
    DAMSyncService damSyncService;

    @Mock
    WorkflowHelper workflowHelper;

    @Mock
    WorkflowPackageManager workflowPackageManager;

    Asset asset;

    @Mock
    WorkItem workItem;

    @Mock
    WorkflowData workflowData;

    @Mock
    WorkflowSession workflowSession;

    @InjectMocks
    BrandPortalSyncProcess workflowProcess = new BrandPortalSyncProcess();

    MetaDataMap metadataMap;

    List<String> paths;

    String assetPath = "/content/dam/foo.png";

    @Before
    public void setUp() throws Exception {

        context.build().resource(assetPath, "jcr:primaryType", "dam:Asset").commit();
        context.build().resource(assetPath + "/jcr:content", "cq:name", "foo").commit();
        Resource assetResource = context.resourceResolver().getResource(assetPath);
        asset = assetResource.adaptTo(Asset.class);

        metadataMap = new SimpleMetaDataMap();
        paths = new ArrayList<>();
        paths.add(assetPath);

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(assetPath);

        when(workflowHelper.getResourceResolver(workflowSession)).thenReturn(context.resourceResolver());
        when(workflowPackageManager.getPaths(eq(context.resourceResolver()), anyString())).thenReturn(paths);
    }

    @Test
    public void execute_activate() throws Exception {
        metadataMap.put("PROCESS_ARGS", "activate");
        workflowProcess.execute(workItem, workflowSession, metadataMap);
        verify(damSyncService, times(1)).publishResourcesToMP(any(List.class), eq(context.resourceResolver()));
    }

    @Test
    public void execute_deactivate() throws Exception {
        metadataMap.put("PROCESS_ARGS", "deactivate");
        workflowProcess.execute(workItem, workflowSession, metadataMap);
        verify(damSyncService, times(1)).unpublishResourcesFromMP(any(List.class), eq(context.resourceResolver()));
    }

    @Test
    public void getReplicationActionType_activate() throws Exception {
        metadataMap.put("PROCESS_ARGS", "activate");
        assertEquals(ReplicationActionType.ACTIVATE, workflowProcess.getReplicationActionType(metadataMap));
    }

    @Test
    public void getReplicationActionType_activate_caps() throws Exception {
        metadataMap.put("PROCESS_ARGS", "ACTIVATE");
        assertEquals(ReplicationActionType.ACTIVATE, workflowProcess.getReplicationActionType(metadataMap));
    }

    @Test
    public void getReplicationActionType_deactivate() throws Exception {
        metadataMap.put("PROCESS_ARGS", "DEACTIVATE");
        assertEquals(ReplicationActionType.DEACTIVATE, workflowProcess.getReplicationActionType(metadataMap));
    }

    @Test
    public void getReplicationActionType_deactivate_caps() throws Exception {
        metadataMap.put("PROCESS_ARGS", "DEACTIVATE");
        assertEquals(ReplicationActionType.DEACTIVATE, workflowProcess.getReplicationActionType(metadataMap));
    }

    @Test
    public void getReplicationActionType_null() throws Exception {
        metadataMap.put("PROCESS_ARGS", "null");
        assertNull(workflowProcess.getReplicationActionType(metadataMap));
    }
}