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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.EnumMap;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.ResourceResolver;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.DatePickerComponent;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.MultifieldComponent;
import com.adobe.acs.commons.mcp.model.GenericReport;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowInstanceRemover;
import com.adobe.acs.commons.workflow.bulk.removal.WorkflowRemovalStatus;

/**
 * Removes workflow process instances based on specified conditions.
 */
public class WorkflowRemover extends ProcessDefinition {

    private static final int BATCH_SIZE = 1000;
    // don't constrain the runtime
    private static final int MAX_DURATION_MINS = 0;

    private final WorkflowInstanceRemover workflowInstanceRemover;

    private final transient GenericReport report = new GenericReport();
    private final transient List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

    @FormField(name = "Workflow Payload Paths", description = "Payload path regex", hint = "/content/dam/.*",
            component = MultifieldComponent.class)
    public List<String> payloadPaths;

    @FormField(name = "Workflows Older Than", description = "only remove workflows older than the specified date",
            component = DatePickerComponent.class)
    public String olderThanVal;

    @FormField(
            name = "Workflow Models",
            description = "If no Workflow Models are selected, Workflow Instances will not be filtered by Workflow Model.",
            component = MultifieldComponent.class, options = { MultifieldComponent.USE_CLASS
                    + "=com.adobe.acs.commons.mcp.form.workflow.WorkflowModelSelector" })
    public List<String> modelIds = new ArrayList<>();

    private List<Pattern> payloads = new ArrayList<>();
    private Calendar olderThan;

    @FormField(name = "Workflow Statuses", component = MultifieldComponent.class, required = true,
            options = { MultifieldComponent.USE_CLASS
                    + "=com.adobe.acs.commons.mcp.form.workflow.WorkflowStatusSelector" })
    public List<String> statuses = new ArrayList<>();

    public WorkflowRemover(WorkflowInstanceRemover workflowInstanceRemover) {
        super();
        this.workflowInstanceRemover = workflowInstanceRemover;
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException, RepositoryException {
        instance.defineCriticalAction("Seek and Destroy Workflows", rr, this::performCleanupActivity);

        // TODO I'd eventually like to refactor this as follows, but that requires some significant change to the
        // underlying service, which I'm not ready to take on
        // criticalAction - find workflows to remove
        // if !dryRun
        // action - remove workflows
        // action - remove empty folders
        // end if
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver rr) throws RepositoryException,
            PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(rr, instance.getPath() + "/jcr:content/report");
    }

    @Override
    public void init() throws RepositoryException {
        // No init needed, do nothing.
    }

    /**
     * Cleanup the old workflows.  Package scoped for unit test purposes.
     * @param manager the action manager to handle removal.
     * @throws Exception if an error occurs
     */
    void performCleanupActivity(ActionManager manager) throws Exception {
        manager.withResolver(rr -> {

            parseParameters();

            workflowInstanceRemover.removeWorkflowInstances(rr, modelIds, statuses, payloads, olderThan, BATCH_SIZE,
                    MAX_DURATION_MINS);

            WorkflowRemovalStatus status = workflowInstanceRemover.getStatus();
            EnumMap<ReportColumns, Object> reportRow = report(status);
            reportRows.add(reportRow);
        });
    }

    /**
     * Collect and return a report row for the workflow status.  Method is package scope for unit tests.
     * @param status the status to report upon.
     * @return the row of data
     */
    EnumMap<ReportColumns, Object> report(WorkflowRemovalStatus status) {
        final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

        row.put(ReportColumns.STARTED, status.getStartedAt());
        row.put(ReportColumns.CHECKED, status.getChecked());
        row.put(ReportColumns.REMOVED, status.getRemoved());

        row.put(ReportColumns.COMPLETED, status.getCompletedAt());
        row.put(ReportColumns.ERRED, status.getErredAt());
        row.put(ReportColumns.INITIATED_BY, status.getInitiatedBy());

        return row;
    }

    /**
     * Parse the input parameters into the form needed to call workflowInstanceRemover. The results are set into
     * instance variables.  Method is package scope for unit testing.
     *
     * @throws ParseException
     *             if the date is in an invalid format.
     * @throws PatternSyntaxException
     *          if the payloads contain illegal patterns
     */
    void parseParameters() throws ParseException {
        if (payloadPaths != null) {
            payloads = payloadPaths.stream().map(Pattern::compile).collect(Collectors.toList());
        }

        if (StringUtils.isNotEmpty(olderThanVal)) {
            final SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
            Date d = sdf.parse(olderThanVal);
            olderThan = Calendar.getInstance();
            olderThan.setTime(d);
        }
    }

    public List<String> getModelIds() {
        return modelIds;
    }

    public List<Pattern> getPayloads() {
        return payloads;
    }

    public Calendar getOlderThan() {
        return olderThan;
    }

    public List<String> getStatuses() {
        return statuses;
    }

    public enum ReportColumns {
        STARTED, CHECKED, REMOVED, COMPLETED, ERRED, INITIATED_BY
    }

}
