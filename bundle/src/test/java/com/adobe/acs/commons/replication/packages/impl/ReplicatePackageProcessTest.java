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

package com.adobe.acs.commons.replication.packages.impl;

import java.io.IOException;

import javax.jcr.RepositoryException;

import com.adobe.acs.commons.replication.packages.automatic.AutomaticPackageReplicator;
import com.adobe.acs.commons.replication.packages.automatic.impl.ReplicatePackageProcess;
import com.adobe.acs.commons.util.WorkflowHelper;
import com.day.cq.replication.ReplicationException;
import com.day.cq.workflow.WorkflowException;
import com.day.cq.workflow.metadata.MetaDataMap;

import org.apache.jackrabbit.vault.packaging.PackageException;
import org.apache.sling.api.resource.LoginException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ReplicatePackageProcessTest {

    @Test
    public void testProcess() throws RepositoryException, PackageException, IOException, ReplicationException,
            LoginException, WorkflowException {

        final String packagePath = "/etc/packages/test";
        ReplicatePackageProcess rpp = new ReplicatePackageProcess();
        AutomaticPackageReplicator apr = Mockito.mock(AutomaticPackageReplicator.class);
        rpp.setAutomaticPackageReplicator(apr);
        MetaDataMap mdm = Mockito.mock(MetaDataMap.class);
        Mockito.when(mdm.get(WorkflowHelper.PROCESS_ARGS, String.class)).thenReturn(packagePath);
        rpp.execute(null, null, mdm);
        Mockito.verify(apr).replicatePackage(packagePath);

    }
}