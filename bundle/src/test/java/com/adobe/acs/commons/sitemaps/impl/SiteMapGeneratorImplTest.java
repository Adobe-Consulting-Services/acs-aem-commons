package com.adobe.acs.commons.sitemaps.impl;

import static org.mockito.Mockito.mock;

import org.apache.sling.api.resource.ResourceResolver;
import org.mockito.InjectMocks;
import org.w3c.dom.Document;

import com.adobe.acs.commons.sitemaps.SiteMapGenerator;
import com.day.cq.wcm.api.PageManager;

public class SiteMapGeneratorImplTest {

    @InjectMocks
    private SiteMapGenerator siteMapGenerator = new SiteMapGeneratorImpl();
    
    public void testGetSiteMap() throws Exception
    {
        final ResourceResolver resolver = mock(ResourceResolver.class);
        PageManager manager = mock(PageManager.class);
        
        
        
        Document sitemapDocument = siteMapGenerator.getSiteMap(resolver);
        
    }
}
