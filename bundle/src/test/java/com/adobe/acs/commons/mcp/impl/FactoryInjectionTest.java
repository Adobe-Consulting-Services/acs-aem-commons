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
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.impl.processes.asset.FileAssetIngestor;
import com.adobe.acs.commons.mcp.impl.processes.asset.FileAssetIngestorFactory;
import com.adobe.acs.commons.mcp.impl.processes.AssetReport;
import com.adobe.acs.commons.mcp.impl.processes.AssetReportFactory;
import com.adobe.acs.commons.mcp.impl.processes.DeepPrune;
import com.adobe.acs.commons.mcp.impl.processes.DeepPruneFactory;
import com.adobe.acs.commons.mcp.impl.processes.ProcessCleanup;
import com.adobe.acs.commons.mcp.impl.processes.ProcessCleanupFactory;
import com.adobe.acs.commons.mcp.impl.processes.asset.S3AssetIngestor;
import com.adobe.acs.commons.mcp.impl.processes.asset.S3AssetIngestorFactory;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.PageManagerFactory;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class FactoryInjectionTest {

    private ControlledProcessManagerImpl cpm;

    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    @Before
    public void setup() {
        // this just forces the creation of the ResourceResolverFactory service
        slingContext.build().commit();

        cpm = new ControlledProcessManagerImpl();
        registerCommonServices();
        registerFactories();

        slingContext.registerInjectActivateService(cpm);

    }

    @Test
    public void testFileIngestorFactory() throws Exception {
        ProcessDefinition def = cpm.findDefinitionByNameOrPath("Asset Ingestor");
        assertNotNull(def);
        assertTrue(def instanceof FileAssetIngestor);
    }

    @Test
    public void testS3IngestorFactory() throws Exception {
        ProcessDefinition def = cpm.findDefinitionByNameOrPath("S3 Asset Ingestor");
        assertNotNull(def);
        assertTrue(def instanceof S3AssetIngestor);
    }

    @Test
    public void testAssetReportFactory() throws Exception {
        ProcessDefinition def = cpm.findDefinitionByNameOrPath("Asset Report");
        assertNotNull(def);
        assertTrue(def instanceof AssetReport);
    }

    @Test
    public void testDeepPruneFactory() throws Exception {
        ProcessDefinition def = cpm.findDefinitionByNameOrPath("Deep Prune");
        assertNotNull(def);
        assertTrue(def instanceof DeepPrune);
    }
   
    @Test
    public void testProcessCleanupFactory() throws Exception {
        ProcessDefinition def = cpm.findDefinitionByNameOrPath("Process Cleanup");
        assertNotNull(def);
        assertTrue(def instanceof ProcessCleanup);
    }

    private void registerFactories() {
        slingContext.registerInjectActivateService(new FileAssetIngestorFactory());
        slingContext.registerInjectActivateService(new AssetReportFactory());
        slingContext.registerInjectActivateService(new DeepPruneFactory());
        slingContext.registerInjectActivateService(new ProcessCleanupFactory());
        slingContext.registerInjectActivateService(new S3AssetIngestorFactory());
    }

    private void registerCommonServices() {
        registerMock(ActionManagerFactory.class);
        registerMock(JobManager.class);
        registerMock(PageManagerFactory.class);
        registerMock(Replicator.class);
    }

    private <T> void registerMock(Class<T> clazz) {
        slingContext.registerService(clazz, mock(clazz));
    }

}
