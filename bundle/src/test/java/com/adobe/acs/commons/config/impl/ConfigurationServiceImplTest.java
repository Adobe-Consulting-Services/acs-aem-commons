package com.adobe.acs.commons.config.impl;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
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

import com.adobe.acs.commons.config.Configuration;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;


@RunWith(MockitoJUnitRunner.class)
public class ConfigurationServiceImplTest {
    
    @InjectMocks
    private ConfigurationServiceImpl configservice = new ConfigurationServiceImpl();

    @Mock
    private Page page;
    
    @Mock
    private Page configPage;

    @Mock
    private Page noconfigPage;
    
    @Mock
    private PageManager manager;
    
    @Mock
    private Resource contentResource;
    
    @Mock
    private Resource gridResource;
    
    private ValueMap map;
    
    private String path ="/etc/geometrixx/config";
    
    private String nopath ="/etc/geometrixx/config1";
    
    private String gridPath = "/etc/geometrixx/config/jcr:content/grid";
    
    @Before
    public void setup() {
        when(page.getPageManager()).thenReturn(manager);
        when(noconfigPage.getPageManager()).thenReturn(manager);
        when(manager.getPage(path)).thenReturn(configPage);
        when(page.getContentResource()).thenReturn(contentResource);
        when(configPage.getContentResource()).thenReturn(contentResource);
        when(contentResource.getChild("grid")).thenReturn(gridResource );
        when(gridResource.getPath()).thenReturn(gridPath);
        Map<String , Object> hmap = new HashMap<String, Object>();
        hmap.put("configPage",path);
        map = new ValueMapDecorator(hmap);
        when(contentResource.adaptTo(ValueMap.class)).thenReturn(map);
    }
    
    @Test
    public void testgetConfigurationFromPagePropertiesWithPagePropertyPresent() {
       Configuration configPage = configservice.getConfiguration(page);
       assertNotNull(configPage);
       assertEquals(configPage.getClass().getName(), ConfigurationImpl.class.getName());
    }
    @Test
    public void testgetConfigurationFromPagePropertiesWithPagePropertyNotPresent() {
       Configuration configPage = configservice.getConfiguration(noconfigPage);
       assertNotNull(configPage);
       assertEquals(configPage.getClass().getName(), NullConfigurationImpl.class.getName());
    }
    
    @Test
    public void testgetConfigurationFromParameter() {
       Configuration configPage = configservice.getConfiguration(page,path);
       assertNotNull(configPage);
       assertEquals(configPage.getClass().getName(), ConfigurationImpl.class.getName());
    }
    
    @Test
    public void testgetConfigurationFromParameterPageNotPresent() {
       Configuration configPage = configservice.getConfiguration(noconfigPage,nopath);
       assertNotNull(configPage);
       assertEquals(configPage.getClass().getName(), NullConfigurationImpl.class.getName());
    }
}
