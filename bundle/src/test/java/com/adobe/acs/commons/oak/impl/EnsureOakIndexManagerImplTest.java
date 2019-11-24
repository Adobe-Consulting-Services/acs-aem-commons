/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.oak.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import javax.management.NotCompliantMBeanException;

import org.apache.sling.commons.scheduler.ScheduleOptions;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.ChecksumGeneratorImpl;

public class EnsureOakIndexManagerImplTest {

    @Rule
    public MockitoRule rule = MockitoJUnit.rule();
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    @Mock
    Scheduler scheduler;
    
    
    private static final String OAK_INDEX = "/oak:index";
    
    Map<String,Object> ensureOakIndexManagerProperties;

    
    
    @Before
    public void setup() {
        // setup test content in the repo
        context.build().resource(OAK_INDEX).commit();
        context.registerService(Scheduler.class,scheduler);

        ScheduleOptions options = mock(ScheduleOptions.class);
        when(scheduler.NOW()).thenReturn(options);
        when(scheduler.schedule(any(), any())).thenAnswer((InvocationOnMock invocation) -> {
            EnsureOakIndexJobHandler handler = invocation.getArgument(0);
            handler.run();
            return true;
        });

        context.registerService(ChecksumGenerator.class, new ChecksumGeneratorImpl());
       
        ensureOakIndexManagerProperties = new HashMap<>();
        ensureOakIndexManagerProperties.put("properties.ignore", null);
        
    }
    
    private EnsureOakIndex createAndRegisterEnsureOakIndexDefinition(String definitionPath, String indexPropertyName) {
        

        Map<String,Object> ensureIndexProperties;
        ensureIndexProperties = new HashMap<>();
        ensureIndexProperties.put("jcr:primaryType",EnsureOakIndexJobHandler.NT_OAK_UNSTRUCTURED);
        ensureIndexProperties.put("type","property");
        ensureIndexProperties.put("propertyNames",indexPropertyName);
        
        context.build().resource(definitionPath, ensureIndexProperties).commit();
        Map<String,Object> props = new HashMap<>();
        props.put("oak.indexes.path", OAK_INDEX);
        props.put("ensure-definitions.path",definitionPath);
        props.put("immediate", "false");
        

        EnsureOakIndex eoi = new EnsureOakIndex();
        return context.registerInjectActivateService(eoi, props);
    }
    
    @Test 
    public void testNoEoiRegistered() throws NotCompliantMBeanException {
        EnsureOakIndexManagerImpl impl = new EnsureOakIndexManagerImpl();
        context.registerInjectActivateService(impl,ensureOakIndexManagerProperties);
        assertEquals(0,impl.ensureAll(true));
    }
    
    @Test
    public void testWithIndexRegistrations() throws NotCompliantMBeanException {
        
        
        EnsureOakIndexManagerImpl impl = new EnsureOakIndexManagerImpl();
        context.registerInjectActivateService(impl,ensureOakIndexManagerProperties);
        EnsureOakIndex eoi1 = createAndRegisterEnsureOakIndexDefinition("/apps/my/index1", "abc");
        assertEquals(1,impl.ensureAll(true));
        assertTrue(eoi1.isApplied());
        assertEquals(0,impl.ensureAll(false));
        assertEquals(1,impl.ensure(true,"/apps/my/index1"));
        assertEquals(0,impl.ensure(false,"/apps/my/index1"));
        
        EnsureOakIndex eoi2 = createAndRegisterEnsureOakIndexDefinition("/apps/my/index2", "abcd");
        assertFalse(eoi2.isApplied());
        assertEquals(1,impl.ensure(false,"/apps/my/index2"));
        assertTrue(eoi2.isApplied());
        
    }
    

    

}
