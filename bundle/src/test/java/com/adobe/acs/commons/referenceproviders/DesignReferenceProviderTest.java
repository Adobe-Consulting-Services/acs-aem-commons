package com.adobe.acs.commons.referenceproviders;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.reference.Reference;

@RunWith(MockitoJUnitRunner.class)
public class DesignReferenceProviderTest {
    @InjectMocks
    private DesignReferenceProvider instance;
    
    @Mock
    private Resource resource;
    
    
    @Mock
    private ResourceResolver resolver;
    
    @Mock
    private Resource designResource;
    
    @Mock
    private Page designPage;
    
    @Before
    public void setUp() throws Exception {
        instance = new DesignReferenceProvider();
        Map<String, Object> map = new HashMap<String, Object>();
        map.put("cq:designPath", "/etc/designs/geometrixx");
        ValueMap vm =  new ValueMapDecorator(map);
        when(resource.adaptTo(ValueMap.class)).thenReturn(vm);
        when(resource.getResourceResolver()).thenReturn(resolver);
        when(resolver.getResource("/etc/designs/geometrixx")).thenReturn(designResource);
        when(designResource.adaptTo(Page.class)).thenReturn(designPage);
        Calendar cal = GregorianCalendar.getInstance();
        when(designPage.getLastModified()).thenReturn(cal);
        
    }
    
    @Test
    public void testfindReferencesWithDesignPathinResource() {
       
       List<Reference> ref = instance.findReferences(resource);
       assertEquals(1, ref.size());
       assertEquals(false, ref==null);
     }

    
}
