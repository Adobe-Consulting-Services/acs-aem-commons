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
import com.adobe.acs.commons.replication.packages.automatic.impl.EventBasedAutomaticPackageReplicator.EventBasedAutomaticPackageReplicatorConfig;
import com.day.cq.replication.ReplicationException;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class EventBasedAutomaticPackageReplicatorTest {

    private static final String PACKAGE_PATH = "/etc/packages/test";
    private AutomaticPackageReplicator apr;
    private EventBasedAutomaticPackageReplicator eb;

    @Before
    public void init() {

        final EventBasedAutomaticPackageReplicatorConfig config = new EventBasedAutomaticPackageReplicatorConfig() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return null;
            }

            @Override
            public String event_topics() {
                return null;
            }

            @Override
            public String packagePath() {
                return PACKAGE_PATH;
            }

            @Override
            public String webconsole_configurationFactory_nameHint() {
                return null;
            }

        };
        eb = new EventBasedAutomaticPackageReplicator();
        eb.activate(config);
        apr = Mockito.mock(AutomaticPackageReplicator.class);
        eb.setAutomaticPackageReplicator(apr);

    }

    @Test
    public void testEvent()
            throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {
        eb.handleEvent(null);
        Mockito.verify(apr).replicatePackage(PACKAGE_PATH);
    }

    @Test
    public void testFailure()
            throws RepositoryException, PackageException, IOException, ReplicationException, LoginException {
        Mockito.doThrow(new ReplicationException("")).when(apr).replicatePackage(Mockito.anyString());
        eb.handleEvent(null);
        Mockito.verify(apr).replicatePackage(PACKAGE_PATH);
    }
}