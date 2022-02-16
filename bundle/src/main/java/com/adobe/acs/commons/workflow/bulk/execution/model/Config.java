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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Default;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.Optional;

import javax.inject.Inject;

@Model(adaptables = Resource.class)
public class Config {

    private final Resource resource;
    private final ModifiableValueMap properties;

    private Workspace workspace;

    @Inject
    @Default(values = "com.adobe.acs.commons.workflow.bulk.execution.impl.runners.AEMWorkflowRunnerImpl")
    private String runnerType;

    @Inject
    @Optional
    private String queryStatement;

    @Inject
    @Default(values = "queryBuilder")
    private String queryType;

    @Inject
    @Default(intValues = 10)
    private int timeout;

    @Inject
    @Default(booleanValues = false)
    private boolean purgeWorkflow;

    @Inject
    @Default(intValues = 0)
    private int batchSize;

    @Inject
    @Default(intValues = 10)
    private int interval;

    @Inject
    @Default(intValues = 10)
    private int throttle;

    @Inject
    @Optional
    private String relativePath;

    @Inject
    @Optional
    private String workflowModel;

    @Inject
    @Default(intValues = 10)
    private int retryCount;

    @Inject
    @Default(booleanValues = true)
    private boolean autoThrottle;

    @Inject
    @Optional
    private String userEventData;

    public Config(Resource resource) {
        this.resource = resource;
        this.properties = resource.adaptTo(ModifiableValueMap.class);
    }

    public int getTimeout() {
        return timeout;
    }

    public int getThrottle() {
        return throttle;
    }


    public int getBatchSize() {
        return batchSize;
    }

    public int getInterval() {
        return interval;
    }

    public String getRelativePath() {
        return relativePath;
    }

    public String getRunnerType() {
        return runnerType;
    }

    public String getWorkflowModelId() {
        return workflowModel;
    }

    public String getPath() {
        return resource.getPath();
    }

    public ResourceResolver getResourceResolver() {
        return this.resource.getResourceResolver();
    }

    public String getQueryStatement() {
        return queryStatement;
    }

    public String getQueryType() {
        return queryType;
    }

    public Resource getResource() {
        return resource;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public boolean isAutoThrottle() {
        return autoThrottle;
    }

    public String getUserEventData() {
        return userEventData;
    }

    public boolean isUserEventData() {
        return StringUtils.isNotBlank(userEventData);
    }

    public Workspace getWorkspace() {
        // Collecting workspace on get to avoid cyclic recursion between models
        if (this.workspace == null) {
            this.workspace = this.resource.getChild(Workspace.NN_WORKSPACE).adaptTo(Workspace.class);
        }

        return this.workspace;
    }

    public boolean isPurgeWorkflow() {
        return purgeWorkflow;
    }

    /** Setters **/

    public void setInterval(int interval) {
        if (interval < 1) {
            interval = 0;
        }

        this.interval = interval;
        properties.put("interval", this.interval);
    }

    public void setThrottle(int throttle) {
        if (throttle < 1) {
            throttle = 0;
        }

        this.throttle = throttle;
        properties.put("throttle", this.throttle);
    }

    /** Commit **/

    public void commit() throws PersistenceException {
        if (this.getResourceResolver().hasChanges()) {
            this.getResourceResolver().commit();
        }
    }

}



