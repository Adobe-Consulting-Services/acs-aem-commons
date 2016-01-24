package com.adobe.acs.commons.oak.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.junit.Assert;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.resourceresolver.MockValueMap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;

import static org.mockito.Mockito.*;

import org.mockito.runners.MockitoJUnitRunner;

/**
 * 
 * Test the algorithm within the JobHandler, but do not validate the inner working of the
 * performing methods
 * 
 *
 */


@RunWith(MockitoJUnitRunner.class)
public class EnsureOakIndexJobHandlerTest {

    
    EnsureOakIndexJobHandler handler;
    
    private static final String OAK_INDEX = "/oak:index";
    private static final String INDEX_NAME = "testIndex";
    private static final String DEFINITION_PATH = "/apps/mydefinitions/index/" + INDEX_NAME;
    
    
    @Before
    public void init () throws RepositoryException, IOException {
        
      
        // Create a job handler, where we mock the execution of the index actions
        EnsureOakIndexJobHandler e = new EnsureOakIndexJobHandler (null, OAK_INDEX, DEFINITION_PATH);
        handler = spy(e);
       
        doNothing().when(handler).disableIndex(Matchers.any(Resource.class));
        doNothing().when(handler).delete(Matchers.any(Resource.class));
        doNothing().when(handler).forceRefresh(Matchers.any(Resource.class));
        
        doReturn(null).when(handler).update(any(Resource.class), any(Resource.class), anyBoolean());
        doReturn(null).when(handler).create(any(Resource.class), any(Resource.class));
            
    }
    
    Resource getEnsureOakDefinition (Map<String,Object> properties) {
        
        Resource r = mock (Resource.class);
        ValueMap vm = new MockValueMap(r, properties);
        when (r.getValueMap()).thenReturn(vm);
        when (r.getName()).thenReturn(INDEX_NAME);
        when (r.getPath()).thenReturn(DEFINITION_PATH);
        
        return r;
        
    }
    
    
    
    @Test
    public void testIgnoreProperty () throws PersistenceException, RepositoryException {
        
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(EnsureOakIndexJobHandler.PN_IGNORE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        Resource def = getEnsureOakDefinition (props);
        
        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, null));
        verify (handler, never()).disableIndex(any(Resource.class));
        verify (handler, never()).delete(any(Resource.class));
        
    }
    
    @Test
    public void testDisableProperty () throws PersistenceException, RepositoryException {
        
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition (props);
        
        Resource r = mock (Resource.class);
        
        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify (handler, times(1)).disableIndex(any(Resource.class));
        verify (handler, never()).delete(any(Resource.class));
    }
    
    
    
    
    @Test
    public void testDeleteProperty () throws PersistenceException, RepositoryException {
        
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition (props);
        
        Resource r = mock (Resource.class);
        
        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify (handler, never()).disableIndex(any(Resource.class));
        verify (handler, times(1)).delete(any(Resource.class));
    }
    
    @Test
    public void testDeletePropertyWithoutExistingIndex () throws PersistenceException, RepositoryException {
        
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(EnsureOakIndexJobHandler.PN_DELETE, "true");
        props.put(EnsureOakIndexJobHandler.PN_DISABLE, "true");
        Resource def = getEnsureOakDefinition (props);
        
        Resource r = null;
        
        Assert.assertTrue(handler.handleLightWeightIndexOperations(def, r));
        verify (handler, never()).disableIndex(any(Resource.class));
        verify (handler, never()).delete(any(Resource.class));
    }
    
    
}
