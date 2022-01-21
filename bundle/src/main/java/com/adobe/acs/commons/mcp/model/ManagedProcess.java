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

import org.osgi.annotation.versioning.ProviderType;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

/**
 * Model bean for process instances.
 */
@ProviderType
@Model(adaptables = Resource.class, defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class ManagedProcess implements Serializable {
    private static final long serialVersionUID = 7526472295622776156L;

    @Inject
    private String requester;
    @Inject
    private ValueMap requestInputs;
    @Inject
    private Long startTime;
    @Inject
    private Long stopTime;
    @Inject
    private String name;
    @Inject
    private String description;
    @Inject
    private boolean isRunning;
    @Inject
    private double progress;
    @Inject
    private String status;
    @Inject
    private Result result;
    @Inject
    private int reportedErrors = 0;
    
    private transient Collection<ArchivedProcessFailure> reportedErrorsList;

    @Inject
    private transient Resource resource;
        
    /**
     * @return the reportedErrors
     */
    public int getReportedErrors() {
        return reportedErrors;
    }
    
    /**
     * @return the reportedErrorsList
     */
    public Collection<ArchivedProcessFailure> getReportedErrorsList() {
        return Optional.ofNullable(reportedErrorsList)
                .map(Collections::unmodifiableCollection)
                .orElse(Collections.emptyList());
    }

    /**
     * @param reportedErrors the reportedErrors to set
     */
    public void setReportedErrors(List<ArchivedProcessFailure> reportedErrors) {
        this.reportedErrorsList = Collections.unmodifiableList(reportedErrors);
        this.reportedErrors = reportedErrorsList.size();
    }

    /**
     * @return the requester
     */
    public String getRequester() {
        return requester;
    }

    /**
     * @param requester the requester to set
     */
    public void setRequester(String requester) {
        this.requester = requester;
    }

    /**
     * @return the requestInputs
     */
    public ValueMap getRequestInputs() {
        return requestInputs;
    }

    /**
     * @param requestInputs the requestInputs to set
     */
    public void setRequestInputs(ValueMap requestInputs) {
        this.requestInputs = requestInputs;
    }

    /**
     * @return the startTime
     */
    public Long getStartTime() {
        return startTime;
    }
    
    public String getStartTimeFormatted() {
        return startTime != null ? formatDate(startTime) : "";
    }

    /**
     * @param startTime the startTime to set
     */
    public void setStartTime(Long startTime) {
        this.startTime = startTime;
    }

    /**
     * @return the stopTime
     */
    public Long getStopTime() {
        return stopTime;
    }

    public String getStopTimeFormatted() {
        return stopTime != null ? formatDate(stopTime) : "";
    }
    
    /**
     * @param stopTime the stopTime to set
     */
    public void setStopTime(Long stopTime) {
        this.stopTime = stopTime;
    }

    /**
     * @return the description
     */
    public String getDescription() {
        return description;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return the isRunning
     */
    public boolean isIsRunning() {
        return isRunning;
    }

    /**
     * @param isRunning the isRunning to set
     */
    public void setIsRunning(boolean isRunning) {
        this.isRunning = isRunning;
    }

    /**
     * @return the status
     */
    public String getStatus() {
        return status;
    }

    /**
     * @param status the status to set
     */
    public void setStatus(String status) {
        this.status = status;
    }

    /**
     * @return the result
     */
    public Result getResult() {
        return result;
    }

    /**
     * @param result the result to set
     */
    public void setResult(Result result) {
        this.result = result;
    }

    /**
     * @return the progress
     */
    public double getProgress() {
        return progress;
    }
    
    public String getProgressPercent() {
        return String.format("%.1f%%", progress*100.0);
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(double progress) {
        this.progress = progress;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
    
    @PostConstruct
    private void readErrors() {
        Resource failuresRoot = resource.getChild("failures");
        if (failuresRoot != null && failuresRoot.hasChildren()) {
            List<ArchivedProcessFailure> failures = new ArrayList<>();
            failuresRoot.getChildren().forEach(step->
                    step.getChildren().forEach(f -> 
                            failures.add(f.adaptTo(ArchivedProcessFailure.class))
                    )
            );
            setReportedErrors(failures);
        }
    }
    
    private String formatDate(long time) {
        Calendar today = Calendar.getInstance();
        today.clear(Calendar.HOUR_OF_DAY);
        today.clear(Calendar.MINUTE);
        today.clear(Calendar.SECOND);
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(time);
        DateFormat format;
        if (cal.after(today)) {
            format = DateFormat.getTimeInstance();        
        } else {
            format = DateFormat.getDateTimeInstance();
        }
        return format.format(new Date(time));
    }
}
