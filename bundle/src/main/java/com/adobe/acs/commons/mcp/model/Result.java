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
package com.adobe.acs.commons.mcp.model;

import aQute.bnd.annotation.ProviderType;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * This bean captures the commonly-collected report summary details from a controlled process
 */
@ProviderType
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class Result {
    @Inject
    private int tasksCompleted;
    @Inject
    private Long runtime;
    @Inject
    private Resource report;

    /**
     * @return the runtime
     */
    public Long getRuntime() {
        return runtime;
    }

    /**
     * @param runtime the runtime to set
     */
    public void setRuntime(Long runtime) {
        this.runtime = runtime;
    }

    /**
     * @return the report
     */
    public Resource getReport() {
        return report;
    }

    /**
     * @param report the report to set
     */
    public void setReport(Resource report) {
        this.report = report;
    }

    /**
     * @return the tasksCompleted
     */
    public int getTasksCompleted() {
        return tasksCompleted;
    }

    /**
     * @param tasksCompleted the tasksCompleted to set
     */
    public void setTasksCompleted(int tasksCompleted) {
        this.tasksCompleted = tasksCompleted;
    }
}
