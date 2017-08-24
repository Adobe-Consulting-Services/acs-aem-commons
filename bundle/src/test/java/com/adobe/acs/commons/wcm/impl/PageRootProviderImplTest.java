package com.adobe.acs.commons.wcm.impl;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

@Deprecated
public class PageRootProviderImplTest {
    PageRootProviderImpl provider = new PageRootProviderImpl();

    Map<String, Object> config = new HashMap<String, Object>();

    @Test
    public void getPageRootPatterns() throws Exception {
        config.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content"});
        provider.activate(config);

        assertEquals(Arrays.asList("^(/content)(|/.*)$"), PageRootProviderConfigTest.toStringList(provider.getPageRootPatterns()));
    }

    @Test
    public void getPageRootPatterns_Deactivate() throws Exception {
        assertNull(provider.getPageRootPatterns());

        config.put(PageRootProviderConfig.PAGE_ROOT_PATH, new String[]{"/content/a", "/content"});
        provider.activate(config);

        assertEquals(Arrays.asList("^(/content/a)(|/.*)$", "^(/content)(|/.*)$"), PageRootProviderConfigTest.toStringList(provider.getPageRootPatterns()));

        provider.deactivate();
        assertNull(provider.getPageRootPatterns());
    }
}
