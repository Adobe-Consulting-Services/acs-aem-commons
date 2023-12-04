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

import com.adobe.acs.commons.testutil.LogTester;
import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.PackageId;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.sling.event.jobs.Job;
import org.apache.sling.event.jobs.consumer.JobConsumer;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import java.io.IOException;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.stream.Collectors;

import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.*;
import static com.adobe.acs.commons.testutil.LogTester.assertLogText;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PackageGarbageCollectionJobTest {

    @Rule
    public AemContext context = new AemContext(ResourceResolverType.JCR_MOCK);

    PackageGarbageCollectionJob consumer;

    @Mock
    Packaging packaging;

    @Before
    public void setup() {
        context.registerService(Packaging.class, packaging);
        consumer = context.registerInjectActivateService(PackageGarbageCollectionJob.class);
        LogTester.reset();
    }

    @Test
    public void testPackageJustOldEnough() throws RepositoryException, IOException {
        mockPackageManager(
                mockPackage(60, 60, "acs.ui.apps-6.0.0.zip", "acs.ui.apps", "6.0.0"),
                mockPackage(30, 30, "acs.ui.apps-7.0.0.zip", "acs.ui.apps", "7.0.0")
        );
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
        assertLogText("Deleted installed package acs.ui.apps:com.acs:v6.0.0 [/etc/packages/acs.ui.apps-6.0.0.zip] since it is not the latest installed version.");
        assertLogText("Package Garbage Collector job finished - Removed 1 packages");
    }

    @Test
    public void testPackageOldEnough() throws RepositoryException, IOException {
        mockPackageManager(
                mockPackage(70, 70, "acs.ui.apps-6.0.0.zip", "acs.ui.apps", "6.0.0"),
                mockPackage(30, 30, "acs.ui.apps-7.0.0.zip", "acs.ui.apps", "7.0.0")
        );
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
        assertLogText("Deleted installed package acs.ui.apps:com.acs:v6.0.0 [/etc/packages/acs.ui.apps-6.0.0.zip] since it is not the latest installed version.");
        assertLogText("Package Garbage Collector job finished - Removed 1 packages");
    }

    @Test
    public void testPackageTooYoung() throws RepositoryException, IOException {
        mockPackageManager(mockPackage(10, 10, "acs.ui.apps-6.0.0.zip", "acs.ui.apps", "6.0.0"));
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
        assertLogText("Not removing package because it's not old enough acs.ui.apps:com.acs:v6.0.0 [/etc/packages/acs.ui.apps-6.0.0.zip]");
        assertLogText("Package Garbage Collector job finished - Removed 0 packages");
    }

    @Test
    public void testMultiplePackagesOfSameName() throws RepositoryException, IOException {
        mockPackageManager(
                mockPackage(30, 30, "acs.ui.apps-5.0.0.zip", "acs.ui.apps", "5.0.0"),
                mockPackage(60, 60, "acs.ui.apps-6.0.0.zip", "acs.ui.apps", "6.0.0"),
                mockPackage(70, 10, "acs.ui.apps-7.0.0.zip", "acs.ui.apps", "7.0.0"),
                mockPackage(80, 80, "acs.ui.apps-8.0.0.zip", "acs.ui.apps", "8.0.0"),
                mockPackage(80, 80, "acs.ui.config-7.0.0.zip", "acs.ui.config", "7.0.0")
        );
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
        assertLogText("Deleted installed package acs.ui.apps:com.acs:v6.0.0 [/etc/packages/acs.ui.apps-6.0.0.zip] since it is not the latest installed version.");
        assertLogText("Deleted installed package acs.ui.apps:com.acs:v8.0.0 [/etc/packages/acs.ui.apps-8.0.0.zip] since it is not the latest installed version.");
        assertLogText("Not removing package because it's not old enough acs.ui.apps:com.acs:v5.0.0 [/etc/packages/acs.ui.apps-5.0.0.zip]");
        assertLogText("Not removing package because it's the current installed one acs.ui.apps:com.acs:v7.0.0 [/etc/packages/acs.ui.apps-7.0.0.zip]");
        assertLogText("Not removing package because it's the current installed one acs.ui.config:com.acs:v7.0.0 [/etc/packages/acs.ui.config-7.0.0.zip]");
        assertLogText("Package Garbage Collector job finished - Removed 2 packages");
    }

    void mockPackageManager(JcrPackage... jcrPackage) throws RepositoryException {
        JcrPackageManager packageManager = mock(JcrPackageManager.class);
        when(packageManager.listPackages(anyString(), anyBoolean())).thenReturn(Arrays.stream(jcrPackage).collect(Collectors.toList()));
        when(packaging.getPackageManager(any())).thenReturn(packageManager);
    }

    JcrPackage mockPackage(Integer daysAgo, Integer lastUnpackedDaysAgo, String packageName, String name, String version) throws RepositoryException, IOException {
        JcrPackage jcrPackage = mock(JcrPackage.class);
        Node packageNode = mock(Node.class);
        when(packageNode.getPath()).thenReturn("/etc/packages/" + packageName);
        JcrPackageDefinition definition = mock(JcrPackageDefinition.class);
        when(definition.getLastUnpacked()).thenReturn(getDate(lastUnpackedDaysAgo));
        when(definition.getCreated()).thenReturn(getDate(daysAgo));
        when(definition.getNode()).thenReturn(packageNode);
        PackageId pid = mock(PackageId.class);
        when(pid.getName()).thenReturn(name);
        when(pid.getGroup()).thenReturn("com.acs");
        when(pid.getVersionString()).thenReturn(version);
        when(definition.getId()).thenReturn(pid);
        when(jcrPackage.getDefinition()).thenReturn(definition);
        return jcrPackage;
    }

    Calendar getDate(Integer daysAgo) {
        Calendar packageDate = Calendar.getInstance();
        Date today = new Date();
        packageDate.setTime(today);
        packageDate.add(Calendar.DAY_OF_MONTH, daysAgo * -1);
        return packageDate;
    }

    Job mockJob() {
        Job job = mock(Job.class);
        when(job.getProperty(GROUP_NAME, String.class)).thenReturn("com.adobe.acs");
        when(job.getProperty(MAX_AGE_IN_DAYS, Integer.class)).thenReturn(60);
        when(job.getProperty(REMOVE_NOT_INSTALLED_PACKAGES, false)).thenReturn(false);
        return job;
    }
}
