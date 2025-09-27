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

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.api.JackrabbitSession;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletRequest;
import org.apache.sling.testing.mock.sling.servlet.MockSlingHttpServletResponse;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import static com.adobe.acs.commons.contentsync.ContentSyncJobConsumer.JOB_TOPIC;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class TestContentSyncRunServlet {
    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_OAK);

    private ContentSyncRunServlet servlet;
    private JobManager jobManager;
    private UserManager userManager;

    @Before
    public void setUp() throws RepositoryException {

        jobManager = mock(JobManager.class);

        context.registerService(JobManager.class, jobManager);
        servlet = context.registerInjectActivateService(new ContentSyncRunServlet(),
                Collections.singletonMap("allowedGroups", new String[]{"administrators"}));

        String servletPath = "/contentsync/_jcr_content/sync";
        context.build().resource(servletPath).commit();
        context.request().setResource(context.resourceResolver().getResource(servletPath));
        context.request().addRequestParameter("root", "/content/test");

        JackrabbitSession session = (JackrabbitSession) context.resourceResolver().adaptTo(Session.class);
        userManager = spy(session.getUserManager());
        context.registerAdapter(ResourceResolver.class, UserManager.class, userManager);
    }

    @Test
    public void testDoPost_Success() throws Exception {

        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.QUEUED);
        when(jobManager.addJob(eq(JOB_TOPIC), anyMap())).thenReturn(job);

        // Execute
        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        servlet.doPost(request, response);

        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals(jobId, result.getString("jobId"));
        assertEquals("QUEUED", result.getString("status"));
    }

    @Test
    public void testDoPost_AccessDenied() throws Exception {
        // No allowed groups, user is not admin
        User testUser = spy(userManager.createUser("alice", "alicespassword"));
        Group testGroup = userManager.createGroup("testgroup");
        doReturn(Arrays.asList(testGroup).iterator()).when(testUser).memberOf();
        doReturn(testUser).when(userManager).getAuthorizable(anyString());

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        servlet.doPost(request, response);

        verify(jobManager, never()).addJob(any(), anyMap());
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();
        String error = result.getString("error");
        assertTrue(error.contains("You do not have permission to run content sync"));
    }

    @Test
    public void testDoPost_AccessAllowed() throws Exception {
        String jobId = "2025/4/10/test-job";
        Job job = mock(Job.class);
        when(job.getId()).thenReturn(jobId);
        when(job.getJobState()).thenReturn(Job.JobState.QUEUED);
        when(jobManager.addJob(eq(JOB_TOPIC), anyMap())).thenReturn(job);

        // user is a member of an allowed group
        User testUser = spy(userManager.createUser("alice", "alicespassword"));
        Group testGroup = userManager.createGroup("administrators");
        testGroup.addMember(testUser);
        doReturn(Arrays.asList(testGroup).iterator()).when(testUser).memberOf();
        doReturn(testUser).when(userManager).getAuthorizable(anyString());

        MockSlingHttpServletRequest request = context.request();
        MockSlingHttpServletResponse response = context.response();
        servlet.doPost(request, response);

        verify(jobManager, atLeastOnce()).addJob(any(), anyMap());
        JsonReader jsonReader = Json.createReader(new StringReader(response.getOutputAsString()));
        JsonObject result = jsonReader.readObject();

        assertEquals("QUEUED", result.getString("status"));
    }

}
