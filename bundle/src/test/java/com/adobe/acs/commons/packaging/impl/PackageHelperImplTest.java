/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.packaging.impl;

import com.adobe.acs.commons.packaging.PackageHelper;
import org.apache.commons.lang.ArrayUtils;
import org.apache.jackrabbit.commons.iterator.NodeIteratorAdapter;
import org.apache.jackrabbit.commons.iterator.PropertyIteratorAdapter;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.api.WorkspaceFilter;
import org.apache.jackrabbit.vault.fs.config.MetaInf;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.Version;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;

@RunWith(MockitoJUnitRunner.class)
public class PackageHelperImplTest {
    @Mock
    Packaging packaging;

    @Mock
    Session session;

    @Mock
    JcrPackageManager jcrPackageManager;

    @Mock
    ResourceResolverFactory resourceResolverFactory;

    @Mock
    Node packageRoot;

    @Mock
    Node packageGroupRoot;

    @Mock
    NodeIterator packageGroupNodeIterator;

    @Mock
    Node packageOneNode;

    @Mock
    Node packageTwoNode;

    @Mock
    JcrPackage packageOne;

    @Mock
    JcrPackage packageTwo;

    @Mock
    JcrPackageDefinition packageOneDef;

    @Mock
    JcrPackageDefinition packageTwoDef;

    @Mock
    Node packageOneDefNode;

    @Mock
    Node packageTwoDefNode;

    @Mock
    MetaInf packageOneMetaInf;

    @Mock
    MetaInf packageTwoMetaInf;

    @Mock
    WorkspaceFilter packageOneFilter;

    @Mock
    WorkspaceFilter packageTwoFilter;

    @Mock
    PackageId packageOneId;

    @Mock
    PackageId packageTwoId;

    @InjectMocks
    final PackageHelperImpl packageHelper = new PackageHelperImpl();

    @Rule
    public SlingContext slingContext = new SlingContext();

    List<PathFilterSet> packageOneFilterSets;
    List<PathFilterSet> packageTwoFilterSets;

    final String packageGroup = "testGroup";
    final String packageName = "testPackageName";
    final String packageOneVersion = "1.0.0";
    final String packageTwoVersion = "2.0.0";

    @Before
    public void setUp() throws Exception {

        packageOneFilterSets = new ArrayList<PathFilterSet>();
        packageOneFilterSets.add(new PathFilterSet("/a/b/c"));
        packageOneFilterSets.add(new PathFilterSet("/d/e/f"));
        packageOneFilterSets.add(new PathFilterSet("/g/h/i"));

        packageTwoFilterSets = new ArrayList<PathFilterSet>();

        when(packaging.getPackageManager(any(Session.class))).thenReturn(jcrPackageManager);

        when(jcrPackageManager.getPackageRoot(false)).thenReturn(packageRoot);

        when(packageRoot.hasNode(packageGroup)).thenReturn(true);

        when(packageRoot.getNode(packageGroup)).thenReturn(packageGroupRoot);

        when(packageGroupRoot.getNodes()).thenReturn(packageGroupNodeIterator);

        when(packageGroupNodeIterator.hasNext()).thenReturn(true, true, false);
        when(packageGroupNodeIterator.nextNode()).thenReturn(packageOneNode, packageTwoNode, null);

        when(jcrPackageManager.open(packageOneNode, true)).thenReturn(packageOne);
        when(jcrPackageManager.open(packageTwoNode, true)).thenReturn(packageTwo);

        when(packageOne.getDefinition()).thenReturn(packageOneDef);
        when(packageTwo.getDefinition()).thenReturn(packageTwoDef);

        when(packageOneDef.getId()).thenReturn(packageOneId);
        when(packageTwoDef.getId()).thenReturn(packageTwoId);

        when(packageOneDef.getNode()).thenReturn(packageOneDefNode);
        when(packageTwoDef.getNode()).thenReturn(packageTwoDefNode);

        when(packageOneDef.getMetaInf()).thenReturn(packageOneMetaInf);
        when(packageTwoDef.getMetaInf()).thenReturn(packageTwoMetaInf);

        when(packageOneMetaInf.getFilter()).thenReturn(packageOneFilter);
        when(packageTwoMetaInf.getFilter()).thenReturn(packageTwoFilter);

        when(packageOneFilter.getFilterSets()).thenReturn(packageOneFilterSets);
        when(packageTwoFilter.getFilterSets()).thenReturn(packageTwoFilterSets);

        when(packageOne.getNode()).thenReturn(packageOneNode);
        when(packageTwo.getNode()).thenReturn(packageTwoNode);

        when(packageOneNode.getPath()).thenReturn("/etc/packages/" + packageGroup + "/" + packageName + "-"
                + packageOneVersion + ".zip");
        when(packageTwoNode.getPath()).thenReturn("/etc/packages/" + packageGroup + "/" + packageName + "-"
                + packageTwoVersion + ".zip");

        when(packageOneId.getName()).thenReturn(packageName);
        when(packageTwoId.getName()).thenReturn(packageName);

        when(packageOneId.getVersion()).thenReturn(Version.create(packageOneVersion));
        when(packageTwoId.getVersion()).thenReturn(Version.create(packageTwoVersion));
    }


    @After
    public void tearDown() throws Exception {
        reset(packaging,
                session,
                resourceResolverFactory,
                jcrPackageManager,
                packageRoot,
                packageGroupRoot,
                packageOneNode,
                packageTwoNode,
                packageOne,
                packageTwo,
                packageOneDef,
                packageTwoDef,
                packageOneDefNode,
                packageTwoDefNode,
                packageOneMetaInf,
                packageTwoMetaInf,
                packageOneFilter,
                packageTwoFilter,
                packageOneId,
                packageTwoId);
    }

    @Test
    public void testAddThumbnail() throws Exception {
        Resource thumbnailResource = mock(Resource.class);
        Node thumbnailNode = mock(Node.class);
        NodeType ntFile = mock(NodeType.class);

        when(thumbnailResource.adaptTo(Node.class)).thenReturn(thumbnailNode);
        when(thumbnailNode.isNodeType("nt:file")).thenReturn(true);
        when(thumbnailNode.getPrimaryNodeType()).thenReturn(ntFile);
        when(ntFile.getName()).thenReturn("nt:file");
        when(thumbnailNode.getMixinNodeTypes()).thenReturn(new NodeType[0]);
        when(thumbnailNode.getProperties()).thenReturn(new PropertyIteratorAdapter(Collections.emptyIterator()));
        when(thumbnailNode.getNodes()).thenReturn(new NodeIteratorAdapter(Collections.emptyIterator()));

        when(packageOneDefNode.getSession()).thenReturn(mock(Session.class));

        packageHelper.addThumbnail(packageOne, thumbnailResource);

        // Verification
        verify(packageOneDefNode, times(1)).addNode(eq("thumbnail.png"), eq("nt:file"));
    }

    @Test
    public void testGetNextVersion_incrementMinor() throws Exception {
        final Version expected = Version.create("2.1.0");
        final Version actual = packageHelper.getNextVersion(jcrPackageManager,
                packageGroup, packageName, "1.0.0");

        assertEquals(expected.toString(), actual.toString());
    }

    @Test
    public void testGetNextVersion_paramVersion() throws Exception {
        final Version expected = Version.create("3.0.0");
        final Version actual = packageHelper.getNextVersion(jcrPackageManager,
                packageGroup, packageName, "3.0.0");

        assertEquals(expected.toString(), actual.toString());
    }


    @Test
    public void testRemovePackage() throws Exception {
        final PackageId packageId = new PackageId(packageGroup, packageName, packageOneVersion);

        when(jcrPackageManager.open(packageId)).thenReturn(packageOne);
        when(packageOneNode.getSession()).thenReturn(mock(Session.class));

        packageHelper.removePackage(jcrPackageManager, packageGroup, packageName, packageOneVersion);

        verify(packageOneNode, times(1)).remove();
    }

    @Test
         public void testCreatePackage() throws Exception {

        Map<String, String> properties = new HashMap<String, String>();
        Set<Resource> resources = new HashSet<Resource>();

        when(jcrPackageManager.create(packageGroup, packageName, packageOneVersion)).thenReturn(packageOne);

        packageHelper.createPackage(resources, session, packageGroup, packageName, packageOneVersion,
                PackageHelper.ConflictResolution.None, properties);

        // Verify the session was saved, creating the package
        verify(session, times(1)).save();
    }


    @Test
    public void testCreatePackageFromPathFilterSets() throws Exception {
        final Map<String, String> properties = new HashMap<String, String>();

        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();
        pathFilterSets.add(new PathFilterSet("/a/b/c"));
        pathFilterSets.add(new PathFilterSet("/d/e/f"));
        pathFilterSets.add(new PathFilterSet("/g/h/i"));

        when(jcrPackageManager.create(packageGroup, packageName, packageOneVersion)).thenReturn(packageOne);

        packageHelper.createPackageFromPathFilterSets(pathFilterSets, session, packageGroup, packageName, packageOneVersion,
                PackageHelper.ConflictResolution.None, properties);

        // Verify the session was saved, creating the package
        verify(session, times(1)).save();
    }


    @Test
    public void testGetSuccessJSON() throws Exception {

        final String actual = packageHelper.getSuccessJSON(packageOne);

        final JSONObject json = new JSONObject(actual);

        assertEquals("success", json.getString("status"));
        assertEquals("/etc/packages/testGroup/testPackageName-1.0.0.zip", json.getString("path"));

        final String[] expectedFilterSets = new String[]{
                "/a/b/c",
                "/d/e/f",
                "/g/h/i"
        };

        JSONArray actualArray = json.getJSONArray("filterSets");
        for(int i = 0; i < actualArray.length(); i++) {
            JSONObject tmp = actualArray.getJSONObject(i);
            assertTrue(ArrayUtils.contains(expectedFilterSets, tmp.get("rootPath")));
        }

        assertEquals(expectedFilterSets.length, actualArray.length());
    }

    @Test
    public void testGetPreviewJSON() throws Exception {
        final Set<Resource> resources = new HashSet<Resource>();

        resources.add(slingContext.create().resource("/a/b/c"));
        resources.add(slingContext.create().resource("/d/e/f"));
        resources.add(slingContext.create().resource("/g/h/i"));


        final String actual = packageHelper.getPreviewJSON(resources);
        final JSONObject json = new JSONObject(actual);

        assertEquals("preview", json.getString("status"));
        assertEquals("Not applicable (Preview)", json.getString("path"));

        final String[] expectedFilterSets = new String[]{
                "/a/b/c",
                "/d/e/f",
                "/g/h/i"
        };

        JSONArray actualArray = json.getJSONArray("filterSets");
        for(int i = 0; i < actualArray.length(); i++) {
            JSONObject tmp = actualArray.getJSONObject(i);
            assertTrue(ArrayUtils.contains(expectedFilterSets, tmp.get("rootPath")));
        }

        assertEquals(expectedFilterSets.length, actualArray.length());
    }



    @Test
    public void testGetPathFilterSetPreviewJSON() throws Exception {
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();
        pathFilterSets.add(new PathFilterSet("/a/b/c"));
        pathFilterSets.add(new PathFilterSet("/d/e/f"));
        pathFilterSets.add(new PathFilterSet("/g/h/i"));

        final String actual = packageHelper.getPathFilterSetPreviewJSON(pathFilterSets);
        final JSONObject json = new JSONObject(actual);

        assertEquals("preview", json.getString("status"));
        assertEquals("Not applicable (Preview)", json.getString("path"));

        final String[] expectedFilterSets = new String[]{
                "/a/b/c",
                "/d/e/f",
                "/g/h/i"
        };

        JSONArray actualArray = json.getJSONArray("filterSets");
        for(int i = 0; i < actualArray.length(); i++) {
            JSONObject tmp = actualArray.getJSONObject(i);
            assertTrue(ArrayUtils.contains(expectedFilterSets, tmp.get("rootPath")));
        }

        assertEquals(expectedFilterSets.length, actualArray.length());
    }

    @Test
    public void testGetErrorJSON() throws Exception {
        final String expectedMessage = "This is a test error message!";
        final String actual = packageHelper.getErrorJSON(expectedMessage);

        final JSONObject json = new JSONObject(actual);

        assertEquals("error", json.getString("status"));
        assertEquals(expectedMessage, json.getString("msg"));
    }
}
