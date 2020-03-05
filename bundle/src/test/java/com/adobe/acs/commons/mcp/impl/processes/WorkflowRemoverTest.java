/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.functions.CheckedConsumer;
import com.adobe.acs.commons.mcp.form.workflow.WorkflowStatusSelector;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalStatus;

import io.wcm.testing.mock.aem.junit.AemContext;

public class WorkflowRemoverTest {

    @Rule
    public AemContext ctx = new AemContext(ResourceResolverType.JCR_MOCK);

    private static final Logger log = LoggerFactory.getLogger(WorkflowRemoverTest.class);

    private WorkflowRemover remover;
    private ActionManager actionManager;
    private WorkflowInstanceRemover workflowInstanceRemover;

    @Before
    public void setUp() throws Exception {
        workflowInstanceRemover = Mockito.mock(WorkflowInstanceRemover.class);
        remover = new WorkflowRemover(workflowInstanceRemover);

        actionManager = Mockito.mock(ActionManager.class);

        Mockito.doAnswer(
                invocation -> {
                    CheckedConsumer<ResourceResolver> method =
                            (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                    method.accept(ctx.resourceResolver());
                    return null;
                }).when(actionManager).withResolver(Mockito.any(CheckedConsumer.class));

        Mockito.doAnswer(
                invocation -> {
                    CheckedConsumer<ResourceResolver> method =
                            (CheckedConsumer<ResourceResolver>) invocation.getArguments()[0];
                    method.accept(ctx.resourceResolver());
                    return null;
                }).when(actionManager).deferredWithResolver(Mockito.any(CheckedConsumer.class));
    }

    @Test
    public void parseParametersNone() throws Exception {
        remover.parseParameters();

        final List<String> modelIds = remover.getModelIds();
        final Calendar olderThan = remover.getOlderThan();
        final List<Pattern> payloads = remover.getPayloads();
        final List<String> statuses = remover.getStatuses();

        assertTrue("Models should be null or empty",modelIds==null || modelIds.size()==0);
        assertNull("Calendar must be null", olderThan);
        assertTrue("Payloads should be null or empty",payloads==null || payloads.size()==0);
        assertTrue("Status should be null or empty",statuses==null || statuses.size()==0);
    }

    @Test
    public void parseParametersValidValues() throws Exception {
        remover.payloadPaths = new ArrayList<>();
        remover.payloadPaths.add("/content/dam/.*");
        remover.payloadPaths.add("/content/mySite/.*");

        remover.olderThanVal = "2019-08-24T00:00:00";

        remover.modelIds = new ArrayList<>();
        remover.modelIds.add("/var/workflow/models/dam/update_asset");
        remover.modelIds.add("/var/workflow/models/dam/process_subasset");
        remover.modelIds.add("/var/workflow/models/request_for_deletion");

        remover.statuses = new ArrayList<>();
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.STALE.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.ABORTED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.COMPLETED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.SUSPENDED.name());

        remover.parseParameters();

        assertEquals("There should be 2 payload paths", 2, remover.getPayloads().size());
        assertEquals("Pattern does not match input", "/content/dam/.*", remover.getPayloads().get(0).pattern());
        assertEquals("Pattern does not match input", "/content/mySite/.*", remover.getPayloads().get(1).pattern());


        assertEquals("Older than does not match year input", 2019, remover.getOlderThan().get(Calendar.YEAR));
        assertEquals("Older than does not match month input", 8, remover.getOlderThan().get(Calendar.MONTH) + 1);
        assertEquals("Older than does not match day input", 24, remover.getOlderThan().get(Calendar.DAY_OF_MONTH));

        assertEquals("There should be 2 models.", 3, remover.getModelIds().size());
        assertEquals("Model path does not match input", "/var/workflow/models/dam/update_asset", remover.getModelIds().get(0));
        assertEquals("Model path does not match input", "/var/workflow/models/request_for_deletion", remover.getModelIds().get(2));


        assertEquals("There should be 4 statuses.", 4, remover.getStatuses().size());
        assertEquals("Status does not match input", WorkflowStatusSelector.WorkflowStatus.STALE.name(), remover.getStatuses().get(0));
        assertEquals("Status does not match input", WorkflowStatusSelector.WorkflowStatus.COMPLETED.name(), remover.getStatuses().get(2));
        assertEquals("Status does not match input", WorkflowStatusSelector.WorkflowStatus.SUSPENDED.name(), remover.getStatuses().get(3));

    }

    @Test(expected = ParseException.class)
    public void parseParametersBadDateInput() throws Exception {
        remover.payloadPaths = new ArrayList<>();
        remover.payloadPaths.add("/content/dam/.*");
        remover.payloadPaths.add("/content/mySite/.*");

        remover.olderThanVal = "8-24-201900:00:00";

        remover.modelIds = new ArrayList<>();
        remover.modelIds.add("/var/workflow/models/dam/update_asset");
        remover.modelIds.add("/var/workflow/models/dam/process_subasset");
        remover.modelIds.add("/var/workflow/models/request_for_deletion");

        remover.statuses = new ArrayList<>();
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.STALE.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.ABORTED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.COMPLETED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.SUSPENDED.name());

        remover.parseParameters();

    }

    @Test(expected = PatternSyntaxException.class)
    public void parseParametersBadRegexInput() throws Exception {
        remover.payloadPaths = new ArrayList<>();
        remover.payloadPaths.add("/content/dam/.*");
        remover.payloadPaths.add("[");

        remover.olderThanVal = "2019-08-24T00:00:00";

        remover.modelIds = new ArrayList<>();
        remover.modelIds.add("/var/workflow/models/dam/update_asset");
        remover.modelIds.add("/var/workflow/models/dam/process_subasset");
        remover.modelIds.add("/var/workflow/models/request_for_deletion");

        remover.statuses = new ArrayList<>();
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.STALE.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.ABORTED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.COMPLETED.name());
        remover.statuses.add(WorkflowStatusSelector.WorkflowStatus.SUSPENDED.name());

        remover.parseParameters();

    }

    @Test
    public void report() {
        WorkflowRemovalStatus removalStatus = new WorkflowRemovalStatus(ctx.resourceResolver());
        removalStatus.setRunning(false);
        removalStatus.setChecked(357);
        removalStatus.setRemoved(124);
        removalStatus.setCompletedAt(Calendar.getInstance());
        EnumMap<WorkflowRemover.ReportColumns, Object> report = remover.report(removalStatus);
        log.debug(report.toString());

        assertEquals("Value does not match input", 357, report.get(WorkflowRemover.ReportColumns.CHECKED));
        assertEquals("Value does not match input", 124, report.get(WorkflowRemover.ReportColumns.REMOVED));
    }

    @Test
    public void performCleanupActivity() throws Exception {
        WorkflowRemovalStatus statusVal = new WorkflowRemovalStatus(ctx.resourceResolver());
        Mockito.when(workflowInstanceRemover.getStatus()).thenReturn(statusVal);

        remover.performCleanupActivity(actionManager);

        Mockito.verify(workflowInstanceRemover, Mockito.times(1)).removeWorkflowInstances(Mockito.eq(ctx.resourceResolver()),
                Mockito.eq(remover.getModelIds()), Mockito.eq(remover.getStatuses()), Mockito.eq(remover.getPayloads()), Mockito.eq(remover.getOlderThan()),
                Mockito.anyInt(), Mockito.anyInt());

    }
}
