/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;

import static com.adobe.acs.commons.contentsync.ContentSyncJobConsumer.JOB_TOPIC;
import com.adobe.granite.security.user.util.AuthorizableUtil;

/**
 * Model for retrieving and representing the history of ACS Commons Content Sync jobs.
 * <p>
 * This model adapts from a {@link SlingHttpServletRequest} and provides access to all jobs
 * (queued, active, and history) for the {@link com.adobe.acs.commons.contentsync.ContentSyncJobConsumer#JOB_TOPIC}.
 * Each job is wrapped as an {@link Item}, which exposes job properties and progress information.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class ContentSyncHistoryModel {

    /**
     * The current Sling HTTP request.
     */
    @SlingObject
    private SlingHttpServletRequest request;

    /**
     * The Sling JobManager service.
     */
    @OSGiService
    private JobManager jobManager;

    /**
     * Returns a collection of all jobs (queued, active, history) for the content sync topic,
     * sorted by creation date descending.
     *
     * @return a collection of {@link Item} representing each job
     */
    public Collection<Item> getJobs() {
        Collection<Job> queued = jobManager.findJobs(JobManager.QueryType.QUEUED, JOB_TOPIC, 100, null);
        Collection<Job> active = jobManager.findJobs(JobManager.QueryType.ACTIVE, JOB_TOPIC, 100,  null);
        Collection<Job> history = jobManager.findJobs(JobManager.QueryType.HISTORY, JOB_TOPIC, 100,  null);
        Collection<Job> all = new ArrayList<>();
        all.addAll(queued);
        all.addAll(active);
        all.addAll(history);

        return all.stream()
                .sorted((j1, j2) -> j2.getCreated().compareTo(j1.getCreated()))
                .map(Item::new)
                .collect(Collectors.toList());
    }

    /**
     * Wrapper for a Sling Job, exposing job properties and progress information.
     */
    public class Item extends LinkedHashMap<String, Object> {
        private final Job job;

        /**
         * Constructs an Item from a Sling Job, copying all job properties.
         *
         * @param job the Sling Job to wrap
         */
        Item(Job job) {
            this.job = job;
            for (String key : job.getPropertyNames()) {
                put(key, job.getProperty(key));
            }
        }

        /**
         * Returns the job state as a string.
         *
         * @return the job state
         */
        public String getJobState() {
            return job.getJobState().toString();
        }

        /**
         * Returns the job progress as a percentage (0-100).
         *
         * @return the progress percentage
         */
        public int getProgress() {
            if (containsKey("slingevent:finishedState")) return 100;

            int progressStep = (int) getOrDefault(Job.PROPERTY_JOB_PROGRESS_STEP, 0);
            int progressSteps = (int) getOrDefault(Job.PROPERTY_JOB_PROGRESS_STEPS, 0);
            return progressSteps == 0 ? 0 : (100 * progressStep) / progressSteps;
        }

        /**
         * Returns a color string representing the job status.
         * <ul>
         *     <li>yellow for ACTIVE</li>
         *     <li>blue for SUCCEEDED</li>
         *     <li>red for other states</li>
         * </ul>
         *
         * @return the status color
         */
        public String getStatusColor() {
            String color;
            switch (job.getJobState()) {
                case ACTIVE:
                    color = "yellow";
                    break;
                case SUCCEEDED:
                    color = "blue";
                    break;
                default:
                    color = "red";
                    break;
            }
            return color;
        }

        /**
         * Returns a progress message in the format "currentStep of numberOfSteps",
         * or "loading" if steps are not available.
         *
         * @return the progress message
         */
        public String getProgressMessage() {
            if (getNumberOfSteps() == 0 || getCurrentStep() == 0) {
                return "loading";
            }
            return String.format("%d of %d", getCurrentStep(), getNumberOfSteps());
        }

        /**
         * Returns the total number of progress steps for the job.
         *
         * @return the number of steps
         */
        public int getNumberOfSteps() {
            return (int) getOrDefault("slingevent:progressSteps", 0);
        }

        /**
         * Returns the current progress step for the job.
         *
         * @return the current step
         */
        public int getCurrentStep() {
            return (int) getOrDefault("slingevent:progressStep", 0);
        }

        /**
         * Returns the formatted name of the user who started the job.
         *
         * @return the started by user name
         */
        public String getStartedBy() {
            return AuthorizableUtil.getFormattedName(request.getResourceResolver(), (String) get("cq:startedBy"));
        }
    }
}
