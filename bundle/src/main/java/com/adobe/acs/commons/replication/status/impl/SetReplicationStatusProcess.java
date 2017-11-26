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

package com.adobe.acs.commons.replication.status.impl;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replication.status.ReplicationStatusManager;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.WorkItem;
import com.day.cq.workflow.exec.WorkflowProcess;
import com.day.cq.workflow.metadata.MetaDataMap;

@Component
@Property(
        label = "Workflow Label",
        name = "process.label",
        value = "Set Replication Status",
        description = "Sets the cq:lastReplicated, cq:lastReplicateBy and cq:lastReplicatedAction on the payload to the values provided"
)
@Service
public class SetReplicationStatusProcess implements WorkflowProcess {

    private static final Logger log = LoggerFactory.getLogger(SetReplicationStatusProcess.class);

    private static final String ARG_REPL_DATE = "replicationDate";
    private static final String ARG_REPL_BY = "replicatedBy";
    private static final String ARG_REPL_ACTION = "replicationAction";

    @Reference
    private WorkflowHelper workflowHelper;

    @Reference
    private ReplicationStatusManager replStatusMgr;


    @Override
    public void execute(WorkItem workItem, WorkflowSession workflowSession, MetaDataMap metadataMap) throws WorkflowException {

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = workflowHelper.getResourceResolver(workflowSession);
            final String replicatedResourcePath = getReplicatedResourcePath(workItem, resourceResolver);

            Map<String, String> params = extractWorkflowParams(metadataMap);

            String replAction;
            if (!params.containsKey(ARG_REPL_ACTION)) {
                log.warn("Please add a replicationAction to your process arguments (ACTIVATED, DEACTIVATED or CLEAR).  Will now exit without processing.");
                return;
            } else {
                replAction = params.get(ARG_REPL_ACTION);
            }

            Calendar replicatedAt;
            if (!params.containsKey(ARG_REPL_DATE)) {
                log.info("No replicationDate argument specified, will default to current time.");
                replicatedAt = Calendar.getInstance();
            } else {
                replicatedAt = getReplicationDate(params);
            }

            String replicatedBy;
            if (!params.containsKey(ARG_REPL_BY)) {
                log.info("No replicatedBy argument specified, will default to 'migration'.");
                replicatedBy = "migration";
            } else {
                replicatedBy = params.get(ARG_REPL_BY);
            }

            replStatusMgr.setReplicationStatus(resourceResolver, replicatedBy, replicatedAt, ReplicationStatusManager.Status.valueOf(replAction), replicatedResourcePath);
        } catch (Exception e) {
            log.error("An exception occurred while setting replication status.", e);
        }
    }

    private String getReplicatedResourcePath(WorkItem workItem, ResourceResolver resourceResolver) {
        String payloadPath = workItem.getWorkflowData().getPayload().toString();
        Resource resource = replStatusMgr.getReplicationStatusResource(payloadPath, resourceResolver);
        return resource.getPath();
    }

    private Map<String, String> extractWorkflowParams(MetaDataMap metadataMap) {
        String[] lines = StringUtils.split(metadataMap.get(WorkflowHelper.PROCESS_ARGS, ""), System.lineSeparator());
        Map<String, String> params = ParameterUtil.toMap(lines, "=");
        return params;
    }

    private Calendar getReplicationDate(Map<String, String> params) throws ParseException {
        Calendar replicatedAt = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm", Locale.ENGLISH);
        replicatedAt.setTime(sdf.parse(params.get(ARG_REPL_DATE)));
        return replicatedAt;
    }

}
