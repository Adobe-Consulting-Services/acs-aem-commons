/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.junit.Before;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

public class TestContentCatalog {

    private static final String CATALOG_SERVLET = "/bin/catalog";
    private static final String TEST_PATH = "/content/test";
    private static final String TEST_STRATEGY = "test.strategy";
    private static final String TEST_JOB_ID = "2025/4/10/test-job";

    private RemoteInstance remoteInstance;
    private ContentCatalog contentCatalog;

    @Before
    public void setUp() throws Exception {
        remoteInstance = mock(RemoteInstance.class);
        contentCatalog = new ContentCatalog(remoteInstance, CATALOG_SERVLET);
    }

    @Test
    public void testStartCatalogJob() throws IOException, URISyntaxException {
        // Setup
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("jobId", TEST_JOB_ID)
            .add("status", "QUEUED");

        when(remoteInstance.getJson(any())).thenReturn(builder.build());

        // Execute
        String jobId = contentCatalog.startCatalogJob(TEST_PATH, TEST_STRATEGY, true);

        // Verify
        assertEquals(TEST_JOB_ID, jobId);
    }

    @Test
    public void testIsComplete_Succeeded() throws IOException, URISyntaxException {
        // Setup
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("status", "SUCCEEDED")
            .add("resources", Json.createArrayBuilder()
                .add(Json.createObjectBuilder()
                    .add("path", TEST_PATH)
                    .add("lastModified", 123456789L)
                    .build())
                .build());

        when(remoteInstance.getJson(any())).thenReturn(builder.build());

        // Execute
        boolean result = contentCatalog.isComplete(TEST_JOB_ID);

        // Verify
        assertTrue(result);
        assertNotNull(contentCatalog.getResults());
        assertEquals(1, contentCatalog.getResults().size());
    }

    @Test
    public void testIsCompleteError() throws URISyntaxException, IOException {
        // Setup
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("status", "ERROR")
            .add("message", "Test error message");

        when(remoteInstance.getJson(any())).thenReturn(builder.build());

        // Verify
        assertThrows(IllegalStateException.class, () -> {
            contentCatalog.isComplete(TEST_JOB_ID);
        });
    }

    @Test
    public void testGetDelta() {
        // Setup
        CatalogItem item1 = new CatalogItem(Json.createObjectBuilder()
            .add("path", "/content/test1")
            .add("lastModified", 123456789L)
            .build());

        CatalogItem item2 = new CatalogItem(Json.createObjectBuilder()
            .add("path", "/content/test2")
            .add("lastModified", 123456789L)
            .build());

        ResourceResolver resourceResolver = mock(ResourceResolver.class);
        Resource resource1 = mock(Resource.class);
        when(resourceResolver.getResource("/content/test1")).thenReturn(resource1);
        when(resourceResolver.getResource("/content/test2")).thenReturn(null);

        UpdateStrategy updateStrategy = mock(UpdateStrategy.class);
        when(updateStrategy.isModified(item1, resource1)).thenReturn(false);
        when(updateStrategy.isModified(item1, resource1)).thenReturn(true);

        List<CatalogItem> catalog = Arrays.asList(item1, item2);

        // Execute
        List<CatalogItem> delta = contentCatalog.getDelta(catalog, resourceResolver, updateStrategy);

        // Verify
        assertEquals(2, delta.size());
        assertTrue(delta.contains(item1)); // Modified
        assertTrue(delta.contains(item2)); // Not present
    }

    @Test
    public void testCheckStatus_InProgress() throws IOException, URISyntaxException {
        // Setup
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("status", "ACTIVE");

        // Execute
        contentCatalog.checkStatus(builder.build());

        // Verify
        assertNull(contentCatalog.getResults());
    }

    @Test
    public void testCheckStatus_GivenUp() {
        // Setup
        JsonObjectBuilder builder = Json.createObjectBuilder()
            .add("status", "GIVEN_UP")
            .add("message", "Test error message");

        // Verify
        assertThrows(IllegalStateException.class, () -> {
            contentCatalog.checkStatus(builder.build());
        });
    }
}