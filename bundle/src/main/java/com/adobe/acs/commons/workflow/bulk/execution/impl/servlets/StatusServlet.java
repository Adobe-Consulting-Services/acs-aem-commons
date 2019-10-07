/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
package com.adobe.acs.commons.workflow.bulk.execution.impl.servlets;

import com.adobe.acs.commons.fam.ActionManager;
import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.fam.impl.ThrottledTaskRunnerStats;
import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowEngine;
import com.adobe.acs.commons.workflow.bulk.execution.model.Status;
import com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl;
import com.adobe.acs.commons.workflow.bulk.execution.model.Config;
import com.adobe.acs.commons.workflow.bulk.execution.model.Failure;
import com.adobe.acs.commons.workflow.bulk.execution.model.Payload;
import com.adobe.acs.commons.workflow.bulk.execution.model.Workspace;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.InstanceNotFoundException;
import javax.management.ReflectionException;
import javax.servlet.ServletException;
import java.io.IOException;
import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * ACS AEM Commons - Bulk Workflow Manager - Status Servlet
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = {"GET"},
        resourceTypes = {BulkWorkflowEngine.SLING_RESOURCE_TYPE},
        selectors = {"status"},
        extensions = {"json"}
)
public class StatusServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(StatusServlet.class);

    private static final int DECIMAL_TO_PERCENT = 100;

    @Reference
    private ThrottledTaskRunnerStats ttrs;

    @Reference
    private ActionManagerFactory actionManagerFactory;

    @Override
    @SuppressWarnings({"squid:S3776", "squid:S1192", "squid:S1872"})
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy hh:mm:ss aaa");

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        Config config = request.getResource().adaptTo(Config.class);
        Workspace workspace = config.getWorkspace();

        final JsonObject json = new JsonObject();

        json.addProperty("initialized", workspace.isInitialized());
        json.addProperty("status", workspace.getStatus().name());

        if (workspace.getSubStatus() != null) {
            json.addProperty("subStatus", workspace.getSubStatus().name());
        }

        json.addProperty("runnerType", config.getRunnerType());
        json.addProperty("queryType", config.getQueryType());
        json.addProperty("queryStatement", config.getQueryStatement());
        json.addProperty("workflowModel", StringUtils.removeEnd(config.getWorkflowModelId(), "/jcr:content/model"));
        json.addProperty("batchSize", config.getBatchSize());
        json.addProperty("autoThrottle", config.isAutoThrottle());

        json.addProperty("purgeWorkflow", config.isPurgeWorkflow());
        json.addProperty("interval", config.getInterval());
        json.addProperty("retryCount", config.getRetryCount());
        json.addProperty("timeout", config.getTimeout());
        json.addProperty("throttle", config.getThrottle());
        json.addProperty("message", workspace.getMessage());

        if (config.isUserEventData()) {
            json.addProperty("userEventData", config.getUserEventData());
        }

        ActionManager actionManager = actionManagerFactory.getActionManager(workspace.getActionManagerName());
        if (actionManager != null && !Status.COMPLETED.equals(workspace.getStatus())) {
            JsonArray failures = new JsonArray();
            json.add("failures", failures);
            // If Complete, then look to JCR for final accounts as ActionManager may be gone
            addActionManagerTrackedCounts(workspace.getActionManagerName(), json);
            for (com.adobe.acs.commons.fam.Failure failure : actionManager.getFailureList()) {
                JsonObject failureJSON = new JsonObject();
                failureJSON.addProperty(Failure.PN_PAYLOAD_PATH, failure.getNodePath());
                failureJSON.addProperty(Failure.PN_FAILED_AT, sdf.format(failure.getTime().getTime()));
                failures.add(failureJSON);
            }
        } else {
            addWorkspaceTrackedCounts(workspace, json);
            JsonArray failures = new JsonArray();
            json.add("failures", failures);
            // Failures
            for (Failure failure : workspace.getFailures()) {
                failures.add(failure.toJSON());
            }
        }

        // Times
        if (workspace.getStartedAt() != null) {
            json.addProperty("startedAt", sdf.format(workspace.getStartedAt().getTime()));
            json.addProperty("timeTakenInMillis", (Calendar.getInstance().getTime().getTime() - workspace.getStartedAt().getTime().getTime()));
        }

        if (workspace.getStoppedAt() != null) {
            json.addProperty("stoppedAt", sdf.format(workspace.getStoppedAt().getTime()));
            json.addProperty("timeTakenInMillis", (workspace.getStoppedAt().getTime().getTime() - workspace.getStartedAt().getTime().getTime()));
        }

        if (workspace.getCompletedAt() != null) {
            json.addProperty("completedAt", sdf.format(workspace.getCompletedAt().getTime()));
            json.addProperty("timeTakenInMillis", (workspace.getCompletedAt().getTime().getTime() - workspace.getStartedAt().getTime().getTime()));
        }

        if (AEMWorkflowRunnerImpl.class.getName().equals(config.getRunnerType())) {
            JsonArray activePayloads = new JsonArray();
            json.add("activePayloads", activePayloads);
            for (Payload payload : config.getWorkspace().getActivePayloads()) {
                activePayloads.add(payload.toJSON());
            }
        }

        json.add("systemStats", getSystemStats());

        Gson gson = new Gson();
        gson.toJson(json, response.getWriter());
    }

    private void addActionManagerTrackedCounts(String name, JsonObject json) {
        final ActionManager actionManager = actionManagerFactory.getActionManager(name);

        int failureCount = actionManager.getErrorCount();
        int completeCount = actionManager.getSuccessCount();
        int totalCount = actionManager.getAddedCount();
        int remainingCount = actionManager.getRemainingCount();

        json.addProperty("totalCount", totalCount);
        json.addProperty("completeCount", completeCount);
        json.addProperty("remainingCount", remainingCount);
        json.addProperty("failCount", failureCount);
        json.addProperty("percentComplete", Math.round(((totalCount - remainingCount) / (totalCount * 1F)) * DECIMAL_TO_PERCENT));
    }

    private void addWorkspaceTrackedCounts(Workspace workspace, JsonObject json) {
        // Counts
        int remainingCount = workspace.getTotalCount() - (workspace.getCompleteCount() + workspace.getFailCount());
        json.addProperty("totalCount", workspace.getTotalCount());
        json.addProperty("completeCount", workspace.getCompleteCount());
        json.addProperty("remainingCount", remainingCount);
        json.addProperty("failCount", workspace.getFailCount());
        json.addProperty("percentComplete", Math.round(((workspace.getTotalCount() - remainingCount) / (workspace.getTotalCount() * 1F)) * DECIMAL_TO_PERCENT));
    }

    @SuppressWarnings("squid:S1192")
    private JsonObject getSystemStats() {
        JsonObject json = new JsonObject();
        try {
            json.addProperty("cpu", MessageFormat.format("{0,number,#%}", ttrs.getCpuLevel()));
        } catch (InstanceNotFoundException e) {
            log.error("Could not collect CPU stats", e);
            json.addProperty("cpu", -1);
        } catch (ReflectionException e) {
            log.error("Could not collect CPU stats", e);
            json.addProperty("cpu", -1);
        }
        json.addProperty("mem", MessageFormat.format("{0,number,#%}", ttrs.getMemoryUsage()));
        json.addProperty("maxCpu", MessageFormat.format("{0,number,#%}", ttrs.getMaxCpu()));
        json.addProperty("maxMem", MessageFormat.format("{0,number,#%}", ttrs.getMaxHeap()));
        return json;
    }
}
