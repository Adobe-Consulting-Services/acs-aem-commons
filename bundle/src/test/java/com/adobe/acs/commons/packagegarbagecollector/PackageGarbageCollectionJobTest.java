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

import io.wcm.testing.mock.aem.junit.AemContext;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageManager;
import org.apache.jackrabbit.vault.packaging.Packaging;
import org.apache.jackrabbit.vault.packaging.VaultPackage;
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
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.when;
import static com.adobe.acs.commons.packagegarbagecollector.PackageGarbageCollectionScheduler.*;

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
    }

    @Test
    public void testPackageJustOldEnough() {
        mockPackaging(60, false);
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
    }

    @Test
    public void testPackageOldEnough() {
        mockPackaging(70, false);
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
    }

    @Test
    public void testPackageTooYoung() {
        mockPackaging(10, false);
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.OK, consumer.process(job));
    }

    @Test
    public void testFailingToRemovePackages() {
        mockPackaging(61, true);
        Job job = mockJob();
        assertEquals(JobConsumer.JobResult.FAILED, consumer.process(job));
    }

    void mockPackaging(Integer daysAgo, boolean fail) {
        try {
            JcrPackageManager packageManager = mock(JcrPackageManager.class);
            when(packaging.getPackageManager(any())).thenReturn(packageManager);
            JcrPackage mockPackage = mockPackage(daysAgo);
            when(packageManager.listPackages(anyString(), anyBoolean())).thenReturn(Collections.singletonList(mockPackage));
            if (fail) {
                doThrow(new RepositoryException()).when(packageManager).remove(any());
            }
        } catch (RepositoryException | IOException e) {
            fail();
        }
    }

    JcrPackage mockPackage(Integer daysAgo) throws RepositoryException, IOException {
        JcrPackage jcrPackage = mock(JcrPackage.class);
        VaultPackage vaultPackage = mock(VaultPackage.class);
        when(jcrPackage.getPackage()).thenReturn(vaultPackage);
        Calendar packageDate = Calendar.getInstance();
        Date today = new Date();
        packageDate.setTime(today);
        packageDate.add(Calendar.DAY_OF_MONTH, daysAgo*-1);
        when(vaultPackage.getCreated()).thenReturn(packageDate);
        Node packageNode = mock(Node.class);
        when(packageNode.getPath()).thenReturn("/etc/packages/test-package.zip");
        when(jcrPackage.getNode()).thenReturn(packageNode);
        return jcrPackage;
    }

    Job mockJob() {
        Job job = mock(Job.class);
        when(job.getProperty(GROUP_NAME, String.class)).thenReturn("com.adobe.acs");
        when(job.getProperty(MAX_AGE_IN_DAYS, Integer.class)).thenReturn(60);
        return job;
    }
}
