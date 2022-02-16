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
package com.adobe.acs.commons.mcp.impl.processes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.mockito.stubbing.Answer;
import org.mockito.invocation.InvocationOnMock;

import static com.adobe.acs.commons.mcp.impl.processes.RefreshFolderTumbnails.ThumbnailScanLogic.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Assert that folders will be detected or skipped in different cases
 */
public class RefreshFolderTumbnailsTest {

    public RefreshFolderTumbnailsTest() {
    }

    Date currentDate = new Date();
    Date previousDate = new Date(currentDate.getTime() - 10);
    ResourceResolver rr;
    Resource noThumbnail;
    Resource automaticThumbnail;
    Resource placeholderThumbnail;
    Resource manualThumbnail;
    Resource outdatedThumbnail;
    Resource currentThumbnail;

    @Before
    public void setUp() {
        rr = mock(ResourceResolver.class);
        doAnswer((Answer) (InvocationOnMock invocation) -> {
            String path = (String) invocation.getArguments()[0];
            return getResource(path);
        }).when(rr).getResource(anyString());

        noThumbnail = mockResource("/missingThumbnail");
        manualThumbnail = mockResource("/manual");
        mockResource("/manual/jcr:content", modifiedDate(currentDate));
        mockResource("/manual/jcr:content/manualThumbnail.png");
        automaticThumbnail = mockResource("/automatic");
        mockResource("/automatic/jcr:content", modifiedDate(currentDate));
        mockResource("/automatic/jcr:content/folderThumbnail", attachBinary(2048));
        placeholderThumbnail = mockResource("/placeholder");
        mockResource("/placeholder/jcr:content", modifiedDate(currentDate));
        mockResource("/placeholder/jcr:content/folderThumbnail", attachBinary(950));
        outdatedThumbnail = mockResource("/outdated");
        mockResource("/outdated/jcr:content", modifiedDate(currentDate));
        mockResource("/outdated/jcr:content/folderThumbnail", attachBinary(2048));
        mockResource("/outdated/jcr:content/folderThumbnail/jcr:content", 
                modifiedDateAndPaths(previousDate, "/automatic","/manual", "/placeholder"));
        currentThumbnail = mockResource("/current");
        mockResource("/current/jcr:content", modifiedDate(currentDate));
        mockResource("/current/jcr:content/folderThumbnail", attachBinary(2048));
        mockResource("/current/jcr:content/folderThumbnail/jcr:content", 
                modifiedDateAndPaths(currentDate, "/automatic","/manual", "/placeholder"));
    }

    @Test
    public void basicAssertions() throws IOException {
        assertTrue(RefreshFolderTumbnails.isThumbnailMissing(noThumbnail));
        assertTrue(RefreshFolderTumbnails.isPlaceholderThumbnail(placeholderThumbnail));
        assertTrue(RefreshFolderTumbnails.isThumbnailManual(manualThumbnail));
        assertTrue(RefreshFolderTumbnails.isThumbnailAutomatic(automaticThumbnail));        
    }

    @Test
    public void scanMissingThumbnail() throws Exception {
        assertTrue(MISSING.shouldReplace(noThumbnail));
        assertFalse(MISSING.shouldReplace(automaticThumbnail));
        assertFalse(MISSING.shouldReplace(manualThumbnail));
        assertFalse(MISSING.shouldReplace(placeholderThumbnail));
        assertFalse(MISSING.shouldReplace(outdatedThumbnail));
        assertFalse(MISSING.shouldReplace(currentThumbnail));
    }

    @Test
    public void scanPlaceholderThumbnail() throws IOException, Exception {
        assertTrue(PLACEHOLDERS.shouldReplace(noThumbnail));
        assertFalse(PLACEHOLDERS.shouldReplace(automaticThumbnail));
        assertFalse(PLACEHOLDERS.shouldReplace(manualThumbnail));
        assertTrue(PLACEHOLDERS.shouldReplace(placeholderThumbnail));
        assertFalse(PLACEHOLDERS.shouldReplace(outdatedThumbnail));
        assertFalse(PLACEHOLDERS.shouldReplace(currentThumbnail));
    }

    @Test
    public void scanOutdatedThumbnail() throws Exception {
        assertTrue(OUTDATED.shouldReplace(noThumbnail));
        assertTrue(OUTDATED.shouldReplace(automaticThumbnail)); // Because it's missing sufficient detail
        assertFalse(OUTDATED.shouldReplace(manualThumbnail));
        assertTrue(OUTDATED.shouldReplace(placeholderThumbnail));
        assertTrue(OUTDATED.shouldReplace(outdatedThumbnail));
        assertFalse(OUTDATED.shouldReplace(currentThumbnail));
    }

    @Test
    public void scanAutomaticOrMissing() throws Exception {
        assertTrue(ALL_AUTOMATIC_OR_MISSING.shouldReplace(noThumbnail));
        assertTrue(ALL_AUTOMATIC_OR_MISSING.shouldReplace(automaticThumbnail));
        assertFalse(ALL_AUTOMATIC_OR_MISSING.shouldReplace(manualThumbnail));
        assertTrue(ALL_AUTOMATIC_OR_MISSING.shouldReplace(placeholderThumbnail));
        assertTrue(ALL_AUTOMATIC_OR_MISSING.shouldReplace(outdatedThumbnail));
        assertTrue(ALL_AUTOMATIC_OR_MISSING.shouldReplace(currentThumbnail));
    }

    @Test
    public void scanAll() throws Exception {
        assertTrue(ALL.shouldReplace(noThumbnail));
        assertTrue(ALL.shouldReplace(automaticThumbnail));
        assertTrue(ALL.shouldReplace(manualThumbnail));
        assertTrue(ALL.shouldReplace(placeholderThumbnail));
        assertTrue(ALL.shouldReplace(outdatedThumbnail));
        assertTrue(ALL.shouldReplace(currentThumbnail));
    }

    Map<String, Resource> resources = new TreeMap<String, Resource>();

    public Resource mockResource(String path, BiConsumer<String, Resource>... setupFunctions) {
        Resource res = mock(Resource.class);
        when(res.getPath()).thenReturn(path);
        when(res.getResourceResolver()).thenReturn(rr);
        doAnswer((Answer) (InvocationOnMock invocation) -> {
            String relPath = (String) invocation.getArguments()[0];
            return getResource(path + "/" + relPath);
        }).when(res).getChild(anyString());
        for (BiConsumer<String, Resource> setup : setupFunctions) {
            setup.accept(path, res);
        }
        resources.put(path, res);
        return res;
    }

    public Resource getResource(String path) {
        return resources.get(path);
    }

    private BiConsumer<String, Resource> attachBinary(int size) {
        return (path, res) -> {
            when(res.adaptTo(InputStream.class)).thenReturn(new ByteArrayInputStream(new byte[size]));
        };
    }
    
    private BiConsumer<String, Resource> modifiedDate(Date date) {
        return (path, res) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("jcr:lastModified", date);
            when(res.getValueMap()).thenReturn(new ValueMapDecorator(map));
        };
    }
    
    private BiConsumer<String, Resource> modifiedDateAndPaths(Date date, String... paths) {
        return (path, res) -> {
            Map<String, Object> map = new HashMap<>();
            map.put("jcr:lastModified", date);
            map.put("dam:folderThumbnailPaths", paths);
            when(res.getValueMap()).thenReturn(new ValueMapDecorator(map));
        };
    }    
}
