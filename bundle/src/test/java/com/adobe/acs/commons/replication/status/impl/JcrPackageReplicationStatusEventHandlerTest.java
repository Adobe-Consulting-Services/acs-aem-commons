package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.cq.replication.ReplicationAction;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationEvent;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.osgi.service.event.Event;

import javax.jcr.Node;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JcrPackageReplicationStatusEventHandlerTest {
    final String PACKAGE_PATH = "/etc/packages/acs-commons/test.zip";

    Calendar calendar;

    @Mock
    ReplicationStatusManager replicationStatusManager;

    @Mock
    PackageHelper packageHelper;

    @Mock
    Packaging packaging;

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    ResourceResolver resourceResolver;

    @Mock
    JobManager jobManager;

    @Mock
    Job job;

    @Mock
    Resource contentResource1;

    @Mock
    Resource contentResource2;

    @Mock
    Resource contentResource3;

    @InjectMocks
    JcrPackageReplicationStatusEventHandler eventHandler= new JcrPackageReplicationStatusEventHandler();

    @Before
    public void setUp() throws Exception {
       calendar = Calendar.getInstance();

        final List<String> contentPaths = new ArrayList<String>();
        contentPaths.add("/content/foo/jcr:content");
        contentPaths.add("/content/bar");
        contentPaths.add("/content/dam/folder/jcr:content");

        final Resource packageResource = mock(Resource.class);
        final Node packageNode = mock(Node.class);
        final JcrPackage jcrPackage = mock(JcrPackage.class);
        final VaultPackage vaultPackage = mock(VaultPackage.class);
        final Node jcrPackageNode = mock(Node.class);
        final JcrPackageDefinition jcrPackageDefinition = mock(JcrPackageDefinition.class);
        final Resource jcrPackageJcrContent = mock(Resource.class);

        final Resource contentResource1parent = mock(Resource.class);
        final Resource contentResource3parent = mock(Resource.class);

        final Node contentNode1 = mock(Node.class);
        final Node contentNode1parent = mock(Node.class);
        final Node contentNode2 = mock(Node.class);
        final Node contentNode3 = mock(Node.class);
        final Node contentNode3parent = mock(Node.class);

        final String[] paths = new String[] {PACKAGE_PATH};

        when(job.getProperty("paths")).thenReturn(paths);

        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource(PACKAGE_PATH)).thenReturn(packageResource);
        when(packageResource.adaptTo(Node.class)).thenReturn(packageNode);
        when(packaging.open(packageNode, false)).thenReturn(jcrPackage);
        when(packageHelper.getContents(jcrPackage)).thenReturn(contentPaths);
        when(jcrPackage.getDefinition()).thenReturn(jcrPackageDefinition);
        when(jcrPackageDefinition.getId()).thenReturn(mock(PackageId.class));
        when(jcrPackage.getNode()).thenReturn(jcrPackageNode);
        when(jcrPackageNode.getPath()).thenReturn(PACKAGE_PATH);
        when(packageResource.getChild("jcr:content")).thenReturn(jcrPackageJcrContent);

        when(jcrPackage.getPackage()).thenReturn(vaultPackage);
        when(vaultPackage.getCreated()).thenReturn(calendar);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JcrConstants.JCR_LASTMODIFIED, calendar);
        when(jcrPackageJcrContent.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(properties));

        when(resourceResolver.getResource("/content/foo/jcr:content")).thenReturn(contentResource1);
        when(contentResource1.adaptTo(Node.class)).thenReturn(contentNode1);
        when(contentNode1.isNodeType("cq:PageContent")).thenReturn(true);
        when(contentResource1.getParent()).thenReturn(contentResource1parent);
        when(contentResource1parent.adaptTo(Node.class)).thenReturn(contentNode1parent);
        when(contentNode1parent.isNodeType("cq:Page")).thenReturn(true);

        when(resourceResolver.getResource("/content/bar")).thenReturn(contentResource2);
        when(contentResource2.adaptTo(Node.class)).thenReturn(contentNode2);
        when(contentNode2.isNodeType("dam:AssetContent")).thenReturn(true);

        when(resourceResolver.getResource("/content/dam/folder/jcr:content")).thenReturn(contentResource3);
        when(contentResource3.adaptTo(Node.class)).thenReturn(contentNode3);
        when(contentNode3.isNodeType("nt:unstructured")).thenReturn(true);

        when(contentResource3.getParent()).thenReturn(contentResource3parent);
        when(contentResource3parent.adaptTo(Node.class)).thenReturn(contentNode3parent);
        when(contentNode3parent.isNodeType("sling:OrderedFolder")).thenReturn(true);
    }

    @Test
    public void testProcess() throws Exception {
        final Map<String, String> config = new HashMap<>();

        config.put("replicated-by.override", "Package Replication");

        eventHandler.activate(config);
        eventHandler.process(job);

        verify(replicationStatusManager, times(1)).setReplicationStatus(
                eq(resourceResolver),
                eq("Package Replication"),
                eq(calendar),
                eq(ReplicationStatusManager.Status.ACTIVATED),
                eq(contentResource1), eq(contentResource2), eq(contentResource3));
    }

    @Test
    public void testHandleEvent() throws LoginException {
        final Map<String, Object> eventParams  = new HashMap<>();
        eventParams.put("paths", new String[]{PACKAGE_PATH});
        eventParams.put("userId", "replication-user");

        final Event event = new Event(ReplicationAction.EVENT_TOPIC, eventParams);

        final ArgumentCaptor<Map> captor = ArgumentCaptor.forClass(Map.class);

        eventHandler.bindRepository(null, null, true);
        eventHandler.handleEvent(event);

        verify(jobManager, times(1)).addJob(eq("acs-commons/replication/package"), captor.capture());
        final Map<String, Object> actual = captor.getValue();
        assertEquals("replication-user", (String) actual.get("replicatedBy"));
    }

    @Test
    public void testGetInfoFromEvent_CQEvent() {
        final String[] expectedPaths = new String[]{PACKAGE_PATH};
        final String expectedUserId = "replication-user";

        final Map<String, Object> properties  = new HashMap<>();
        properties.put("paths", expectedPaths);
        properties.put("userId", expectedUserId);

        final Event event = new Event(ReplicationAction.EVENT_TOPIC, properties);

        final Map<String, Object> actual = eventHandler.getInfoFromEvent(event);
        assertEquals(expectedPaths, actual.get("paths"));
        assertEquals(expectedUserId, actual.get("replicatedBy"));
    }

    @Test
    public void testGetInfoFromEvent_GraniteEvent() {
        final String[] expectedPaths = new String[]{PACKAGE_PATH};
        final String expectedUserId = "replication-user";

        final Map<String, Object> modification  = new HashMap<>();
        modification.put("paths", expectedPaths);
        modification.put("userId", expectedUserId);
        modification.put("type", ReplicationActionType.ACTIVATE);
        modification.put("time", 0l);
        modification.put("revision", "1");

        final List<Map<String, Object>> modifications = new ArrayList<>();
        modifications.add(modification);

        final Map<String, Object> properties  = new HashMap<>();
        properties.put("modifications", modifications);

        final Event event = new Event(ReplicationEvent.EVENT_TOPIC, properties);

        final Map<String, Object> actual = eventHandler.getInfoFromEvent(event);
        assertEquals(expectedPaths, actual.get("paths"));
        assertEquals(expectedUserId, actual.get("replicatedBy"));
    }
}