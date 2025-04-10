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
package com.adobe.acs.commons.contentsync.servlet;

import com.adobe.acs.commons.contentsync.CatalogItem;
import com.adobe.acs.commons.contentsync.UpdateStrategy;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;
import static com.adobe.acs.commons.contentsync.TestUtils.getParameters;
import static javax.servlet.http.HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestContentCatalogServlet {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    private ContentCatalogServlet servlet;
    private UpdateStrategy updateStrategy;
    private JobManager jobManager;
    private Job job;

    @Before
    public void setUp() {

        updateStrategy = mock(UpdateStrategy.class);
        jobManager = mock(JobManager.class);
        job = mock(Job.class);
        doReturn(UUID.randomUUID().toString()).when(job).getId();
        doReturn(Job.JobState.QUEUED).when(job).getJobState();
        doReturn(job).when(jobManager).addJob(eq(JOB_TOPIC), anyMap());
        context.registerService(UpdateStrategy.class, updateStrategy);
        context.registerService(JobManager.class, jobManager);
        servlet = context.registerInjectActivateService(new ContentCatalogServlet());
    }

    @Test
    public void testMissingRequiredParameters() throws IOException, LoginException {
        doAnswer(invocation -> {
            throw new IllegalArgumentException("root request parameter is required");
        }).when(updateStrategy).getItems(eq(getParameters(context.request())));

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertEquals("root request parameter is required", jsonResponse.getString("error"));
    }

    /**
     * return an empty array if the requested path does not exist
     */
    @Test
    public void testContentTreeDoesNotExist() throws IOException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/wknd");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertTrue("resources[] is missing in the response json", jsonResponse.containsKey("resources"));

        JsonArray resources = jsonResponse.getJsonArray("resources");

        assertEquals(0, resources.size());
    }

    @Test
    public void testPageTree() throws IOException, LoginException {
        doAnswer(invocation -> {
            List<CatalogItem> items = new ArrayList<>();
            JsonObject o1 = Json.createObjectBuilder()
                    .add("path", "/content/wknd")
                    .add("jcr:primaryType", "cq:Page")
                    .build();
            JsonObject o2 = Json.createObjectBuilder()
                    .add("path", "/content/wknd/page1")
                    .add("jcr:primaryType", "cq:Page")
                    .build();
            items.add(new CatalogItem(o1));
            items.add(new CatalogItem(o2));
            return items;
        }).when(updateStrategy).getItems(eq(getParameters(context.request())));

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/wknd");
        request.addRequestParameter("strategy", updateStrategy.getClass().getName());
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_OK, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertTrue("resources[] is missing in the response json", jsonResponse.containsKey("resources"));

        JsonArray resources = jsonResponse.getJsonArray("resources");
        assertEquals(2, resources.size());

        // first item is the root
        JsonObject item1 = resources.getJsonObject(0);
        assertEquals("/content/wknd", item1.getString("path"));
        assertEquals("cq:Page", item1.getString("jcr:primaryType"));

        JsonObject item2 = resources.getJsonObject(1);
        assertEquals("/content/wknd/page1", item2.getString("path"));
        assertEquals("cq:Page", item2.getString("jcr:primaryType"));
    }

    @Test
    public void testInvalidStrategyPid() throws IOException {
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/wknd");
        request.addRequestParameter("strategy", "invalid");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        assertEquals("application/json", response.getContentType());
        assertEquals(SC_INTERNAL_SERVER_ERROR, response.getStatus());
        JsonObject jsonResponse = Json.createReader(new StringReader(response.getOutputAsString())).readObject();
        assertTrue(jsonResponse.getString("error").startsWith("Cannot find UpdateStrategy for pid"));
    }
}
