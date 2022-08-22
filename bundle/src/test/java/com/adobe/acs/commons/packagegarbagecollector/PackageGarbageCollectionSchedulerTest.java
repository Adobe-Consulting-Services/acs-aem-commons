/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 AEM developer community
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

import com.adobe.acs.commons.util.RequireAem;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.sling.event.jobs.JobBuilder;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.event.jobs.ScheduledJobInfo;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mozilla.javascript.commonjs.module.Require;

import java.util.Collection;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageGarbageCollectionSchedulerTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    @Mock
    JobManager jobManager;

    PackageGarbageCollectionScheduler scheduler;

    @Before
    public void setup() {
        context.registerService(RequireAem.class,mock(RequireAem.class),"distribution","classic");
        context.registerService(JobManager.class, jobManager);
    }

    @Test
    public void testDefaultSchedulerConfigurationWithEnabled() {
        mockJobManager();
        scheduler = context.registerInjectActivateService(PackageGarbageCollectionScheduler.class, Collections.singletonMap("enabled", true));
        assertNotNull(scheduler);
        scheduler.deactivate();
    }

    void mockJobManager() {
        JobBuilder builder = mock(JobBuilder.class);
        JobBuilder.ScheduleBuilder schedule = mock(JobBuilder.ScheduleBuilder.class);
        ScheduledJobInfo job = mock(ScheduledJobInfo.class);
        when(jobManager.createJob(anyString())).thenReturn(builder);
        when(builder.properties(anyMap())).thenReturn(builder);
        when(builder.schedule()).thenReturn(schedule);
        when(schedule.cron(anyString())).thenReturn(schedule);
        when(schedule.add()).thenReturn(job);
    }
}
