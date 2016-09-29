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
import com.day.cq.commons.jcr.JcrUtil;
import com.day.jcr.vault.fs.api.PathFilterSet;
import com.day.jcr.vault.fs.api.WorkspaceFilter;
import com.day.jcr.vault.fs.config.MetaInf;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.PackageId;
import com.day.jcr.vault.packaging.Packaging;
import com.day.jcr.vault.packaging.Version;
import org.apache.commons.lang.ArrayUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.testing.sling.MockResource;
import org.apache.sling.commons.testing.sling.MockResourceResolver;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest(JcrUtil.class)
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
    PackageId packageOneID;

    @Mock
    PackageId packageTwoID;

    @InjectMocks
    final PackageHelperImpl packageHelper = new PackageHelperImpl();

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

        when(packageOneDef.getId()).thenReturn(packageOneID);
        when(packageTwoDef.getId()).thenReturn(packageTwoID);

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

        when(packageOneNode.getPath()).thenReturn("/etc/packages/" + packageGroup + "/" + packageName + "-" +
                packageOneVersion + ".zip");
        when(packageTwoNode.getPath()).thenReturn("/etc/packages/" + packageGroup + "/" + packageName + "-" +
                packageTwoVersion + ".zip");

        when(packageOneID.getName()).thenReturn(packageName);
        when(packageTwoID.getName()).thenReturn(packageName);

        when(packageOneID.getVersion()).thenReturn(Version.create(packageOneVersion));
        when(packageTwoID.getVersion()).thenReturn(Version.create(packageTwoVersion));
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
                packageOneID,
                packageTwoID);
    }

    @Test
    public void testAddThumbnail() throws Exception {
        PowerMockito.mockStatic(JcrUtil.class);

        Resource thumbnailResource = mock(Resource.class);
        Node thumbnailNode = mock(Node.class);

        when(thumbnailResource.adaptTo(Node.class)).thenReturn(thumbnailNode);
        when(thumbnailNode.isNodeType("nt:file")).thenReturn(true);

        when(packageOneDefNode.getSession()).thenReturn(mock(Session.class));

        packageHelper.addThumbnail(packageOne, thumbnailResource);

        // Verification
        PowerMockito.verifyStatic(times(1));
        JcrUtil.copy(thumbnailNode, packageOneDefNode, "thumbnail.png");
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
        Map<String, String> properties = new HashMap<String, String>();

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
        final MockResourceResolver resourceResolver = new MockResourceResolver();
        final Set<Resource> resources = new HashSet<Resource>();

        resources.add(new MockResource(resourceResolver, "/a/b/c", ""));
        resources.add(new MockResource(resourceResolver, "/d/e/f", ""));
        resources.add(new MockResource(resourceResolver, "/g/h/i", ""));


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
