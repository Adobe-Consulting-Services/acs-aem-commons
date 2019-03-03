package com.adobe.acs.commons.exporters.impl.users;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.testing.mock.sling.junit.SlingContext;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class UsersSaveServletTest {
    
    public static final String PATH = "/report";
    
    @Rule
    public SlingContext context = new SlingContext();
    
    
    @Before
    public void setup() {
        
        Map<String,Object> props = new HashMap<String,Object>();
        props.put(Constants.GROUP_FILTER,"abc");
        context.build().resource(PATH, props ).commit();
        Resource resource = context.resourceResolver().getResource(PATH);
        context.request().setResource(resource);
        
    }
    
    
    @Test
    public void testPost() throws Exception {
        UsersSaveServlet servlet = new UsersSaveServlet();
        Map<String,Object> params = new HashMap<>();
        params.put("params", UserExportServletTest.buildParameterObject("direct", "somegroup"));
        context.request().setParameterMap(params);
        servlet.doPost(context.request(), context.response());
        
        assertEquals(context.response().getStatus(), 200);
        
        context.resourceResolver().refresh();
        Resource resource = context.resourceResolver().getResource(PATH);
        ValueMap vm = resource.getValueMap();
        assertNotNull(vm);
        assertEquals("direct",vm.get(Constants.GROUP_FILTER,"default"));
        assertEquals("somegroup",vm.get(Constants.GROUPS,"default"));
        assertEquals("abc",vm.get(Constants.CUSTOM_PROPERTIES,"default")); 
        
    }

}
