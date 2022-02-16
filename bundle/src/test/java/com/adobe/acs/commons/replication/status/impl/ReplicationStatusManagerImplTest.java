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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.function.Function;

import javax.annotation.Nullable;
import javax.jcr.Node;
import javax.jcr.Session;

import org.apache.jackrabbit.commons.cnd.CndImporter;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.dam.api.Asset;
import com.day.cq.replication.ReplicationStatus;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@RunWith(MockitoJUnitRunner.class)
public class ReplicationStatusManagerImplTest {

    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);

    @Spy
    ReplicationStatusManagerImpl replicationStatusManager = new ReplicationStatusManagerImpl();

    ResourceResolver resourceResolver;

    Session session;
    
    @Mock
    PageManager pageManager;

    static final String PAGE_PATH = "/content/page";

    @Mock
    Page pagePage;

    Resource pageContentResource;
    
    static final String ASSET_PATH = "/content/asset";

    @Mock
    Asset assetAsset;

    Resource assetContentResource;

    static final String UNREPLICATED_PATH = "/content/unreplicated";

    Resource unreplicatedResource;

    Node unreplicatedNode;

    static final String REPLICATED_PATH = "/content/replicated";

    Resource replicatedResource;

    Node replicatedNode;

    @Before
    public void setUp() throws Exception {
        resourceResolver = context.resourceResolver();
        session = resourceResolver.adaptTo(Session.class);

        InputStream cnd = getClass().getResourceAsStream("replication.cnd");
        CndImporter.registerNodeTypes(new InputStreamReader(cnd, "UTF-8"), session);

        context.registerAdapter(ResourceResolver.class, PageManager.class, pageManager);

        context.load().json(getClass().getResourceAsStream("ReplicationStatusManagerImplTest.json"), "/content");

        /* Page */
        pageContentResource = resourceResolver.getResource(PAGE_PATH + "/jcr:content");
        when(pageManager.getContainingPage(PAGE_PATH)).thenReturn(pagePage);
        when(pagePage.getContentResource()).thenReturn(pageContentResource);
        
        /* Asset */
        assetContentResource = resourceResolver.getResource(ASSET_PATH + "/jcr:content");
        context.registerAdapter(Resource.class, Asset.class, new Function<Resource, Asset>() {
            @Nullable
            @Override
            public Asset apply(@Nullable Resource input) {
                if (input.getPath().equals(ASSET_PATH)) {
                    return assetAsset;
                } else {
                    return null;
                }
            }
        });
        when(assetAsset.getPath()).thenReturn(ASSET_PATH);

        /* Unreplicated Node */
        unreplicatedResource = resourceResolver.getResource(UNREPLICATED_PATH);
        unreplicatedNode = unreplicatedResource.adaptTo(Node.class);

        /* Replicated Node */
        replicatedResource = resourceResolver.getResource(REPLICATED_PATH);
        replicatedNode = replicatedResource.adaptTo(Node.class);
    }
    
    @Test
    public void testGetReplicationStatusResource_Page() {
        assertSamePath(pageContentResource, replicationStatusManager.getReplicationStatusResource(PAGE_PATH, resourceResolver));
    }
    
    @Test
    public void testGetReplicationStatusResource_Asset() {
        assertSamePath(assetContentResource, replicationStatusManager.getReplicationStatusResource(ASSET_PATH, resourceResolver));
    }
    
    @Test
    public void testGetReplicationStatusResource_Resource() {
        assertSamePath(unreplicatedResource, replicationStatusManager.getReplicationStatusResource(UNREPLICATED_PATH, resourceResolver));
    }

    @Test
    public void testSetReplicationStatus_Activate() throws Exception {
        final String replicationStatus = "Activate";
        final String replicatedBy = "Test User";
        final Calendar replicatedAt = Calendar.getInstance();
        replicatedAt.set(1,1);

        replicationStatusManager.setReplicationStatus(resourceResolver,
                replicatedBy,
                replicatedAt,
                ReplicationStatusManager.Status.ACTIVATED,
                UNREPLICATED_PATH);

        assertTrue(unreplicatedNode.isNodeType(ReplicationStatus.NODE_TYPE));

        assertSameTime(replicatedAt, unreplicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED).getDate());
        assertEquals(replicatedBy, unreplicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY).getString());
        assertEquals(replicationStatus, unreplicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION).getString());
    }

    @Test
    public void testSetReplicationStatus_Deactivate_1() throws Exception {
        final String replicationStatus = "Deactivate";
        final String replicatedBy = "Test User";
        final Calendar replicatedAt = Calendar.getInstance();
        replicatedAt.set(1,1);

        replicationStatusManager.setReplicationStatus(resourceResolver,
                replicatedBy,
                replicatedAt,
                ReplicationStatusManager.Status.DEACTIVATED,
                REPLICATED_PATH);

        assertSameTime(replicatedAt, replicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED).getDate());
        assertEquals(replicatedBy, replicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY).getString());
        assertEquals(replicationStatus, replicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION).getString());
    }


    // Issue #1265
    @Test
    public void testSetReplicationStatus_NullReplicatedByAndReplicatedAt() throws Exception {
        final String replicatedBy = null;
        final Calendar replicatedAt = null;

        replicationStatusManager.setReplicationStatus(resourceResolver,
                replicatedBy,
                replicatedAt,
                ReplicationStatusManager.Status.ACTIVATED,
                REPLICATED_PATH);

        assertNotNull(replicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED).getDate());
        assertEquals(ReplicationStatusManagerImpl.DEFAULT_REPLICATED_BY, replicatedNode.getProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY).getString());
    }

    @Test
    public void testSetReplicationStatus2() throws Exception {
        replicationStatusManager.setReplicationStatus(resourceResolver,
                null,
                null,
                ReplicationStatusManager.Status.CLEAR,
                REPLICATED_PATH);

        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED));
        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY));
        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION));
    }

    @Test
    public void testClearReplicationStatus() throws Exception {
        replicationStatusManager.clearReplicationStatus(resourceResolver,
                replicatedResource);

        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED));
        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATED_BY));
        assertFalse(replicatedNode.hasProperty(ReplicationStatus.NODE_PROPERTY_LAST_REPLICATION_ACTION));
    }

    private static void assertSamePath(Resource expected, Resource actual) {
        assertEquals(expected.getPath(), actual.getPath());
    }

    private static void assertSameTime(Calendar expected, Calendar actual) {
        assertEquals(expected.getTimeInMillis(), actual.getTimeInMillis());
    }
}
