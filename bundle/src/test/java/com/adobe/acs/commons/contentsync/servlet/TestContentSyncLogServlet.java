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

import com.adobe.acs.commons.contentsync.ExecutionContext;
import com.adobe.acs.commons.contentsync.io.JobLogWriter;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.servlet.http.HttpServletResponse;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestContentSyncLogServlet {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    private ContentSyncLogServlet servlet;
    private JobManager jobManager;
    private ResourceResolverFactory resourceResolverFactory;

    private String jobId = "2025/09/30/test-job";

    @Before
    public void setUp() throws Exception {
        jobManager = mock(JobManager.class);
        context.registerService(JobManager.class, jobManager);
        servlet = context.registerInjectActivateService(new ContentSyncLogServlet());
        context.requestPathInfo().setExtension("json");

        resourceResolverFactory = mock(ResourceResolverFactory.class);
        when(resourceResolverFactory.getServiceResourceResolver(anyMap())).thenReturn(context.resourceResolver());

    }

    @Test
    public void testDoGet_NoSuffix() throws Exception {
        context.requestPathInfo().setSuffix(null);

        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertTrue(output.contains("Usage: ...sync.log.txt/jobId"));
        assertEquals(HttpServletResponse.SC_BAD_REQUEST, context.response().getStatus());
    }

    @Test
    public void testDoGet_JobNotFound() throws Exception {
        context.requestPathInfo().setSuffix("/" + jobId);

        try {
            servlet.doGet(context.request(), context.response());
            fail("Expected FileNotFoundException");
        } catch (java.io.FileNotFoundException e) {
            assertTrue(e.getMessage().contains("Job not found: "));
        }
    }

    @Test
    public void testDoGet_WithLogResourceAndProgressLog() throws Exception {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(jobManager.getJobById(eq(jobId))).thenReturn(job);
        context.requestPathInfo().setSuffix("/" + jobId);

        String logPath = ExecutionContext.getLogPath(job);


        try (JobLogWriter logWriter = new JobLogWriter(resourceResolverFactory, logPath)) {
            logWriter.write("log line 1");
            logWriter.write("log line 2");
        }

        // Simulate job progress log
        when(job.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG)).thenReturn(new String[]{"progress1", "progress2"});

        // Actually call doGet
        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        // Since we can't inject our iterator, just check progress log output
        assertEquals(
                "log line 1" + System.lineSeparator() +
                        "log line 2" + System.lineSeparator() +
                        "progress1" + System.lineSeparator() +
                        "progress2" + System.lineSeparator(), output);
    }

    @Test
    public void testDoGet_WithOnlyProgressLog() throws Exception {
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(jobManager.getJobById(eq(jobId))).thenReturn(job);
        context.requestPathInfo().setSuffix("/" + jobId);

        when(job.getProperty(Job.PROPERTY_JOB_PROGRESS_LOG)).thenReturn(new String[]{"progress1", "progress2"});

        servlet.doGet(context.request(), context.response());

        String output = context.response().getOutputAsString();
        assertEquals(
                "progress1" + System.lineSeparator() +
                "progress2" + System.lineSeparator());
    }
}
