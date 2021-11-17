/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.scheduler.Scheduler;
import org.apache.sling.testing.mock.sling.ResourceResolverType;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.ChecksumGeneratorImpl;
import com.adobe.acs.commons.oak.EnsureOakIndexManager;

/**
 * 
 * Test the algorithm within the JobHandler, but do not validate the inner working of the
 * performing methods
 * 
 *
 */
@RunWith(MockitoJUnitRunner.class)
public class EnsureOakIndexJobHandlerTest {

    
    /**
     * It's required to use JCR_OAK here, although it's not really necessary from a 
     * feature point of view.
     * But the Node implementation of JCR_MOCK (MockNode.class) does not support the 
     * "setPrimaryType()" call, but throws an exception instead. So falling back to the
     * full-blown Oak implementation.
     * That makes testing a bit slower and also requires to make all index definitions fully compliant.
     */
    
    @Rule
    public SlingContext context = new SlingContext(ResourceResolverType.JCR_OAK);
    
    EnsureOakIndexJobHandler handler;
    
    private static final String OAK_INDEX = "/oak:index";
    private static final String INDEX_NAME = "myIndex";
    private static final String DEFINITION_PATH = "/apps/mydefinitions/index";
    
    private static final String ENSURE_INDEX_PATH = DEFINITION_PATH + "/" + INDEX_NAME;
    private static final String OAK_INDEX_PATH = OAK_INDEX + "/" + INDEX_NAME;

    Map<String,Object> ensureIndexProperties;
    Map<String,Object> oakIndexProperties;
    
    @Mock
    Scheduler scheduler;
    

    @Before
    public void setup() {
        
        // setup test content in the repo
        context.build().resource(OAK_INDEX).commit();
        
        // setup dependencies
        Map<String,Object> props = new HashMap<>();
        props.put("oak.indexes.path", OAK_INDEX);
        props.put("ensure-definitions.path",DEFINITION_PATH);
        props.put("immediate", "false");
        
        EnsureOakIndexManager eoim = Mockito.mock(EnsureOakIndexManager.class);
        context.registerService(EnsureOakIndexManager.class, eoim);
        
        context.registerService(Scheduler.class,scheduler);
        context.registerService(ChecksumGenerator.class, new ChecksumGeneratorImpl());
        EnsureOakIndex eoi = new EnsureOakIndex();
        context.registerInjectActivateService(eoi, props);

        handler = new EnsureOakIndexJobHandler(eoi, OAK_INDEX, DEFINITION_PATH);
        
        // setup the invariant properties for the index definition
        ensureIndexProperties = new HashMap<>();
        ensureIndexProperties.put("jcr:primaryType",EnsureOakIndexJobHandler.NT_OAK_UNSTRUCTURED);
        ensureIndexProperties.put("type","property");
        ensureIndexProperties.put("propertyNames","newProp");
        
        // we need to create a working index definition, let's choose a property index to make it simpler here.
        // it has also the advantage that indexing is synchronous, so we can validate the result immediately after
        oakIndexProperties = new HashMap<>();
        oakIndexProperties.put("jcr:primaryType", EnsureOakIndexJobHandler.NT_OAK_QUERY_INDEX_DEFINITION);
        oakIndexProperties.put("type", "property");
        oakIndexProperties.put("propertyNames", "randomProp");
        
    }
    
    @Test
    public void testIgnoreProperty() {
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_IGNORE, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNull(indexResource);   
    }
    
    @Test
    public void testDeleteIndex() {
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        context.build().resource(OAK_INDEX_PATH, oakIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNull(indexResource);
        // re-run the test and get the log message (statement about non-existing index)
        handler.run();
        indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNull(indexResource);
    }
    
    @Test
    public void testDisableIndex() {
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        context.build().resource(OAK_INDEX_PATH, oakIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull("Index does not exist anymore",indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("disabled",vm.get("type",String.class));
        
    }
    
    @Test
    public void testDisableIndexWithNonExistingOakIndex() {
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNull("Index has been created, but it should not have",indexResource);
    }
    
    @Test
    public void testCreate() {
        
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull(indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("newProp",vm.get("propertyNames",String.class));
        assertEquals("1",vm.get("reindexCount",String.class));
    }
    
    @Test
    public void testCreateWithForcedRefresh() {
        
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_FORCE_REINDEX, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull(indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("newProp",vm.get("propertyNames",String.class));
        assertEquals("1",vm.get("reindexCount",String.class));
        assertEquals("true",vm.get(EnsureOakIndexJobHandler.PN_FORCE_REINDEX,String.class));
    }
    
    @Test
    public void testUpdate() {
        
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        context.build().resource(OAK_INDEX_PATH, oakIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull(indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("newProp",vm.get("propertyNames",String.class));
        assertEquals("1",vm.get("reindexCount",String.class));
    }
    
    @Test
    public void testUpdateRecreateOnUpdate() {
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_RECREATE_ON_UPDATE, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        context.build().resource(OAK_INDEX_PATH, oakIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull(indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("newProp",vm.get("propertyNames",String.class));
        assertNull(vm.get("reindexCount",String.class));
    }
    
    @Test
    public void testUpdateWithForcedRefresh() {
        
        ensureIndexProperties.put(EnsureOakIndexJobHandler.PN_FORCE_REINDEX, "true");
        context.build().resource(ENSURE_INDEX_PATH, ensureIndexProperties).commit();
        context.build().resource(OAK_INDEX_PATH, oakIndexProperties).commit();
        handler.run();
        Resource indexResource = context.resourceResolver().getResource(OAK_INDEX_PATH);
        assertNotNull(indexResource);
        ValueMap vm = indexResource.adaptTo(ValueMap.class);
        assertNotNull(vm);
        assertEquals("newProp",vm.get("propertyNames",String.class));
        assertEquals("2",vm.get("reindexCount",String.class));
        
        // if we repeat this step, forceReindex should be ignored
        handler.run();
        vm = indexResource.adaptTo(ValueMap.class);
        assertEquals("2",vm.get("reindexCount",String.class));
    }
    

}
