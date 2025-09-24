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

import com.adobe.acs.commons.contentsync.UpdateStrategy;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceUtil;
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
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;

import static com.adobe.acs.commons.contentsync.ContentCatalogJobConsumer.JOB_TOPIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestContentCatalogServlet {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCEPROVIDER_MOCK);

    private ContentCatalogServlet servlet;
    private UpdateStrategy updateStrategy;
    private JobManager jobManager;

    @Before
    public void setUp() {

        updateStrategy = mock(UpdateStrategy.class);
        jobManager = mock(JobManager.class);
        context.registerService(UpdateStrategy.class, updateStrategy);
        context.registerService(JobManager.class, jobManager);
        servlet = context.registerInjectActivateService(new ContentCatalogServlet());
    }

    @Test
    public void testSubmitNewJob() throws Exception {

        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.QUEUED);
        when(jobManager.addJob(eq(JOB_TOPIC), anyMap())).thenReturn(job);

        // Execute
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("root", "/content/test");
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        assertEquals("application/json", response.getContentType());
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("QUEUED", result.getString("status"));
    }

    @Test
    public void testFailedJobHaveErrorMessage() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        when(jobManager.getJobById(jobId)).thenReturn(null);
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.ERROR);
        doReturn(new String[]{"Something went wrong"}).when(job).getProperty(eq(Job.PROPERTY_JOB_PROGRESS_LOG));
        when(jobManager.getJobById(jobId)).thenReturn(job);
        when(jobManager.getJobById(jobId)).thenReturn(job);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("jobId", jobId);
        when(job.getJobState()).thenReturn(Job.JobState.ERROR);

        // Execute
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("ERROR", result.getString("status"));
        assertEquals("Something went wrong", result.getString(Job.PROPERTY_JOB_PROGRESS_LOG));
    }

    @Test
    public void testJobNotFoundException() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("jobId", jobId);

        // Execute
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("ERROR", result.getString("status"));
        String msg = result.getString(Job.PROPERTY_JOB_PROGRESS_LOG);
        assertTrue(msg, msg.contains("Sling job was not found by id"));
    }

    /**
     * The job results node was not found
     */
    @Test
    public void testJobResultsNotFoundException() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";

        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        doReturn(new String[]{"Something went wrong"}).when(job).getProperty(eq(Job.PROPERTY_JOB_PROGRESS_LOG));
        when(jobManager.getJobById(jobId)).thenReturn(job);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("jobId", jobId);
        when(job.getJobState()).thenReturn(Job.JobState.ERROR);

        // Execute
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        String resultsPath = ContentCatalogServlet.getJobResultsPath(job);
        assertEquals(jobId, result.getString("jobId"));
        assertEquals("ERROR", result.getString("status"));
        assertEquals("Something went wrong", result.getString(Job.PROPERTY_JOB_PROGRESS_LOG));
    }

    @Test
    public void testGetActiveJobStatus() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.QUEUED);
        when(jobManager.getJobById(jobId)).thenReturn(job);

        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("jobId", jobId);
        when(job.getJobState()).thenReturn(Job.JobState.ACTIVE);

        // Execute
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("ACTIVE", result.getString("status"));
    }

    @Test
    public void testGetCompletedJobResults() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.SUCCEEDED);
        when(jobManager.getJobById(jobId)).thenReturn(job);

        String resultsJson = "{\"resources\":[{\"path\":\"/content/test\",\"lastModified\":1234567890}]}";
        MockSlingHttpServletRequest request = context.request();
        request.addRequestParameter("jobId", jobId);

        String resultsPath = ContentCatalogServlet.getJobResultsPath(job);
        context.build()
                .resource("/content/test")
                .resource(ResourceUtil.getParent(resultsPath))
                .file(ResourceUtil.getName(resultsPath), new ByteArrayInputStream(resultsJson.getBytes()));

        // Execute
        MockSlingHttpServletResponse response = context.response();
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("SUCCEEDED", result.getString("status"));
        JsonArray resources = result.getJsonArray("resources");
        assertEquals(1, resources.size());
        assertEquals("/content/test", resources.get(0).asJsonObject().getString("path"));
    }

    @Test
    public void testGetJobResults_FiltersNonExistingResources() throws Exception {
        String resultsPath = "/results/path";
        // Simulate JSON results with two items, only one exists
        JsonArray remoteItems = Json.createArrayBuilder()
                .add(Json.createObjectBuilder().add("path", "/content/exists").build())
                .add(Json.createObjectBuilder().add("path", "/content/missing").build())
                .build();
        JsonObject resultsJson = Json.createObjectBuilder()
                .add(ContentCatalogServlet.JOB_RESOURCES, remoteItems)
                .build();

        // Simulate results resource
        context.build()
                .resource(ResourceUtil.getParent(resultsPath))
                .file(ResourceUtil.getName(resultsPath), new ByteArrayInputStream(resultsJson.toString().getBytes(StandardCharsets.UTF_8)))
                .resource("/content/exists")
        ;


        JsonArray filtered = servlet.getJobResults(context.request(), resultsPath);
        assertEquals(1, filtered.size());
        assertEquals("/content/exists", filtered.getJsonObject(0).getString("path"));
    }
}
