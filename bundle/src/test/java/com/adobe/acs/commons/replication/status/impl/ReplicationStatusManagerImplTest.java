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

package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.dam.api.Asset;
import com.day.cq.dam.commons.util.DamUtil;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.Calendar;

import static junit.framework.Assert.assertEquals;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({JcrUtil.class, DamUtil.class})
public class ReplicationStatusManagerImplTest {

    @Mock
    NodeType replicationNodeType;

    @Spy
    ReplicationStatusManagerImpl replicationStatusManager = new ReplicationStatusManagerImpl();

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;
    
    @Mock
    PageManager pageManager;
    
    
    static final String PAGE_PATH = "/content/page";

    @Mock
    Page pagePage;
    
    @Mock
    Resource pageContentResource;
    
    
    static final String ASSET_PATH = "/content/asset";

    @Mock
    Resource assetResource;

    @Mock
    Asset assetAsset;
    
    @Mock
    Resource assetContentResource;


    static final String UNREPLICATED_PATH = "/content/unreplicated";

    @Mock
    Resource unreplicatedResource;

    @Mock
    Node unreplicatedNode;


    static final String REPLICATED_PATH = "/content/replicated";

    @Mock
    Resource replicatedResource;

    @Mock
    Node replicatedNode;



    @Before
    public void setUp() throws Exception {
        when(replicationNodeType.getName()).thenReturn(ReplicationStatus.NODE_TYPE);

        when(resourceResolver.adaptTo(Session.class)).thenReturn(session);
        when(resourceResolver.adaptTo(PageManager.class)).thenReturn(pageManager);
        
        /* Page */
        when(pageManager.getContainingPage(PAGE_PATH)).thenReturn(pagePage);
        when(pagePage.getContentResource()).thenReturn(pageContentResource);
        
        /* Asset */
        PowerMockito.mockStatic(DamUtil.class);
        when(resourceResolver.getResource(ASSET_PATH)).thenReturn(assetResource);
        when(DamUtil.resolveToAsset(assetResource)).thenReturn(assetAsset);
        when(assetAsset.getPath()).thenReturn(ASSET_PATH);
        when(assetResource.getChild(JcrConstants.JCR_CONTENT)).thenReturn(assetContentResource);

        /* Unreplicated Node */
        when(resourceResolver.getResource(UNREPLICATED_PATH)).thenReturn(unreplicatedResource);
        when(unreplicatedResource.adaptTo(Node.class)).thenReturn(unreplicatedNode);
        when(unreplicatedNode.getSession()).thenReturn(session);
        when(unreplicatedNode.isNodeType(ReplicationStatus.NODE_TYPE)).thenReturn(false);
        when(unreplicatedNode.getMixinNodeTypes()).thenReturn(new NodeType[] { });
        when(unreplicatedNode.canAddMixin(ReplicationStatus.NODE_TYPE)).thenReturn(true);

        /* Replicated Node */
        when(resourceResolver.getResource(REPLICATED_PATH)).thenReturn(replicatedResource);
        when(replicatedResource.adaptTo(Node.class)).thenReturn(replicatedNode);
        when(replicatedNode.getSession()).thenReturn(session);
        when(replicatedNode.isNodeType(ReplicationStatus.NODE_TYPE)).thenReturn(true);
        when(replicatedNode.getMixinNodeTypes()).thenReturn(new NodeType[] { replicationNodeType });
    }
    
    @Test
    public void testGetReplicationStatusResource_Page() {
    	assertEquals(pageContentResource, replicationStatusManager.getReplicationStatusResource(PAGE_PATH, resourceResolver));
    }
    
    @Test
    public void testGetReplicationStatusResource_Asset() {
    	assertEquals(assetContentResource, replicationStatusManager.getReplicationStatusResource(ASSET_PATH, resourceResolver));
    }
    
    @Test
    public void testGetReplicationStatusResource_Resource() {
    	assertEquals(unreplicatedResource, replicationStatusManager.getReplicationStatusResource(UNREPLICATED_PATH, resourceResolver));
    }

    @Test
    public void testSetReplicationStatus_Activate() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        final String replicationStatus = "Activate";
        final String replicatedBy = "Test User";
        final Calendar replicatedAt = Calendar.getInstance();
        replicatedAt.set(1,1);

        replicationStatusManager.setReplicationStatus(resourceResolver,
                replicatedBy,
                replicatedAt,
                ReplicationStatusManager.Status.ACTIVATED,
                UNREPLICATED_PATH);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(unreplicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, replicatedAt);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(unreplicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(unreplicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, replicationStatus);

        verify(unreplicatedNode, times(1)).addMixin(ReplicationStatus.NODE_TYPE);

        verify(session, times(1)).save();
    }

    @Test
    public void testSetReplicationStatus_Deactivate_1() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        final String replicationStatus = "Deactivate";
        final String replicatedBy = "Test User";
        final Calendar replicatedAt = Calendar.getInstance();
        replicatedAt.set(1,1);

        replicationStatusManager.setReplicationStatus(resourceResolver,
                replicatedBy,
                replicatedAt,
                ReplicationStatusManager.Status.DEACTIVATED,
                REPLICATED_PATH);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, replicatedAt);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, replicatedBy);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, replicationStatus);

        verify(replicatedNode, times(0)).addMixin(ReplicationStatus.NODE_TYPE);

        verify(session, times(1)).save();
    }

    @Test
    public void testSetReplicationStatus2() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        replicationStatusManager.setReplicationStatus(resourceResolver,
                null,
                null,
                ReplicationStatusManager.Status.CLEAR,
                REPLICATED_PATH);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, null);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, null);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, null);

        verify(replicatedNode, times(0)).addMixin(ReplicationStatus.NODE_TYPE);

        verify(session, times(1)).save();
    }

    @Test
    public void testClearReplicationStatus() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        replicationStatusManager.clearReplicationStatus(resourceResolver,
                replicatedResource);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED, null);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY, null);

        PowerMockito.verifyStatic(times(1));
        JcrUtil.setProperty(replicatedNode, ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION, null);

        verify(replicatedNode, times(0)).addMixin(ReplicationStatus.NODE_TYPE);

        verify(session, times(1)).save();
    }
}
