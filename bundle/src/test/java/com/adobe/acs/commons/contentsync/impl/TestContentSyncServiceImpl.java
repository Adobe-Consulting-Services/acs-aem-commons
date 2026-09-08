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
package com.adobe.acs.commons.contentsync.impl;
import com.adobe.acs.commons.adobeio.service.IntegrationService;
import com.adobe.acs.commons.contentsync.*;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkflowData;
import com.adobe.granite.workflow.model.WorkflowModel;
import org.apache.sling.api.resource.*;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.jcr.contentloader.ContentImporter;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class TestContentSyncServiceImpl {

    private ContentSyncServiceImpl service;
    private ContentImporter importer;
    private IntegrationService integrationService;
    private ResourceResolverFactory resolverFactory;
    private ResourceResolver resolver;
    private ExecutionContext context;
    private Job job;
    private RemoteInstance remoteInstance;
    private UpdateStrategy updateStrategy;

    @Before
    public void setUp() throws Exception {
        service = new ContentSyncServiceImpl();
        importer = mock(ContentImporter.class);
        integrationService = mock(IntegrationService.class);
        resolverFactory = mock(ResourceResolverFactory.class);
        resolver = mock(ResourceResolver.class);
        context = mock(ExecutionContext.class);
        job = mock(Job.class);
        remoteInstance = mock(RemoteInstance.class);
        updateStrategy = mock(UpdateStrategy.class);

        service.importer = importer;
        service.integrationService = integrationService;
        service.resourceResolverFactory = resolverFactory;

        when(context.getJob()).thenReturn(job);
        when(context.getRemoteInstance()).thenReturn(remoteInstance);
        when(resolverFactory.getServiceResourceResolver(anyMap())).thenReturn(resolver);
    }

    @Test
    public void testGetNodesToSort() {
        when(job.getProperty("root")).thenReturn("/content");
        CatalogItem item1 = mock(CatalogItem.class);
        CatalogItem item2 = mock(CatalogItem.class);
        when(item1.getPath()).thenReturn("/content/a/b");
        when(item2.getPath()).thenReturn("/content/a/c");
        List<CatalogItem> items = Arrays.asList(item1, item2);

        Set<String> result = service.getNodesToSort(items, context);
        assertTrue(result.contains("/content/a"));
    }

    @Test
    public void testSortNodesDryRun() throws Exception {
        when(context.dryRun()).thenReturn(true);
        when(context.getRemoteInstance()).thenReturn(remoteInstance);
        Collection<String> paths = Arrays.asList("/content/a", "/content/b");
        when(resolver.getResource(anyString())).thenReturn(mock(Resource.class));
        service.sortNodes(paths, context);
        // Should not throw
    }

    @Test
    public void testDeleteUnknownResourcesNoDeleteProperty() throws Exception {
        when(job.getProperty("delete")).thenReturn(null);
        service.deleteUnknownResources(context);
        // Should return without doing anything
    }

    @Test
    public void testStartWorkflowsNoModel() throws Exception {
        when(job.getProperty("workflowModel")).thenReturn(null);
        service.startWorkflows(Collections.emptyList(), context);
        // Should return without doing anything
    }

    @Test
    public void testStartWorkflowsModelNotFound() throws Exception {
        when(job.getProperty("workflowModel")).thenReturn("model");
        when(resolver.getResource(anyString())).thenReturn(mock(Resource.class));
        List<CatalogItem> items = new ArrayList<>();
        CatalogItem item = mock(CatalogItem.class);
        when(item.isUpdated()).thenReturn(true);
        when(item.getPath()).thenReturn("/content/a");
        items.add(item);

        WorkflowSession workflowSession = mock(WorkflowSession.class);
        when(resolver.adaptTo(WorkflowSession.class)).thenReturn(workflowSession);
        when(workflowSession.getModel("model")).thenReturn(null);

        service.startWorkflows(items, context);
        // Should log "cannot find workflow model"
    }

    @Test
    public void testStartWorkflowsHappyPath() throws Exception {
        when(job.getProperty("workflowModel")).thenReturn("model");
        when(resolver.getResource(anyString())).thenReturn(mock(Resource.class));
        List<CatalogItem> items = new ArrayList<>();
        CatalogItem item = mock(CatalogItem.class);
        when(item.isUpdated()).thenReturn(true);
        when(item.getPath()).thenReturn("/content/a");
        items.add(item);

        WorkflowSession workflowSession = mock(WorkflowSession.class);
        WorkflowModel workflowModel = mock(WorkflowModel.class);
        WorkflowData workflowData = mock(WorkflowData.class);

        when(resolver.adaptTo(WorkflowSession.class)).thenReturn(workflowSession);
        when(workflowSession.getModel("model")).thenReturn(workflowModel);
        when(workflowSession.newWorkflowData(anyString(), anyString())).thenReturn(workflowData);

        when(context.dryRun()).thenReturn(false);

        service.startWorkflows(items, context);
        verify(workflowSession).startWorkflow(workflowModel, workflowData);
    }
}