/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.mcp.impl;

import com.adobe.acs.commons.fam.ActionManagerFactory;
import com.adobe.acs.commons.mcp.DynamicScriptResolverService;
import com.adobe.acs.commons.mcp.ProcessDefinition;
import com.adobe.acs.commons.mcp.ProcessDefinitionFactory;
import com.adobe.acs.commons.mcp.form.FieldComponent;
import com.day.cq.replication.Replicator;
import com.day.cq.wcm.api.PageManagerFactory;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.scripting.SlingScriptHelper;
import org.apache.sling.event.jobs.JobManager;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class DynamicScriptServiceProviderTest {
    private ControlledProcessManagerImpl cpm;

    @Rule
    public final SlingContext slingContext = new SlingContext(ResourceResolverType.RESOURCERESOLVER_MOCK);

    static ProcessDefinition testProcessDefinition;
    static ProcessDefinitionFactory testProcessDefinitionFactory;
    static Map<String, FieldComponent> testComponents = new HashMap<>();
    
    @BeforeClass
    public static void setupClass() {
        testProcessDefinition = mock(ProcessDefinition.class);
        testProcessDefinitionFactory = new TestProcessDefinitionFactory();
    }

    @Before
    public void setup() {
        // this just forces the creation of the ResourceResolverFactory service
        slingContext.build().commit();

        registerCommonServices();
        cpm = new ControlledProcessManagerImpl();
        slingContext.registerService(ProcessDefinitionFactory.class, mock(ProcessDefinitionFactory.class));
        slingContext.registerInjectActivateService(cpm);
        slingContext.registerService(DynamicScriptResolverService.class, new TestDynamicScriptResolverService());
    }

    @Test
    public void testScriptLists() throws ReflectiveOperationException {
        ProcessDefinition pd = cpm.findDefinitionByNameOrPath(TEST_PATH);
        assertNotNull("Should retrieve by path", pd);
        assertEquals("Should return expected object", testProcessDefinition, pd);
    }

    @Test
    public void testScriptComponents() throws ReflectiveOperationException {
        assertEquals("Should return components", testComponents, cpm.getComponentsForProcessDefinition(TEST_PATH, slingContext.slingScriptHelper()));
        assertNull("Should null", cpm.getComponentsForProcessDefinition("/bogus/path", slingContext.slingScriptHelper()));
    }

    public static String TEST_PATH = "/test/path";

    public static class TestDynamicScriptResolverService implements DynamicScriptResolverService {

        @Override
        public Map<String, ProcessDefinitionFactory> getDetectedProcesDefinitionFactories(ResourceResolver rr) {
            Map<String, ProcessDefinitionFactory> map = new HashMap<>();
            map.put(TEST_PATH, testProcessDefinitionFactory);
            return map;
        }

        @Override
        public Map<String, FieldComponent> geFieldComponentsForProcessDefinition(String identifier, SlingScriptHelper sling) throws ReflectiveOperationException {
            if (identifier.equals(TEST_PATH)) {
                return testComponents;
            } else {
                return null;
            }
        }
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

    private static class TestProcessDefinitionFactory extends ProcessDefinitionFactory {
        @Override
        public String getName() {
            return "test";
        }

        @Override
        protected ProcessDefinition createProcessDefinitionInstance() {
            return testProcessDefinition;
        }
    }
}
