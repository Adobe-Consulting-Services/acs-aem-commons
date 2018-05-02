/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import com.adobe.acs.commons.mcp.impl.processes.asset.AssetIngestor.Source;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.Collections;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 */
public class FileOrRenditionTest {
    
    public FileOrRenditionTest() {
    }
    
    Folder testFolder;
    
    @Before
    public void setUp() {
        testFolder = new Folder("test", "/");
    }

    /**
     * Test bean behaviors for renditions
     */
    @Test
    public void testRenditionBehavior() {
        FileOrRendition asset = new FileOrRendition(()->null, "name", "url", testFolder, Collections.EMPTY_MAP);
        FileOrRendition rendition = new FileOrRendition(()->null, "name", "url", testFolder, Collections.EMPTY_MAP);
        rendition.setAsRenditionOfImage("testRendition", "original asset");
        asset.addRendition(rendition);
        assertEquals("original asset", rendition.getOriginalAssetName());
        assertTrue("Is a file", rendition.isFile());
        assertTrue("Is a rendition", rendition.isRendition());
        assertFalse("Is not a folder", rendition.isFolder());
        assertEquals("Tracking rendition correctly", rendition, asset.getRenditions().get("testRendition"));
    }

    /**
     * Test asset behavior
     */
    @Test
    public void testAssetBehavior() {
        FileOrRendition instance = new FileOrRendition(()->null, "name", "url", testFolder, Collections.EMPTY_MAP);
        assertNull("No rendition name present", instance.getOriginalAssetName());
        assertNotNull("Renditions data strucutre always present", instance.getRenditions());
        assertTrue("Is a file", instance.isFile());
        assertFalse("Is not rendition", instance.isRendition());
        assertFalse("Is not a folder", instance.isFolder());
    }

    /**
     * Test of isFolder method, of class FileOrRendition.
     */
    @Test
    public void testFileSource() throws MalformedURLException, IOException {
        String basePath = new File(".").toURI().toURL().toString();
        
        FileOrRendition instance = new FileOrRendition(()->null, "name", basePath+"/pom.xml", testFolder, Collections.EMPTY_MAP);
        Source fileSource = instance.getSource();
        assertEquals(instance, fileSource.getElement());
        assertEquals("name", fileSource.getName());
        assertTrue("Able to determine file size", fileSource.getLength() > 0);
        assertTrue("Able to read file", fileSource.getStream().available() > 0);
        fileSource.close();
    }
    
}
