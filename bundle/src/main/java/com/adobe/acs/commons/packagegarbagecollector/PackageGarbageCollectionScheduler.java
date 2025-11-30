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
package com.adobe.acs.commons.packagegarbagecollector;

import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component(immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE)
@Designate(ocd = PackageGarbageCollectionConfig.class, factory = true)
public class PackageGarbageCollectionScheduler {
    private static final Logger LOG = LoggerFactory.getLogger(PackageGarbageCollectionScheduler.class);
    public static final String JOB_TOPIC = "com/adobe/acs/commons/PackageGarbageCollectionJob";
    public static final String GROUP_NAME = "groupName";
    public static final String MAX_AGE_IN_DAYS = "maxAgeInDays";
    static final String REMOVE_NOT_INSTALLED_PACKAGES = "removeNotInstalledPackages";

    @Reference
    JobManager jobManager;

    ScheduledJobInfo job;

    @Activate
    protected void activate(PackageGarbageCollectionConfig config) {
        job = scheduleJob(config);
        if (LOG.isInfoEnabled() && job != null) {
            LOG.info("Next scheduled run for job with group name {} at {}", config.groupName(), job.getNextScheduledExecution());
        }
    }

    @Deactivate
    protected void deactivate() {
        if (job != null) {
            job.unschedule();
        }
    }

    private ScheduledJobInfo scheduleJob(PackageGarbageCollectionConfig config) {
        Map<String, Object> filter = Collections.singletonMap("=" + GROUP_NAME, config.groupName());
        Collection<ScheduledJobInfo> existingJob = jobManager.getScheduledJobs(JOB_TOPIC, 1, filter);
        if (existingJob.isEmpty()) {
            return jobManager.createJob(JOB_TOPIC)
                    .properties(getProperties(config))
                    .schedule()
                    .cron(config.scheduler())
                    .add();
        }
        LOG.info("Job for {} at {} already scheduled - just returning the existing one", config.groupName(), config.scheduler());
        return existingJob.stream().findFirst().orElse(null);
    }

    private Map<String, Object> getProperties(PackageGarbageCollectionConfig config) {
        Map<String, Object> properties = new HashMap<>();
        properties.put(GROUP_NAME, config.groupName());
        properties.put(MAX_AGE_IN_DAYS, config.maxAgeInDays());
        properties.put(REMOVE_NOT_INSTALLED_PACKAGES, config.removeNotInstalledPackages());
        return properties;
    }
}
