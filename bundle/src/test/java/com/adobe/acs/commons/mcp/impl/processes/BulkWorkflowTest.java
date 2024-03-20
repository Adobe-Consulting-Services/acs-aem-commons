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
package com.adobe.acs.commons.mcp.impl.processes;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.util.impl.QueryHelperImpl;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.day.cq.workflow.WorkflowException;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

public class BulkWorkflowTest {

    @Rule
    public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    private SyntheticWorkflowRunner syntheticWorkflowRunner;
    private BulkWorkflow bulkWorkflow;
    private QueryHelper queryHelper;
    private ActionManager actionManager;

    @Before
    public void setUp() throws Exception {
        ctx.load().json("/com/adobe/acs/commons/mcp/impl/processes/BulkWorkflowTest.json", "/content/dam");

        syntheticWorkflowRunner = Mockito.mock(SyntheticWorkflowRunner.class);

        Mockito.doReturn(Mockito.mock(SyntheticWorkflowModel.class)).when(syntheticWorkflowRunner).getSyntheticWorkflowModel(Mockito.any(ResourceResolver.class),
                Mockito.eq("/var/workflow/models/test"),
                Mockito.eq(true));

        queryHelper = new QueryHelperImpl();
        actionManager = Mockito.mock(ActionManager.class);

        Mockito.doAnswer(invocation -> {
            CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
            method.accept(ctx.resourceResolver());
            return null;
        }).when(actionManager).withResolver(Mockito.any(CheckedConsumer.class));


        Mockito.doAnswer(invocation -> {
            CheckedConsumer<ResourceResolver> method = (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
            method.accept(ctx.resourceResolver());
            return null;
        }).when(actionManager).deferredWithResolver(Mockito.any(CheckedConsumer.class));
    }

    @Test
    public void processPayloads() throws Exception {
        bulkWorkflow = Mockito.spy(new BulkWorkflow(queryHelper, syntheticWorkflowRunner));

        bulkWorkflow.queryLanguage = BulkWorkflow.QueryLanguage.LIST;
        bulkWorkflow.queryStatement =
                "/content/dam/test-1.png"
                        + "\n"
                        + "/content/dam/test-2.png";

        bulkWorkflow.workflowId = "/var/workflow/models/test";
        bulkWorkflow.relativePayloadPath = "";

        bulkWorkflow.processPayloads(actionManager);

        Mockito.verify(syntheticWorkflowRunner, Mockito.times(1)).execute(
                Mockito.eq(ctx.resourceResolver()),
                Mockito.eq("/content/dam/test-1.png"),
                Mockito.any(SyntheticWorkflowModel.class),
                Mockito.eq(false),
                Mockito.eq(true));

        Mockito.verify(bulkWorkflow, Mockito.times(1)).record(
                Mockito.eq("/content/dam/test-1.png"),
                Mockito.eq(BulkWorkflow.ItemStatus.SUCCESS),
                Mockito.anyLong());

        Mockito.verify(syntheticWorkflowRunner, Mockito.times(1)).execute(
                Mockito.eq(ctx.resourceResolver()),
                Mockito.eq("/content/dam/test-2.png"),
                Mockito.any(SyntheticWorkflowModel.class),
                Mockito.eq(false),
                Mockito.eq(true));

        Mockito.verify(bulkWorkflow, Mockito.times(1)).record(
                Mockito.eq("/content/dam/test-2.png"),
                Mockito.eq(BulkWorkflow.ItemStatus.SUCCESS),
                Mockito.anyLong());

        Mockito.verify(syntheticWorkflowRunner, Mockito.times(0)).execute(
                Mockito.eq(ctx.resourceResolver()),
                Mockito.eq("/content/dam/non-existing.png"),
                Mockito.any(SyntheticWorkflowModel.class),
                Mockito.eq(false),
                Mockito.eq(true));
    }

    @Test
    public void processPayloads_WithFailure() throws Exception {
        bulkWorkflow = Mockito.spy(new BulkWorkflow(queryHelper, syntheticWorkflowRunner));

        bulkWorkflow.queryLanguage = BulkWorkflow.QueryLanguage.LIST;
        bulkWorkflow.queryStatement =
                "/content/dam/test-1.png";
        bulkWorkflow.workflowId = "/var/workflow/models/test";
        bulkWorkflow.relativePayloadPath = "";

        Mockito.doThrow(WorkflowException.class).when(syntheticWorkflowRunner).execute(Mockito.eq(ctx.resourceResolver()),
                Mockito.eq("/content/dam/test-1.png"),
                Mockito.any(SyntheticWorkflowModel.class),
                Mockito.eq(false),
                Mockito.eq(true));

        bulkWorkflow.processPayloads(actionManager);

        Mockito.verify(syntheticWorkflowRunner, Mockito.times(1)).execute(
                Mockito.eq(ctx.resourceResolver()),
                Mockito.eq("/content/dam/test-1.png"),
                Mockito.any(SyntheticWorkflowModel.class),
                Mockito.eq(false),
                Mockito.eq(true));

        Mockito.verify(bulkWorkflow, Mockito.times(1)).record(
                Mockito.eq("/content/dam/test-1.png"),
                Mockito.eq(BulkWorkflow.ItemStatus.FAILURE),
                Mockito.anyLong());
    }
}