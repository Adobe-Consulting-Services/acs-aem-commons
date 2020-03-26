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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ScheduledAutomaticPackageReplicatorTest {

    @Test
    public void testRunnable()
            throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {

        final String packagePath = "/etc/packages/test";
        final ScheduledAutomaticPackageReplicatorConfig config = new ScheduledAutomaticPackageReplicatorConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String packagePath() {
                return packagePath;
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
        final ScheduledAutomaticPackageReplicator sb = new ScheduledAutomaticPackageReplicator();
        sb.activate(config);
        final AutomaticPackageReplicator apr = Mockito.mock(AutomaticPackageReplicator.class);
        sb.setAutomaticPackageReplicator(apr);
        sb.run();
        Mockito.verify(apr).replicatePackage(packagePath);

    }
}