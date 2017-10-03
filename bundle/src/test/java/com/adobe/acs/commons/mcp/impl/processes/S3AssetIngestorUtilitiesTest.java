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
package com.adobe.acs.commons.mcp.impl.processes;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.*;

/**
 * Test of utility methods in S3AssetIngestor. Separated out from S3AssetIngestorTest for minimal bootstrapping
 */
public class S3AssetIngestorUtilitiesTest {

    @Test
    public void testGetNameOnFolder() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        assertEquals("folder1", ingestor.getName(forKey("folder1/")));
        assertEquals("folder2", ingestor.getName(forKey("folder1/folder2/")));
        assertEquals("folder1", ingestor.getName(forKey("folder1")));
        assertEquals("folder2", ingestor.getName(forKey("folder1/folder2")));
    }

    @Test
    public void testGetNameOnFile() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        assertEquals("file1.jpg", ingestor.getName(forKey("file1.jpg")));
        assertEquals("file2.jpg", ingestor.getName(forKey("folder1/folder2/file2.jpg")));
    }

    @Test
    public void testKeyToNodePathWithNullBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.jcrBasePath = "/content/dam";
        assertEquals("/content/dam/folder1", ingestor.keyToNodePath("folder1/", false));
        assertEquals("/content/dam/folder1", ingestor.keyToNodePath("folder1", false));
        assertEquals("/content/dam/folder1/folder2/folder3", ingestor.keyToNodePath("folder1/folder2/folder3/", false));
        assertEquals("/content/dam/folder1/folder2/folder_3", ingestor.keyToNodePath("folder1/folder2/folder 3/", false));
    }

    @Test
    public void testKeyToNodePathWithBlankBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.jcrBasePath = "/content/dam";
        ingestor.s3BasePath = "";
        assertEquals("/content/dam/first.jpg", ingestor.keyToNodePath("first.jpg", true));
        assertEquals("/content/dam/folder1", ingestor.keyToNodePath("folder1/", false));
        assertEquals("/content/dam/folder1", ingestor.keyToNodePath("folder1", false));
        assertEquals("/content/dam/folder1/folder2/folder3", ingestor.keyToNodePath("folder1/folder2/folder3/", false));
        assertEquals("/content/dam/folder1/folder2/folder_3", ingestor.keyToNodePath("folder1/folder2/folder 3/", false));
    }

    @Test
    public void testKeyToNodePathWithBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.jcrBasePath = "/content/dam";
        ingestor.s3BasePath = "folder1/";
        assertEquals("/content/dam/first.jpg", ingestor.keyToNodePath("folder1/first.jpg", true));
        assertEquals("/content/dam", ingestor.keyToNodePath("folder1/", false));
        assertEquals("/content/dam", ingestor.keyToNodePath("folder1", false));
        assertEquals("/content/dam/folder2/folder3", ingestor.keyToNodePath("folder1/folder2/folder3/", false));
        assertEquals("/content/dam/folder2/folder_3", ingestor.keyToNodePath("folder1/folder2/folder 3/", false));
        assertEquals("/content/dam/folder2/folder_3/foo.jpg", ingestor.keyToNodePath("folder1/folder2/folder 3/foo.jpg", true));
    }

    @Test
    public void testCanImportFolderWithNullBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.ignoreFolderList = Arrays.asList(".ds_store");
        assertTrue(ingestor.canImportFolder(forKey("folder1/")));
        assertTrue(ingestor.canImportFolder(forKey("folder1/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey(".ds_store/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey("folder1/.ds_store/")));
    }

    @Test
    public void testCanImportFolderWithBlankBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.ignoreFolderList = Arrays.asList(".ds_store");
        ingestor.s3BasePath = "";
        assertTrue(ingestor.canImportFolder(forKey("folder1/")));
        assertTrue(ingestor.canImportFolder(forKey("folder1/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey(".ds_store/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey("folder1/.ds_store/")));
    }

    @Test
    public void testCanImportFolderWithBasePath() {
        S3AssetIngestor ingestor = new S3AssetIngestor(null);
        ingestor.ignoreFolderList = Arrays.asList(".ds_store");
        ingestor.s3BasePath = "";
        ingestor.s3BasePath = "folder1/";
        assertTrue(ingestor.canImportFolder(forKey("folder1/")));
        assertTrue(ingestor.canImportFolder(forKey("folder1/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey("folder1/.ds_store/folder2/")));
        assertFalse(ingestor.canImportFolder(forKey("folder1/.ds_store/")));
    }

    private S3ObjectSummary forKey(String key) {
        S3ObjectSummary s = new S3ObjectSummary();
        s.setKey(key);
        return s;
    }
}
