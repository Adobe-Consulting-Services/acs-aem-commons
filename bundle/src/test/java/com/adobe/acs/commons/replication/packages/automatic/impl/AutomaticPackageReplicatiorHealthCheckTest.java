/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 - Adobe
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
package com.adobe.acs.commons.replication.packages.automatic.impl;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Hashtable;

import javax.management.NotCompliantMBeanException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;

import org.apache.sling.hc.api.Result;
import org.apache.sling.hc.api.ResultLog;
import org.apache.sling.hc.api.ResultLog.Entry;
import org.apache.sling.testing.mock.osgi.MockOsgi;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.osgi.framework.BundleContext;

public class AutomaticPackageReplicatiorHealthCheckTest {

    private AutomaticPackageReplicatorImpl automaticPackageReplicator;

    private AutomaticPackageReplicationHealthCheck healthCheck;

    @Before
    public void init() {
        automaticPackageReplicator = Mockito.mock(AutomaticPackageReplicatorImpl.class);
        BundleContext bundleContext = MockOsgi.newBundleContext();
        bundleContext.registerService(new String[] { AutomaticPackageReplicator.class.getName() },
                automaticPackageReplicator, new Hashtable<String, Object>());

        healthCheck = new AutomaticPackageReplicationHealthCheck();
        MockOsgi.injectServices(healthCheck, bundleContext);
    }

    @Test
    public void testNoResults() throws NotCompliantMBeanException {
        Mockito.when(automaticPackageReplicator.getResultLog()).thenReturn(new ResultLog());
        assertTrue(healthCheck.execute().isOk());
    }

    @Test
    public void testFailure() throws NotCompliantMBeanException {
        ResultLog log = new ResultLog();
        log.add(new Entry(Result.Status.CRITICAL, "Ruh Roh"));
        log.add(new Entry(Result.Status.OK, "Yay!"));
        Mockito.when(automaticPackageReplicator.getResultLog()).thenReturn(log);
        assertFalse(healthCheck.execute().isOk());
    }

    @Test
    public void testSuccess() throws NotCompliantMBeanException {
        ResultLog log = new ResultLog();
        log.add(new Entry(Result.Status.OK, "Yay!!"));
        log.add(new Entry(Result.Status.OK, "Yay!"));
        Mockito.when(automaticPackageReplicator.getResultLog()).thenReturn(log);
        assertTrue(healthCheck.execute().isOk());
    }
}