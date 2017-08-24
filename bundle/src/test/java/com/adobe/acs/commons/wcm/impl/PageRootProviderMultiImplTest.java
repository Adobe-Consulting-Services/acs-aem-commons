package com.adobe.acs.commons.wcm.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

public class PageRootProviderMultiImplTest {
	
    @Mock
    private PageRootProviderConfig config1;
	
    @Mock
    private PageRootProviderConfig config2;
    
    PageRootProviderMultiImpl provider = null;

    @Before
    public final void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        provider = new PageRootProviderMultiImpl();
    }

    @Test
    public void getRootPagePath() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content")));
        provider.bindConfig(config1);    	
    	provider.activate();        

        assertEquals("/content", provider.getRootPagePath("/content/site/en_us/products/product-x"));
        assertEquals("/content", provider.getRootPagePath("/content/site/en_us/products/product-x/jcr:content/my-component"));
        assertEquals("/content", provider.getRootPagePath("/content/site/en_us"));
        assertEquals("/content", provider.getRootPagePath("/content/"));
        assertEquals("/content", provider.getRootPagePath("/content"));

        assertNull("/content", provider.getRootPagePath("/etc/site"));
        assertNull("/content", provider.getRootPagePath("/conf/site"));
    }

    @Test
    public void getRootPagePath_Regex() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/site/([a-z_-]+)")));
        provider.bindConfig(config1);
    	provider.activate();       

        assertEquals("/content/site/en_us", provider.getRootPagePath("/content/site/en_us/products/product-x"));
        assertEquals("/content/site/fr", provider.getRootPagePath("/content/site/fr/products/product-x/jcr:content/my-component"));
        assertEquals("/content/site/de_de", provider.getRootPagePath("/content/site/de_de"));

        assertNull(provider.getRootPagePath("/content"));
        assertNull(provider.getRootPagePath("/content/en_us/products"));
        assertNull(provider.getRootPagePath("/content/123/site"));
    }

    @Test
    public void getRootPagePath_RegexEnd() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/site/[a-z]{2}")));
        provider.bindConfig(config1);
        provider.activate();       

        assertEquals("/content/site/en", provider.getRootPagePath("/content/site/en/products/product-x"));
        assertEquals("/content/site/de", provider.getRootPagePath("/content/site/de"));

        assertNull(provider.getRootPagePath("/content/site/en_us/products"));
        assertNull(provider.getRootPagePath("/content/site/somewhereelse"));
    }

    @Test
    public void getRootPagePath_Order1() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content"), buildPattern("/content/a")));
        provider.bindConfig(config1);
    	provider.activate();

        assertEquals("/content", provider.getRootPagePath("/content/a/b/c"));
    }

    @Test
    public void getRootPagePath_Order2() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/a"), buildPattern("/content")));
        provider.bindConfig(config1);
        provider.activate();

        assertEquals("/content/a", provider.getRootPagePath("/content/a/b/c"));
        assertEquals("/content", provider.getRootPagePath("/content/b"));
    }
    
    @Test
    public void getRootPagePath_MultiOrder1() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content")));
        provider.activate();
    	when(config2.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/a")));
        provider.bindConfig(config1);
        provider.bindConfig(config2);        

        assertEquals("/content", provider.getRootPagePath("/content/a/b/c"));
    }

    @Test
    public void getRootPagePath_MultiOrder2() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/a")));
        provider.activate();
    	when(config2.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content")));    	
        provider.bindConfig(config1);
        provider.bindConfig(config2);

        assertEquals("/content/a", provider.getRootPagePath("/content/a/b/c"));
        assertEquals("/content", provider.getRootPagePath("/content/b"));
    }

    
    @Test
    public void getRootPagePath_Unbind() throws Exception {
    	when(config1.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content/a")));
        provider.activate();    	
    	when(config2.getPageRootPatterns()).thenReturn(Arrays.asList(buildPattern("/content")));
        provider.bindConfig(config1);
        provider.bindConfig(config2);

        assertEquals("/content/a", provider.getRootPagePath("/content/a/b/c"));
        assertEquals("/content", provider.getRootPagePath("/content/b"));
        
        provider.unbindConfig(config1);
        
        assertEquals("/content", provider.getRootPagePath("/content/a/b/c"));
        assertEquals("/content", provider.getRootPagePath("/content/b"));        
    }

    
    private Pattern buildPattern(final String regex) {
    	return Pattern.compile("^(" + regex + ")(|/.*)$");
    }
}