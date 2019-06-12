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

import com.adobe.acs.commons.util.WorkflowHelper;
import com.adobe.acs.commons.workflow.WorkflowPackageManager;
import com.adobe.cq.dam.mac.sync.api.DAMSyncService;
import com.adobe.granite.workflow.WorkflowException;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.metadata.MetaDataMap;
import com.adobe.granite.workflow.metadata.SimpleMetaDataMap;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationActionType;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.testing.mock.osgi.junit.OsgiContext;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.util.ArrayList;
import java.util.List;

import javax.jcr.RepositoryException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(PowerMockRunner.class)
@PrepareForTest(DamUtil.class)
public final class BrandPortalSyncProcessTest {

    private static final String ASSET_PATH = "/content/dam/foo.png";

    private static final String PROCESS_ARGS = "PROCESS_ARGS";

    private static final String ACTIVATE_SMALL = "activate";

    private static final String DEACTIVATE_SMALL = "deactivate";

    private static final String ACTIVATE_CAPITALS = StringUtils.upperCase(ACTIVATE_SMALL);

    private static final String DEACTIVATE_CAPITALS = StringUtils.upperCase(DEACTIVATE_SMALL);

    @Rule
    public final OsgiContext osgiContext = new OsgiContext();

    @Rule
    public final SlingContext context = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @InjectMocks
    private final BrandPortalSyncProcess workflowProcess = new BrandPortalSyncProcess();

    private final MetaDataMap metadataMap = new SimpleMetaDataMap();

    private final List<String> paths = new ArrayList<>();

    @Mock
    private DAMSyncService damSyncService;

    @Mock
    private WorkflowHelper workflowHelper;

    @Mock
    private WorkflowPackageManager workflowPackageManager;

    @Mock
    private Asset asset;

    @Mock
    private WorkItem workItem;

    @Mock
    private WorkflowData workflowData;

    @Mock
    private WorkflowSession workflowSession;

    @Before
    public void setUp() throws RepositoryException {
        PowerMockito.mockStatic(DamUtil.class);

        paths.add(ASSET_PATH);

        when(workItem.getWorkflowData()).thenReturn(workflowData);
        when(workflowData.getPayload()).thenReturn(ASSET_PATH);

        when(workflowHelper.getResourceResolver(workflowSession)).thenReturn(context.resourceResolver());
        when(workflowPackageManager.getPaths(eq(context.resourceResolver()), anyString())).thenReturn(paths);
        when(asset.getPath()).thenReturn(ASSET_PATH);
        when(DamUtil.resolveToAsset(any(Resource.class))).thenReturn(asset);
    }

    @Test
    public void execute_activate() throws WorkflowException {
        putProcessArgs(ACTIVATE_SMALL);
        workflowProcess.execute(workItem, workflowSession, metadataMap);
        verify(damSyncService, times(1)).publishResourcesToMP(any(List.class), eq(context.resourceResolver()));
    }

    @Test
    public void execute_deactivate() throws WorkflowException {
        putProcessArgs(DEACTIVATE_SMALL);
        workflowProcess.execute(workItem, workflowSession, metadataMap);
        verify(damSyncService, times(1)).unpublishResourcesFromMP(any(List.class), eq(context.resourceResolver()));
    }

    @Test
    public void getReplicationActionType_activate()  {
        putProcessArgs(ACTIVATE_SMALL);
        assertReplicationActionType(ReplicationActionType.ACTIVATE);
    }

    @Test
    public void getReplicationActionType_activate_caps() {
        putProcessArgs(ACTIVATE_CAPITALS);
        assertReplicationActionType(ReplicationActionType.ACTIVATE);
    }

    @Test
    public void getReplicationActionType_deactivate() {
        putProcessArgs(DEACTIVATE_CAPITALS);
        assertReplicationActionType(ReplicationActionType.DEACTIVATE);
    }

    @Test
    public void getReplicationActionType_deactivate_caps() {
        putProcessArgs(DEACTIVATE_CAPITALS);
        assertReplicationActionType(ReplicationActionType.DEACTIVATE);
    }

    @Test
    public void getReplicationActionType_null() {
        putProcessArgs("null");
        assertNull(getReplicationActionType());
    }

    private void putProcessArgs(final String args) {
        metadataMap.put(PROCESS_ARGS, args);
    }

    private void assertReplicationActionType(final ReplicationActionType type) {
        assertEquals(ReplicationActionType.ACTIVATE, getReplicationActionType());
    }

    private ReplicationActionType getReplicationActionType() {
        return workflowProcess.getReplicationActionType(metadataMap);
    }

}
