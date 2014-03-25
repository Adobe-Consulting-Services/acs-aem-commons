package com.adobe.acs.commons.config.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.day.cq.wcm.api.Page;

@RunWith(MockitoJUnitRunner.class)
public class ConfigurationImplTest {

    @Mock
    private Page page;
    
    @Mock
    private Resource contentResource;
    @Mock
    private Resource gridResource;
    
    @Mock
    private Resource rowResource1;

    @Mock
    private Resource rowResource2;
    
    @Mock
    private Iterator<Resource> iter;
    
    private String gridNodeName ="grid";
    
    private String loginPath ="/content/geometrixx/login";
    
    private String loginPath1 ="/content/geometrixx_outdoors/login";    
   
    
    private String gridPath = "/etc/geometrixx/config/jcr:content/grid";
    
  
    private ConfigurationImpl config;
    
    private ValueMap map ;
    
    
    @Before
    public void setup() {
        when(page.getParent()).thenReturn(null);
        when(page.getContentResource()).thenReturn(contentResource);
        when(contentResource.getChild(gridNodeName)).thenReturn(gridResource);
        when(gridResource.getPath()).thenReturn(gridPath);
        when(gridResource.listChildren()).thenReturn(iter);
        when(iter.hasNext()).thenReturn(true, false);
        when(iter.next()).thenReturn(rowResource1);
        Map<String , Object> hmap = new HashMap<String, Object>();
        hmap.put("key","loginPath");
        hmap.put("value",loginPath);
        map = new ValueMapDecorator(hmap);
        when(rowResource1.adaptTo(ValueMap.class)).thenReturn(map);
        config = new ConfigurationImpl(page);
    }
    
    
    @Test
    public void testGetRowByKey() {
       Map<String,String> result = config.getRowByKey("loginPath");
       assertNotNull(result);
       assertEquals(loginPath, result.get("value"));
       assertEquals(2, result.size());
    }
    @Test
    public void testGetRowByKeyNotPresent() {
       Map<String,String> result = config.getRowByKey("loginPath1");
       assertNotNull(result);
       assertEquals(0, result.size());
     }
    @Test
    public void testGetRowsByKey() {
        when(iter.hasNext()).thenReturn(true,true, false);
        when(iter.next()).thenReturn(rowResource1,rowResource2);
        Map<String , Object> hmap = new HashMap<String, Object>();
        hmap.put("key","loginPath");
        hmap.put("value",loginPath1);
      ValueMap  map1 = new ValueMapDecorator(hmap);
        when(rowResource2.adaptTo(ValueMap.class)).thenReturn(map1);
       List<Map<String,String>> result = config.getRowsByKey("loginPath");
       assertNotNull(result);
       assertEquals(loginPath, result.get(0).get("value"));
       assertEquals(loginPath1, result.get(1).get("value"));
       assertEquals(2, result.size());
    }
    @Test
    public void testGetRowsByKeyNotPresent() {
       List<Map<String,String>> result = config.getRowsByKey("loginPath1");
       assertNotNull(result);
       assertEquals(0, result.size());
     }

}
