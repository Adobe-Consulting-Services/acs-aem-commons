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

import com.adobe.acs.commons.workflow.bulk.execution.BulkWorkflowRunner;
import com.day.cq.commons.jcr.JcrUtil;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.EnumUtils;
import org.apache.jackrabbit.commons.JcrUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

@Model(adaptables = Resource.class)
public class Workspace {

    public static final String NT_UNORDERED = "oak:Unstructured";

    public static final String NN_FAILURES = "failures";
    public static final String NN_FAILURE = "failure";
    public static final String NN_WORKSPACE = "workspace";
    public static final String NN_PAYLOADS = "payloads";
    public static final String PN_ACTIVE_PAYLOAD_GROUPS = "activePayloadGroups";
    public static final String PN_ACTIVE_PAYLOADS = "activePayloads";
    public static final String PN_STATUS = "status";
    public static final String PN_SUB_STATUS = "subStatus";
    private static final String PN_INITIALIZED = "initialized";
    private static final String PN_COMPLETED_AT = "completedAt";
    private static final String PN_COUNT_COMPLETE = "completeCount";
    private static final String PN_COUNT_FAILURE = "failCount";
    private static final String PN_COUNT_TOTAL = "totalCount";
    private static final String PN_STARTED_AT = "startedAt";
    private static final String PN_STOPPED_AT = "stoppedAt";
    private static final String PN_MESSAGE = "message";
    private static final String PN_ACTION_MANAGER_NAME = "actionManagerName";
    public static final String NN_PAYLOADS_LAUNCHPAD = "payloads_zero";

    private Resource resource;

    private ModifiableValueMap properties;

    private BulkWorkflowRunner runner;

    private Config config;

    @Inject
    private List<BulkWorkflowRunner> runners;

    @Inject
    @Optional
    private String jobName;

    @Inject
    private ResourceResolver resourceResolver;

    @Inject
    @Default(values = {})
    private String[] activePayloads;

    @Inject
    @Default(values = {})
    private String[] activePayloadGroups;

    @Inject
    @Default(values = "NOT_STARTED")
    private String status;

    @Inject
    @Optional
    private String subStatus;

    @Inject
    @Default(booleanValues = false)
    private boolean initialized;

    @Inject
    @Default(intValues = 0)
    private int totalCount;

    @Inject
    @Default(intValues = 0)
    private int completeCount;

    @Inject
    @Default(intValues = 0)
    private int failCount;

    @Inject
    @Optional
    private Calendar startedAt;

    @Inject
    @Optional
    private Calendar stoppedAt;

    @Inject
    @Optional
    private Calendar completedAt;

    @Inject
    @Optional
    private String message;

    @Inject
    private List<Failure> failures;

    @Inject
    @Optional
    private String actionManagerName;

    public Workspace(Resource resource) {
        this.resource = resource;
        this.properties = resource.adaptTo(ModifiableValueMap.class);
        this.jobName = "acs-commons@bulk-workflow-execution:/" + this.resource.getPath();
    }

    @PostConstruct
    protected void activate() {
        this.config = resource.getParent().adaptTo(Config.class);

        for (BulkWorkflowRunner candidate : runners) {
            if (StringUtils.equals(this.config.getRunnerType(), candidate.getClass().getName())) {
                runner = candidate;
                break;
            }
        }
    }

    /** Getters **/

    public Calendar getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Calendar completedAt) {
        this.completedAt = completedAt;
        properties.put(PN_COMPLETED_AT, this.completedAt);
    }

    public int getCompleteCount() {
        return completeCount;
    }

    public int getFailCount() {
        return failCount;
    }

    public String getJobName() {
        return jobName;
    }

    public int getTotalCount() {
        return totalCount;
    }

    public void setTotalCount(int totalCount) {
        this.totalCount = totalCount;
        properties.put(PN_COUNT_TOTAL, this.totalCount);
    }

    public BulkWorkflowRunner getRunner() {
        return runner;
    }

    public Calendar getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Calendar startedAt) {
        this.startedAt = startedAt;
        properties.put(PN_STARTED_AT, this.startedAt);
    }

    public Status getStatus() {
        // Refresh state before getting the status.
        // Note, this gets the value from the session state, and not the cached Sling Model value as this value can change over the life of the SlingModel.
        resourceResolver.refresh();
        status = resource.getValueMap().get(PN_STATUS, Status.NOT_STARTED.toString());

        return EnumUtils.getEnum(Status.class, status);
    }

    public Calendar getStoppedAt() {
        return stoppedAt;
    }

    public boolean isInitialized() {
        return initialized;
    }


    public boolean isRunning() {
        return Status.RUNNING.equals(getStatus());
    }

    public boolean isStopped() {
        return Status.STOPPED.equals(getStatus());
    }

    public boolean isStopping() {
        return Status.RUNNING.equals(getStatus()) && SubStatus.STOPPING.equals(getSubStatus());
    }

    public Config getConfig() {
        return config;
    }

    public ResourceResolver getResourceResolver() {
        return resourceResolver;
    }

    public boolean isActive(PayloadGroup payloadGroup) {
        return ArrayUtils.contains(activePayloadGroups, payloadGroup.getDereferencedPath());
    }

    public String getPath() {
        return resource.getPath();
    }

    public String getMessage() {
        return message;
    }

    public String getActionManagerName() {
        return actionManagerName;
    }

    public List<Failure> getFailures() {
        return Collections.unmodifiableList(failures);
    }


    /** Setters **/

    public void setStatus(Status status) {
        this.status = status.toString();
        properties.put(PN_STATUS, this.status);
        // Clear subStatus
        subStatus = null;
        properties.remove(PN_SUB_STATUS);
    }

    public void setStatus(Status status, SubStatus subStatus) {
        setStatus(status);
        if (subStatus != null) {
            this.subStatus = subStatus.toString();
            properties.put(PN_SUB_STATUS, this.subStatus);
        }
    }

    public SubStatus getSubStatus() {
        // Refresh state before getting the status.
        // Note, this gets the value from the session state, and not the cached Sling Model value as this value can change over the life of the SlingModel.
        resourceResolver.refresh();
        subStatus = resource.getValueMap().get(PN_SUB_STATUS, String.class);

        if (subStatus != null) {
            return EnumUtils.getEnum(SubStatus.class, subStatus);
        } else {
            return null;
        }
    }

    public void setStoppedAt(Calendar stoppedAt) {
        this.stoppedAt = stoppedAt;
        properties.put(PN_STOPPED_AT, this.stoppedAt);
    }


    public void setInitialized(boolean initialized) {
        this.initialized = initialized;
        properties.put(PN_INITIALIZED, this.initialized);
    }

    public int incrementCompleteCount() {
        setCompleteCount(completeCount + 1);
        return completeCount;
    }

    public int setCompleteCount(int count) {
        completeCount = count;
        properties.put(PN_COUNT_COMPLETE, count);
        return completeCount;
    }

    public int incrementFailCount() {
        setFailureCount(failCount + 1);
        return failCount;
    }

    public int setFailureCount(int count) {
        failCount = count;
        properties.put(PN_COUNT_FAILURE, count);
        return failCount;
    }

    public void setError(String message) {
        setStatus(Status.STOPPED, SubStatus.ERROR);
        setMessage(message);
    }

    public void setMessage(String message) {
        this.message = message;
        properties.put(PN_MESSAGE, message);
    }

    public void setActionManagerName(String name) {
        this.actionManagerName = name;
        properties.put(PN_ACTION_MANAGER_NAME, name);
    }

    /** Internal logic proxies **/

    public void addActivePayload(Payload payload) {
        if (!ArrayUtils.contains(activePayloads, payload.getDereferencedPath())) {
            activePayloads = (String[]) ArrayUtils.add(activePayloads, payload.getDereferencedPath());
            properties.put(PN_ACTIVE_PAYLOADS, activePayloads);

            addActivePayloadGroup(payload.getPayloadGroup());
        }
    }

    public void addActivePayloads(List<Payload> payloads) {
        for (Payload payload : payloads) {
            addActivePayload(payload);
        }
    }

    public void removeActivePayload(Payload payload) {
        if (ArrayUtils.contains(activePayloads, payload.getDereferencedPath())) {
            activePayloads = (String[]) ArrayUtils.removeElement(activePayloads, payload.getDereferencedPath());
            properties.put(PN_ACTIVE_PAYLOADS, activePayloads);
        }
    }

    /**
     * @return a list of the payloads that are being actively processed by bulk workflow manager.
     */
    public List<Payload> getActivePayloads() {
        List<Payload> payloads = new ArrayList<Payload>();

        for (String path : activePayloads) {
            Resource r = resourceResolver.getResource(Payload.reference(path));
            if (r != null) {
                Payload p = r.adaptTo(Payload.class);
                if (p != null) {
                    payloads.add(p);
                }
            }
        }

        return payloads;
    }

    /**
     * @return a list of the payload groups that have atleast 1 payload being process by bulk workflow manager.
     */
    public List<PayloadGroup> getActivePayloadGroups() {
        List<PayloadGroup> payloadGroups = new ArrayList<PayloadGroup>();

        if (activePayloadGroups != null) {
            for (String path : activePayloadGroups) {
                Resource r = resourceResolver.getResource(PayloadGroup.reference(path));
                if (r == null) {
                    continue;
                }
                PayloadGroup pg = r.adaptTo(PayloadGroup.class);
                if (pg == null) {
                    continue;
                }
                payloadGroups.add(pg);
            }
        }

        return payloadGroups;
    }

    /**
     * Adds the payload group to the list of active payload groups.
     *
     * @param payloadGroup the payload group to add as active
     */
    public void addActivePayloadGroup(PayloadGroup payloadGroup) {
        if (payloadGroup != null && !ArrayUtils.contains(activePayloadGroups, payloadGroup.getDereferencedPath())) {
            activePayloadGroups = (String[]) ArrayUtils.add(activePayloadGroups, payloadGroup.getDereferencedPath());
            properties.put(PN_ACTIVE_PAYLOAD_GROUPS, activePayloadGroups);
        }
    }

    /**
     * Removes the payload group from the list of active payload groups.
     *
     * @param payloadGroup the payload group to remove from the active list.
     */
    public void removeActivePayloadGroup(PayloadGroup payloadGroup) {
        if (payloadGroup != null && ArrayUtils.contains(activePayloadGroups, payloadGroup.getDereferencedPath())) {
            activePayloadGroups = (String[]) ArrayUtils.removeElement(activePayloadGroups, payloadGroup.getDereferencedPath());
            properties.put(PN_ACTIVE_PAYLOAD_GROUPS, activePayloadGroups);
        }
    }

    public void addFailure(Payload payload) throws RepositoryException {
        addFailure(payload.getDereferencedPayloadPath(), payload.getDereferencedPath(), Calendar.getInstance());
    }

    public void addFailure(String payloadPath, String trackPath, Calendar failedAt) throws RepositoryException {
        Node failure = JcrUtils.getOrCreateByPath(resource.getChild(Workspace.NN_FAILURES).adaptTo(Node.class),
                Workspace.NN_FAILURE, true, Workspace.NT_UNORDERED, Workspace.NT_UNORDERED, false);

        JcrUtil.setProperty(failure, Failure.PN_PAYLOAD_PATH, payloadPath);

        if (StringUtils.isNotBlank(trackPath)) {
            JcrUtil.setProperty(failure, Failure.PN_PATH, Payload.dereference(trackPath));
        }

        if (failedAt != null) {
            JcrUtil.setProperty(failure, Failure.PN_FAILED_AT, failedAt);
        }
    }


    /**
     * Commit the changes for this bulk workflow manager execution.
     *
     * @throws PersistenceException
     */
    public void commit() throws PersistenceException {
        config.commit();
    }


}