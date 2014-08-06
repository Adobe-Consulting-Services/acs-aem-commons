package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.commons.jcr.JcrUtil;
import com.day.cq.replication.ReplicationStatus;
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

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrUtil.class)
public class ReplicationStatusManagerImplTest {

    @Mock
    NodeType replicationNodeType;

    @Spy
    ReplicationStatusManagerImpl replicationStatusManager = new ReplicationStatusManagerImpl();

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    Session session;

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