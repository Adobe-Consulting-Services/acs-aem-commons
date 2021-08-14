/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2019 Adobe
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

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessInstance;
import com.adobe.acs.commons.mcp.form.FormField;
import com.adobe.acs.commons.mcp.form.SelectComponent;
import com.adobe.acs.commons.mcp.form.TextareaComponent;
import com.adobe.acs.commons.mcp.form.workflow.WorkflowModelSelector;
import com.adobe.acs.commons.mcp.model.GenericBlobReport;
import com.adobe.acs.commons.mcp.util.StringUtil;
import com.adobe.acs.commons.util.QueryHelper;
import com.adobe.acs.commons.util.impl.QueryHelperImpl;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowModel;
import com.adobe.acs.commons.workflow.synthetic.SyntheticWorkflowRunner;
import com.day.cq.workflow.WorkflowException;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

public class BulkWorkflow extends ProcessDefinition implements Serializable {
    private static final Logger log = LoggerFactory.getLogger(BulkWorkflow.class);

    public static final String PROCESS_NAME = "Bulk Workflow";

    private final transient QueryHelper queryHelper;
    private final transient SyntheticWorkflowRunner syntheticWorkflowRunner;

    public enum ItemStatus {
        SUCCESS, FAILURE
    }

    
    public enum QueryLanguage {
        QUERY_BUILDER(QueryHelperImpl.QUERY_BUILDER),
        LIST(QueryHelperImpl.LIST),
        @SuppressWarnings("deprecation")
        XPATH(Query.XPATH),
        JCR_SQL2(Query.JCR_SQL2),
        JCR_SQL("JCR-SQL");

        private String value;

        QueryLanguage(String value) {
            this.value = value;
        }

        public String getValue() {
            return this.value;
        }
    }

    public enum ReportColumns {
        PAYLOAD_PATH, TIME_TAKEN_IN_MILLISECONDS, STATUS
    }

    @FormField(
            name = "Workflow model",
            description = "The workflow model to execute. This workflow model MUST be compatible with ACS AEM Commons Synthetic Workflow.",
            component = WorkflowModelSelector.class,
            options = {"required"}
    )
    public String workflowId = "";

    @FormField(
            name = "Query language",
            description = "",
            component = SelectComponent.EnumerationSelector.class,
            options = {"default=QUERY_BUILDER", "required"}
    )
    public QueryLanguage queryLanguage = QueryLanguage.QUERY_BUILDER;

    @FormField(
            name = "Query statement",
            description = "Ensure that this query is correct prior to submitting form as it will collect the resources for processing which can be an expensive operation for large bulk workflow processes.",
            component = TextareaComponent.class,
            options = {"required"}
    )
    public String queryStatement = "";

    @FormField(
            name = "Relative path",
            description = "This can be used to select otherwise difficult to search for resources. Examples: jcr:content/renditions/original OR ../renditions/original"
    )
    public String relativePayloadPath = "";

    private final transient GenericBlobReport report = new GenericBlobReport();
    private final transient List<EnumMap<ReportColumns, Object>> reportRows = new ArrayList<>();

    private transient List<Resource> payloads;
    private transient SyntheticWorkflowModel syntheticWorkflowModel;

    public BulkWorkflow(final QueryHelper queryHelper,
                        final SyntheticWorkflowRunner syntheticWorkflowRunner) {
        this.queryHelper = queryHelper;
        this.syntheticWorkflowRunner = syntheticWorkflowRunner;
    }

    @Override
    public void buildProcess(ProcessInstance instance, ResourceResolver rr) throws LoginException {
        report.setName(instance.getName());
        instance.getInfo().setDescription("Bulk process payloads using synthetic workflow");

        instance.defineCriticalAction("Process payloads with synthetic workflow", rr, this::processPayloads);
    }

    protected void queryPayloads(ActionManager manager) throws Exception {
        manager.withResolver(resourceResolver -> {
            payloads = queryHelper.findResources(resourceResolver, queryLanguage.getValue(), queryStatement, relativePayloadPath);
        });
    }

    protected void prepareSyntheticWorkflowModel(ActionManager manager) throws Exception {
        manager.withResolver(resourceResolver -> {
            syntheticWorkflowModel = syntheticWorkflowRunner.getSyntheticWorkflowModel(
                    resourceResolver,
                    workflowId,
                    true);
        });
    }

    public void processPayloads(ActionManager manager) throws Exception {
        prepareSyntheticWorkflowModel(manager);
        queryPayloads(manager);

        log.info("Executing synthetic workflow [ {} ] against [ {} ] payloads via Bulk Workflow MCP process.", workflowId, payloads.size());

        payloads.stream()
                .map((resource) -> resource.getPath())
                .forEach((path) -> manager.deferredWithResolver((ResourceResolver resourceResolver) -> {
                    final long start = System.currentTimeMillis();

                    resourceResolver.adaptTo(Session.class).getWorkspace().getObservationManager().setUserData("changedByWorkflowProcess");

                    try {
                        syntheticWorkflowRunner.execute(resourceResolver, path, syntheticWorkflowModel, false, true);
                        final long duration = System.currentTimeMillis() - start;
                        record(path, ItemStatus.SUCCESS, duration);
                        log.debug("Successfully processed payload [ {} ] with synthetic workflow [ {} ] in [ {} ] milliseconds.", path, workflowId, duration);
                    } catch (WorkflowException e) {
                        final long duration = System.currentTimeMillis() - start;
                        record(path, ItemStatus.FAILURE, duration);
                        log.warn("Failed to process payload [ {} ] with synthetic workflow [ {} ] in [ {} ] milliseconds.", path, workflowId, duration);
                    }
                }));
    }

    public GenericBlobReport getReport() {
        return report;
    }

    @Override
    public void init() throws RepositoryException {
        // nothing to do here
    }

    protected void record(String path, ItemStatus status, long timeTaken) {
        final EnumMap<ReportColumns, Object> row = new EnumMap<>(ReportColumns.class);

        row.put(ReportColumns.PAYLOAD_PATH, path);
        row.put(ReportColumns.STATUS, StringUtil.getFriendlyName(status.name()));
        row.put(ReportColumns.TIME_TAKEN_IN_MILLISECONDS, timeTaken);

        reportRows.add(row);
    }

    @Override
    public void storeReport(ProcessInstance instance, ResourceResolver resourceResolver)
            throws RepositoryException, PersistenceException {
        report.setRows(reportRows, ReportColumns.class);
        report.persist(resourceResolver, instance.getPath() + "/jcr:content/report");
    }
}