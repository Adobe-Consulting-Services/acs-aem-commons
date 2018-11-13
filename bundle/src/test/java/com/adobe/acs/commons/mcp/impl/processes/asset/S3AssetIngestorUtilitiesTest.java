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

import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Test of utility methods in S3AssetIngestor. Separated out from S3AssetIngestorTest for minimal bootstrapping
 */
public class S3AssetIngestorUtilitiesTest {

    private S3AssetIngestor ingestor;

    @Before
    public void setup() {
        ingestor = new S3AssetIngestor(null);
        ingestor.bucket = "testbucket";
        ingestor.jcrBasePath = "/content/dam";
    }

    @Test
    public void testHierarchialElementForFolderNoBasePath() {
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/folder2/"));
        assertEquals("testbucket:folder1/folder2/", el.getItemName());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("/content/dam/folder1/folder2", el.getNodePath());
        assertEquals("folder2", el.getName());

        HierarchicalElement parent = el.getParent();
        assertEquals("testbucket:folder1/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder1", parent.getName());

        assertNull(parent.getParent());
    }

    @Test
    public void testHierarchialElementForFolderWithBasePath() {
        ingestor.s3BasePath = "folder1/";
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/folder2/folder3/"));
        assertEquals("testbucket:folder1/folder2/folder3/", el.getItemName());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("/content/dam/folder2/folder3", el.getNodePath());
        assertEquals("folder3", el.getName());

        HierarchicalElement parent = el.getParent();
        assertEquals("testbucket:folder1/folder2/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("/content/dam/folder2", parent.getNodePath());
        assertEquals("folder2", parent.getName());

        assertNull(parent.getParent());
    }

    @Test
    public void testHierarchialElementForFileNoBasePath() {
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/folder2/image.png"));
        assertEquals("testbucket:folder1/folder2/image.png", el.getItemName());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());

        HierarchicalElement parent = el.getParent();
        assertEquals("testbucket:folder1/folder2/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder2", parent.getName());

        parent = parent.getParent();
        assertEquals("testbucket:folder1/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder1", parent.getName());

        assertNull(parent.getParent());
    }

    @Test
    public void testHierarchialElementForFileWithBasePath() {
        ingestor.s3BasePath = "folder1/";
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/folder2/folder3/image.png"));
        assertEquals("testbucket:folder1/folder2/folder3/image.png", el.getItemName());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());
        assertEquals("/content/dam/folder2/folder3/image.png", el.getNodePath());

        HierarchicalElement parent = el.getParent();
        assertEquals("testbucket:folder1/folder2/folder3/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder3", parent.getName());
        assertEquals("/content/dam/folder2/folder3", parent.getNodePath());

        parent = parent.getParent();
        assertEquals("testbucket:folder1/folder2/", parent.getItemName());
        assertNotNull(parent);
        assertTrue(parent.isFolder());
        assertFalse(parent.isFile());
        assertEquals("folder2", parent.getName());
        assertEquals("/content/dam/folder2", parent.getNodePath());

        assertNull(parent.getParent());
    }

    @Test
    public void testHierarchialElementForFileInRootNoBasePath() {
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("image.png"));
        assertEquals("testbucket:image.png", el.getItemName());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());

        assertNull(el.getParent());
    }

    @Test
    public void testHierarchialElementForFolderInRootNoBasePath() {
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/"));
        assertEquals("testbucket:folder1/", el.getItemName());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("folder1", el.getName());

        assertNull(el.getParent());
    }

    @Test
    public void testHierarchialElementForFileInRootWithBasePath() {
        ingestor.s3BasePath = "folder1/";
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/image.png"));
        assertEquals("testbucket:folder1/image.png", el.getItemName());
        assertFalse(el.isFolder());
        assertTrue(el.isFile());
        assertEquals("image.png", el.getName());

        assertNull(el.getParent());
    }

    @Test
    public void testHierarchialElementForFolderInRootWithBasePath() {
        ingestor.s3BasePath = "folder1/";
        HierarchicalElement el = ingestor.new S3HierarchicalElement(forKey("folder1/folder2/"));
        assertEquals("testbucket:folder1/folder2/", el.getItemName());
        assertTrue(el.isFolder());
        assertFalse(el.isFile());
        assertEquals("folder2", el.getName());

        assertNull(el.getParent());
    }


    private S3ObjectSummary forKey(String key) {
        S3ObjectSummary s = new S3ObjectSummary();
        s.setKey(key);
        return s;
    }
}
