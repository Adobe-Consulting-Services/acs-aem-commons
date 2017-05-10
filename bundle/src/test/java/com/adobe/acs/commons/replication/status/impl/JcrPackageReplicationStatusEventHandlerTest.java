package com.adobe.acs.commons.replication.status.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import org.apache.jackrabbit.JcrConstants;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.event.jobs.Job;
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

import static org.mockito.Matchers.anyMap;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class JcrPackageReplicationStatusEventHandlerTest {

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

    @InjectMocks
    JcrPackageReplicationStatusEventHandler jcrPackageReplicationStatusEventHandler = new
            JcrPackageReplicationStatusEventHandler();

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
        contentPaths.add("/content/foo/jcr:content");
        contentPaths.add("/content/bar");
        contentPaths.add("/content/dam/folder/jcr:content");

        final String packagePath = "/etc/packages/acs-commons/test.zip";
        final Resource packageResource = mock(Resource.class);
        final Node packageNode = mock(Node.class);
        final JcrPackage jcrPackage = mock(JcrPackage.class);
        final Node jcrPackageNode = mock(Node.class);
        final JcrPackageDefinition jcrPackageDefinition = mock(JcrPackageDefinition.class);
        final Resource jcrPackageJcrContent = mock(Resource.class);

        final Resource contentResource1 = mock(Resource.class);
        final Resource contentResource1parent = mock(Resource.class);
        final Resource contentResource2 = mock(Resource.class);
        final Resource contentResource3 = mock(Resource.class);
        final Resource contentResource3parent = mock(Resource.class);

        final Node contentNode1 = mock(Node.class);
        final Node contentNode1parent = mock(Node.class);
        final Node contentNode2 = mock(Node.class);
        final Node contentNode3 = mock(Node.class);
        final Node contentNode3parent = mock(Node.class);

        final String[] paths = new String[] { packagePath };

        final Job job = mock(Job.class);
        when(job.getProperty("paths")).thenReturn(paths);

        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resourceResolver);
        when(resourceResolver.getResource(packagePath)).thenReturn(packageResource);
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

        jcrPackageReplicationStatusEventHandler.process(job);

        verify(replicationStatusManager, times(1)).setReplicationStatus(
                eq(resourceResolver),
                eq("Package Replication"),
                eq(calendar),
                eq(ReplicationStatusManager.Status.ACTIVATED),
                eq(contentResource1), eq(contentResource2), eq(contentResource3));
    }
}