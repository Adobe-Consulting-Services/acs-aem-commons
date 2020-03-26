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

package com.adobe.acs.commons.replication.packages.automatic.impl;

import java.io.IOException;
import java.lang.annotation.Annotation;

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.ScheduledAutomaticPackageReplicator.ScheduledAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledAutomaticPackageReplicatorTest {

    private static final String PACKAGE_PATH = "/etc/packages/test";
    private ScheduledAutomaticPackageReplicator sb;
    private AutomaticPackageReplicator apr;

    @Before
    public void init() {
        final ScheduledAutomaticPackageReplicatorConfig config = new ScheduledAutomaticPackageReplicatorConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String packagePath() {
                return PACKAGE_PATH;
            }

            @Override
            public String scheduler_expression() {
                return null;
            }

            @Override
            public String webconsole_configurationFactory_nameHint() {
                return null;
            }

        };
        sb = new ScheduledAutomaticPackageReplicator();
        sb.activate(config);
        apr = Mockito.mock(AutomaticPackageReplicator.class);
        sb.setAutomaticPackageReplicator(apr);
    }

    @Test
    public void testRunnable()
            throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {
        sb.run();
        Mockito.verify(apr).replicatePackage(PACKAGE_PATH);
    }

    @Test
    public void testFailure()
            throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {
        Mockito.doThrow(new ReplicationException("")).when(apr).replicatePackage(Mockito.anyString());
        sb.run();
        Mockito.verify(apr).replicatePackage(PACKAGE_PATH);
    }
}