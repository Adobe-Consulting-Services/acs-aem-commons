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
package com.adobe.acs.commons.mcp.impl.processes.asset;

import com.google.common.io.Files;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Test of utility methods in FileAssetIngestor. Separated out from FileAssetIngestorTest for minimal bootstrapping
 */
public class FileAssetIngestorUtilitiesTest {

    private FileAssetIngestor ingestor;

    private File tempDirectory;

    @Before
    public void setup() throws Exception {
        ingestor = new FileAssetIngestor(null);
        ingestor.jcrBasePath = "/content/dam";
        tempDirectory = Files.createTempDir();
        ingestor.fileBasePath = tempDirectory.getAbsolutePath();
        ingestor.init();
    }

    @After
    public void teardown() throws Exception {
        FileUtils.deleteDirectory(tempDirectory);
    }

    @Test
    public void testHierarchialElementForFolder() {
        File folder1 = new File(tempDirectory, "folder1");
        folder1.mkdir();
        File folder2 = new File(folder1, "folder2");
        folder2.mkdir();

        HierarchicalElement el = ingestor.new FileHierarchicalElement(folder2);
        assertEquals(folder2.getAbsolutePath(), el.getSourcePath());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("/content/dam/folder1/folder2", el.getNodePath());
        assertEquals("folder2", el.getName());

        HierarchicalElement parent = el.getParent();
        assertEquals(folder1.getAbsolutePath(), parent.getSourcePath());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder1", parent.getName());

        assertNotNull(parent.getParent());
        assertNull(parent.getParent().getParent());
    }

    @Test
    public void testHierarchialElementForFile() throws Exception {
        File folder1 = new File(tempDirectory, "folder1");
        folder1.mkdir();
        File folder2 = new File(folder1, "folder2");
        folder2.mkdir();
        File image = new File(folder2, "image.png");
        FileUtils.writeByteArrayToFile(image, new byte[0]);

        HierarchicalElement el = ingestor.new FileHierarchicalElement(image);
        assertEquals(image.getAbsolutePath(), el.getSourcePath());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());

        HierarchicalElement parent = el.getParent();
        assertEquals(folder2.getAbsolutePath(), parent.getSourcePath());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder2", parent.getName());

        parent = parent.getParent();
        assertEquals(folder1.getAbsolutePath(), parent.getSourcePath());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder1", parent.getName());

        assertNotNull(parent.getParent());
        assertNull(parent.getParent().getParent());
    }

    @Test
    public void testHierarchialElementForFileInRoot() throws Exception {
        File image = new File(tempDirectory, "image.png");
        FileUtils.writeByteArrayToFile(image, new byte[0]);
        HierarchicalElement el = ingestor.new FileHierarchicalElement(image);
        assertEquals(image.getAbsolutePath(), el.getSourcePath());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());

        assertNotNull(el.getParent());
        assertNull(el.getParent().getParent());
    }

    @Test
    public void testHierarchialElementForFolderInRoot() {
        File folder1 = new File(tempDirectory, "folder1");
        folder1.mkdir();
        HierarchicalElement el = ingestor.new FileHierarchicalElement(folder1);
        assertEquals(folder1.getAbsolutePath(), el.getSourcePath());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("folder1", el.getName());

        assertNotNull(el.getParent());
        assertNull(el.getParent().getParent());
    }

}
