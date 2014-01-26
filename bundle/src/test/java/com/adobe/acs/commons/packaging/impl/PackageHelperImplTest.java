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

import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.JcrPackageManager;
import com.day.jcr.vault.packaging.PackageId;
import com.day.jcr.vault.packaging.Version;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.Session;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageHelperImplTest {
    final PackageHelperImpl packageHelper = new PackageHelperImpl();

    @Mock
    JcrPackageManager jcrPackageManager;

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
    PackageId packageOneID;

    @Mock
    PackageId packageTwoID;

    final String packageGroup = "testGroup";
    final String packageName = "testPackageName";
    final String packageOneVersion = "1.0.0";
    final String packageTwoVersion = "2.0.0";


    @Before
    public void setUp() throws Exception {
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

        when(packageOne.getNode()).thenReturn(packageOneNode);
        when(packageTwo.getNode()).thenReturn(packageTwoNode);

        when(packageOneID.getName()).thenReturn(packageName);
        when(packageTwoID.getName()).thenReturn(packageName);

        when(packageOneID.getVersion()).thenReturn(Version.create(packageOneVersion));
        when(packageTwoID.getVersion()).thenReturn(Version.create(packageTwoVersion));
    }

    @After
    public void tearDown() throws Exception {
        reset(jcrPackageManager, packageRoot, packageGroupRoot, packageOneNode, packageTwoNode,
                packageOne, packageTwo, packageOneDef, packageTwoDef, packageOneID, packageTwoID);
    }

    @Test
    public void testAddThumbnail() throws Exception {

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

    }

    @Test
    public void testGetSuccessJSON() throws Exception {
    }

    @Test
    public void testGetPreviewJSON() throws Exception {

    }

    @Test
    public void testGetErrorJSON() throws Exception {

    }
}
