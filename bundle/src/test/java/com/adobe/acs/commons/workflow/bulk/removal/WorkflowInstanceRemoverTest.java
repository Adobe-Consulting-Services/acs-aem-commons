/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.workflow.bulk.removal;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.adobe.acs.commons.workflow.bulk.removal.impl.WorkflowInstanceRemoverImpl;
import com.adobe.granite.workflow.WorkflowSession;
import com.adobe.granite.workflow.exec.WorkItem;
import com.adobe.granite.workflow.exec.Workflow;
import com.day.cq.workflow.WorkflowService;
import io.wcm.testing.mock.aem.junit.AemContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class WorkflowInstanceRemoverTest {

    @Rule
    public final AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    private JobManager jobManager;

    @Mock
    WorkflowService workflowService;

    @Before
    public void setUp() throws Exception {
        ctx.load().json(getClass().getResourceAsStream("WorkflowInstanceRemoverTest.json"), "/var/workflow/instances");
        ctx.load().json(getClass().getResourceAsStream("WorkflowInstanceRemoverTest_empty.json"), "/etc/workflow/instances");

        WorkflowSession workflowSession = Mockito.mock(WorkflowSession.class);
        ctx.registerAdapter(ResourceResolver.class, WorkflowSession.class, workflowSession);

        Workflow workflow = Mockito.mock(Workflow.class);
        when(workflowSession.getWorkflow(anyString())).thenReturn(workflow);
        when(workflow.getWorkItems(any())).thenReturn(Arrays.asList(Mockito.mock(WorkItem.class)));

        ctx.registerService(JobManager.class, jobManager);
        ctx.registerService(WorkflowService.class, workflowService);
        ctx.registerInjectActivateService(new WorkflowInstanceRemoverImpl());
    }


    /**
     * IF the property doesn’t exist on the source, THEN do nothing (AEM will never remove a property entirely, so empty values are allowed after a property is created)
     */
    @Test
    public void removeAllTestWorkflows() throws Exception {
        Collection<String> modelIds = new ArrayList<>();
        modelIds.add("/var/workflow/models/test-workflow");
        modelIds.add("/var/workflow/models/test-workflow-two");

        Collection<String> statuses = new ArrayList<>();
        statuses.add("COMPLETED");
        statuses.add("ABORTED");
        statuses.add("RUNNING");

        Collection<String> payloadPaths = new ArrayList<>();
        payloadPaths.add("/content/dam/.*");
        Collection<Pattern> payloads = payloadPaths.stream().map(Pattern::compile).collect(Collectors.toList());

        WorkflowInstanceRemover workflowInstanceRemover = ctx.getService(WorkflowInstanceRemover.class);

        workflowInstanceRemover.removeWorkflowInstances(ctx.resourceResolver(), modelIds, statuses, payloads, null);

        assertEquals(3, workflowInstanceRemover.getStatus().getChecked());
        assertEquals("All workflows should be deleted",3, workflowInstanceRemover.getStatus().getRemoved());
    }

    @Test
    public void removeAllCompleted() throws Exception {
        Collection<String> modelIds = new ArrayList<>();
        modelIds.add("/var/workflow/models/test-workflow");
        modelIds.add("/var/workflow/models/test-workflow-two");

        Collection<String> statuses = new ArrayList<>();
        statuses.add("COMPLETED");

        Collection<String> payloadPaths = new ArrayList<>();
        payloadPaths.add("/content/dam/.*");
        Collection<Pattern> payloads = payloadPaths.stream().map(Pattern::compile).collect(Collectors.toList());

        WorkflowRemovalConfig workflowRemovalConfig = new WorkflowRemovalConfig(modelIds, statuses, payloads, null, -1);
        workflowRemovalConfig.setBatchSize(1);

        WorkflowInstanceRemover workflowInstanceRemover = ctx.getService(WorkflowInstanceRemover.class);
        workflowInstanceRemover.removeWorkflowInstances(ctx.resourceResolver(), workflowRemovalConfig);

        assertEquals("All workflows should have been checked",3, workflowInstanceRemover.getStatus().getChecked());
        assertEquals("Only completed workflows should be deleted",1, workflowInstanceRemover.getStatus().getRemoved());
    }

    @Test
    public void removePayloadSpecifiedWorkflow() throws Exception {
        Collection<String> modelIds = new ArrayList<>();
        modelIds.add("/var/workflow/models/test-workflow");
        modelIds.add("/var/workflow/models/test-workflow-two");

        Collection<String> statuses = new ArrayList<>();
        statuses.add("COMPLETED");
        statuses.add("ABORTED");
        statuses.add("RUNNING");

        Collection<String> payloadPaths = new ArrayList<>();
        payloadPaths.add("/content/dam/any/special/.*");
        Collection<Pattern> payloads = payloadPaths.stream().map(Pattern::compile).collect(Collectors.toList());

        WorkflowRemovalConfig workflowRemovalConfig = new WorkflowRemovalConfig(modelIds, statuses, payloads, null, -1);

        WorkflowInstanceRemover workflowInstanceRemover = ctx.getService(WorkflowInstanceRemover.class);
        workflowInstanceRemover.removeWorkflowInstances(ctx.resourceResolver(), workflowRemovalConfig);

        assertEquals("All workflows should have been checked",3, workflowInstanceRemover.getStatus().getChecked());
        assertEquals("Only workflows with specified workflow should be deleted", 1, workflowInstanceRemover.getStatus().getRemoved());
    }

    @Test
    public void removeWorkflowByModelId() throws Exception {
        Collection<String> modelIds = new ArrayList<>();
        modelIds.add("/var/workflow/models/test-workflow-two");

        Collection<String> statuses = new ArrayList<>();
        statuses.add("COMPLETED");
        statuses.add("ABORTED");
        statuses.add("RUNNING");

        Collection<String> payloadPaths = new ArrayList<>();
        payloadPaths.add("/content/dam/.*");
        Collection<Pattern> payloads = payloadPaths.stream().map(Pattern::compile).collect(Collectors.toList());

        WorkflowRemovalConfig workflowRemovalConfig = new WorkflowRemovalConfig(modelIds, statuses, payloads, null, -1);

        WorkflowInstanceRemover workflowInstanceRemover = ctx.getService(WorkflowInstanceRemover.class);
        int removed = workflowInstanceRemover.removeWorkflowInstances(ctx.resourceResolver(), workflowRemovalConfig);

        assertEquals("All workflows should have been checked",3, workflowInstanceRemover.getStatus().getChecked());
        assertEquals("Only workflows with specified modelID should be deleted", 2, workflowInstanceRemover.getStatus().getRemoved());
    }
}