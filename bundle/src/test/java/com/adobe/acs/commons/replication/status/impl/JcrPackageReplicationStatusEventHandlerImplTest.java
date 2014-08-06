package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.PackageId;
import com.day.jcr.vault.packaging.Packaging;
import org.apache.jackrabbit.JcrConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JcrPackageReplicationStatusEventHandlerImplTest {

    @Mock
    ReplicationStatusManager replicationStatusManager;

    @Mock
    PackageHelper packageHelper;

    @Mock
    Packaging packaging;

    @Mock
    ResourceResolver adminResourceResolver;

    @InjectMocks
    JcrPackageReplicationStatusEventHandlerImpl jcrPackageReplicationStatusEventHandler = new
            JcrPackageReplicationStatusEventHandlerImpl();

    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testProcess() throws Exception {
        final Calendar calendar = Calendar.getInstance();

        final List<String> contentPaths = new ArrayList<String>();
        contentPaths.add("/content/foo");
        contentPaths.add("/content/bar");

        final String packagePath = "/etc/packages/acs-commons/test.zip";
        final Resource packageResource = mock(Resource.class);
        final Node packageNode = mock(Node.class);
        final JcrPackage jcrPackage = mock(JcrPackage.class);
        final Node jcrPackageNode = mock(Node.class);
        final JcrPackageDefinition jcrPackageDefinition = mock(JcrPackageDefinition.class);
        final Resource jcrPackageJcrContent = mock(Resource.class);

        final Resource contentResource1 = mock(Resource.class);
        final Resource contentResource2 = mock(Resource.class);
        final Node contentNode1 = mock(Node.class);
        final Node contentNode2 = mock(Node.class);

        final String[] paths = new String[] { packagePath };

        final Map<String, Object> map = new HashMap<String, Object>();
        map.put("paths", paths);

        final Event event = new Event("MOCK", map);

        when(adminResourceResolver.getResource(packagePath)).thenReturn(packageResource);
        when(packageResource.adaptTo(Node.class)).thenReturn(packageNode);
        when(packaging.open(packageNode, false)).thenReturn(jcrPackage);
        when(packageHelper.getContents(jcrPackage)).thenReturn(contentPaths);
        when(jcrPackage.getDefinition()).thenReturn(jcrPackageDefinition);
        when(jcrPackageDefinition.getId()).thenReturn(mock(PackageId.class));
        when(jcrPackage.getNode()).thenReturn(jcrPackageNode);
        when(jcrPackageNode.getPath()).thenReturn(packagePath);
        when(packageResource.getChild("jcr:content")).thenReturn(jcrPackageJcrContent);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(JcrConstants.JCR_LASTMODIFIED, calendar);
        when(jcrPackageJcrContent.adaptTo(ValueMap.class)).thenReturn(new ValueMapDecorator(properties));

        when(adminResourceResolver.getResource("/content/foo")).thenReturn(contentResource1);
        when(contentResource1.adaptTo(Node.class)).thenReturn(contentNode1);
        when(contentNode1.isNodeType("cq:PageContent")).thenReturn(true);

        when(adminResourceResolver.getResource("/content/bar")).thenReturn(contentResource2);
        when(contentResource2.adaptTo(Node.class)).thenReturn(contentNode2);
        when(contentNode2.isNodeType("dam:AssetContent")).thenReturn(true);

        jcrPackageReplicationStatusEventHandler.process(event);

        verify(replicationStatusManager, times(1)).setReplicationStatus(
                eq(adminResourceResolver),
                eq("Package Replication"),
                eq(calendar),
                eq(ReplicationStatusManager.Status.ACTIVATED),
                eq(contentResource1), eq(contentResource2));
    }
}