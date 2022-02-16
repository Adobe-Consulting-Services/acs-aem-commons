/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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

package com.adobe.acs.commons.workflow.bulk.execution.model;

import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.WorkflowService;
import com.day.cq.workflow.WorkflowSession;
import com.day.cq.workflow.exec.Workflow;
import com.google.gson.JsonObject;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Session;

@Model(adaptables = Resource.class)
public class Payload {
    private static final Logger log = LoggerFactory.getLogger(Payload.class);

    private static final String PN_WORKFLOW_INSTANCE_ID = "workflowInstanceId";
    private static final String PN_STATUS = "status";
    private static final String PN_PATH = "path";
    public static final String NN_PAYLOAD = "payload";

    private ModifiableValueMap properties;

    private Resource resource;

    @Inject
    private WorkflowService workflowService;

    @Inject
    @Default(values = "NOT_STARTED")
    private String status;

    @Inject
    private String path;

    @Inject
    @Optional
    private String workflowInstanceId;

    public Payload(Resource resource) {
        this.resource = resource;
    }

    @PostConstruct
    public void activate() {
        properties = resource.adaptTo(ModifiableValueMap.class);
    }

    public ResourceResolver getResourceResolver() {
        return resource.getResourceResolver();
    }

    public Status getStatus() {
        return EnumUtils.getEnum(Status.class, status);
    }

    public String getPayloadPath() {
        return reference(path);
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getDereferencedPayloadPath() {
        return path;
    }

    public String getDereferencedPath() {
        return dereference(resource.getPath());
    }

    public PayloadGroup getPayloadGroup() {
        return resource.getParent().adaptTo(PayloadGroup.class);
    }

    public Workflow getWorkflow() throws WorkflowException {
        final WorkflowSession workflowSession =
                workflowService.getWorkflowSession(resource.getResourceResolver().adaptTo(Session.class));
        String tmp = getWorkflowInstanceId();

        try {
            if (resource.getResourceResolver().getResource(tmp) != null){
                return workflowSession.getWorkflow(tmp);
            }
        } catch (Exception e) {
            log.error(String.format("Could not get workflow with id [ %s ] for payload [ %s ~> %s ]", tmp, getPath(), getPayloadPath()), e);
        }

        return null;
    }

    public String getWorkflowInstanceId() {
        if (StringUtils.isBlank(workflowInstanceId)) {
            resource.getResourceResolver().refresh();
            workflowInstanceId = properties.get(PN_WORKFLOW_INSTANCE_ID, String.class);
        }

        return reference(workflowInstanceId);
    }

    public boolean isOnboarded() {
        Status tmpStatus = getStatus();
        return (tmpStatus != null && !Status.NOT_STARTED.equals(tmpStatus));
    }

    /** Setters **/

    public void setStatus(Status newStatus) {
        this.status = newStatus.toString();
        properties.put(PN_STATUS, this.status);
    }

    public void updateWith(Workflow workflow) throws PersistenceException {

        if (StringUtils.isBlank(getWorkflowInstanceId())) {
            workflowInstanceId = workflow.getId();
            properties.put(PN_WORKFLOW_INSTANCE_ID, dereference(workflowInstanceId));
        } else if (!StringUtils.equals(getWorkflowInstanceId(), workflow.getId())) {
            throw new PersistenceException("Batch Entry workflow instance does not match. [ " + workflowInstanceId + " ] vs [ " + workflow.getId() + " ]");
        }

        if (!StringUtils.equals(status, workflow.getState())) {
            // Status is different, so update
            setStatus(EnumUtils.getEnum(Status.class, workflow.getState()));
        }
    }

    /** Renditions **/

    public JsonObject toJSON() {
        JsonObject json = new JsonObject();
        json.addProperty(PN_STATUS, getStatus().toString());
        json.addProperty(PN_PATH, getPayloadPath());
        return json;
    }

    public static String dereference(String str) {
        if (!StringUtils.startsWith(str, "-")) {
            str = "-" + str;
        }

        return str;
    }

    public static String reference(String str) {
        return StringUtils.removeStart(str, "-");
    }

}
