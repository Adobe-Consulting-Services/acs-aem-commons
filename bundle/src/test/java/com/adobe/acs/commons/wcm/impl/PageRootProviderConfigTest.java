package com.adobe.acs.commons.wcm.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;


public class PageRootProviderConfigTest {
    PageRootProviderConfig config = null;

    Map<String, Object> properties = new HashMap<String, Object>();
    
    @Before
    public final void setUp() throws Exception {
        config = new PageRootProviderConfig();
    }

    @Test
    public void getPageRootPatterns() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content"});
        config.activate(properties);
        
        assertEquals(Arrays.asList("^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Regex() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/site/([a-z_-]+)"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/site/([a-z_-]+))(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_RegexEnd() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/site/[a-z]{2}"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/site/[a-z]{2})(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order1() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content", "/content/a"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content)(|/.*)$", "^(/content/a)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Order2() throws Exception {
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
    }
    
    
    @Test
    public void getPageRootPatterns_Deactivate() throws Exception {
    	assertNull(config.getPageRootPatterns());
    	
        properties.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        config.activate(properties);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), toStringList(config.getPageRootPatterns()));
        
        config.deactivate();
        assertNull(config.getPageRootPatterns());
    }
    
    
    static List<String> toStringList(final List<Pattern> patterns) {
    	List<String> list = new ArrayList<String>();
    	
    	for (Pattern p : patterns) {
    		list.add(p.toString());
    	}
    	
    	return list;
    }
}