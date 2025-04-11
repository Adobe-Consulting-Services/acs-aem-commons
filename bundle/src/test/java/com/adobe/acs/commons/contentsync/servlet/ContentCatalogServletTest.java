package com.adobe.acs.commons.contentsync.servlet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceNotFoundException;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@ExtendWith(MockitoExtension.class)
class ContentCatalogServletTest {

    @Mock
    private JobManager jobManager;

    @Mock
    private SlingHttpServletRequest request;

    @Mock
    private SlingHttpServletResponse response;

    @Mock
    private ResourceResolver resourceResolver;

    @Mock
    private Job job;

    @Mock
    private Resource resultsResource;

    @InjectMocks
    private ContentCatalogServlet servlet;

    private StringWriter writer;

    @BeforeEach
    void setUp() throws Exception {
        writer = new StringWriter();
        when(response.getWriter()).thenReturn(new java.io.PrintWriter(writer));
        when(request.getResourceResolver()).thenReturn(resourceResolver);
    }

    @Test
    void testSubmitNewJob() throws Exception {
        // Setup
        Map<String, String[]> params = new HashMap<>();
        params.put("path", new String[]{"/content/test"});
        when(request.getParameterMap()).thenReturn(params);
        when(request.getParameter("jobId")).thenReturn(null);

        String jobId = "2025/4/10/test-job";
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.QUEUED);
//        when(jobManager.createJob(anyString())).thenReturn(new TestJobBuilder(job));

        // Execute
        servlet.doGet(request, response);

        // Verify
        verify(response).setContentType("application/json");
        JsonReader jsonReader = Json.createReader(new java.io.StringReader(writer.toString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("QUEUED", result.getString("status"));
    }

    @Test
    void testGetActiveJobStatus() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        when(request.getParameter("jobId")).thenReturn(jobId);
        when(jobManager.getJobById(jobId)).thenReturn(job);
        when(job.getJobState()).thenReturn(Job.JobState.ACTIVE);

        // Execute
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new java.io.StringReader(writer.toString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("ACTIVE", result.getString("status"));
    }

    @Test
    void testGetCompletedJobResults() throws Exception {
        // Setup
        String jobId = "2025/4/10/test-job";
        String resultsJson = "{\"resources\":[{\"path\":\"/content/test\",\"lastModified\":1234567890}]}";

        when(request.getParameter("jobId")).thenReturn(jobId);
        when(jobManager.getJobById(jobId)).thenReturn(null); // Job completed
        when(resourceResolver.getResource("/var/acs-commons/contentsync/jobs/" + jobId + "/results"))
            .thenReturn(resultsResource);
//        when(resultsResource.adaptTo(InputStream.class))
//            .thenReturn(new ByteArrayInputStream(resultsJson.getBytes(StandardCharsets.UTF_8)));

        // Execute
        servlet.doGet(request, response);

        // Verify
        JsonReader jsonReader = Json.createReader(new java.io.StringReader(writer.toString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("SUCCEEDED", result.getString("status"));
        assertTrue(result.containsKey("resources"));
    }

    @Test
    void testGetJobResultsNotFound() {
        // Setup
        String jobId = "2025/4/10/test-job";
        when(resourceResolver.getResource(anyString())).thenReturn(null);

        // Verify
        assertThrows(ResourceNotFoundException.class, () -> {
            servlet.getJobResults(resourceResolver, jobId);
        });
    }

    // Helper class to mock Job.Builder
    private static class TestJobBuilder {
        private final Job job;
        private Map<String, Object> properties = new HashMap<>();

        TestJobBuilder(Job job) {
            this.job = job;
        }

        public TestJobBuilder properties(Map<String, Object> properties) {
            this.properties = properties;
            return this;
        }

        public Job add() {
            return job;
        }
    }
}