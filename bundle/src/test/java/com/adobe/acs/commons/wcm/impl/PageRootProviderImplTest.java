package com.adobe.acs.commons.wcm.impl;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class PageRootProviderImplTest {
    PageRootProviderImpl provider = new PageRootProviderImpl();

    Map<String, Object> config = new HashMap<String, Object>();

    @Test
    public void getRootPagePath() throws Exception {
        config.put("page.root.path", new String[]{"/content/"});
        provider.activate(config);

        assertEquals("/content", provider.getRootPagePath("/content/site/en_us/products/product-x"));
        assertEquals("/content", provider.getRootPagePath("/content/site/en_us/products/product-x/jcr:content/my-component"));
        assertEquals("/content", provider.getRootPagePath("/content/site/en_us"));
        assertEquals("/content", provider.getRootPagePath("/content/"));

        assertNull("/content", provider.getRootPagePath("/content"));
        assertNull("/content", provider.getRootPagePath("/etc/site"));
        assertNull("/content", provider.getRootPagePath("/conf/site"));
    }

    @Test
    public void getRootPagePath_Regex() throws Exception {
        config.put("page.root.path", new String[]{"/content/site/([a-z_-]+)"});
        provider.activate(config);

        assertEquals("/content/site/en_us", provider.getRootPagePath("/content/site/en_us/products/product-x"));
        assertEquals("/content/site/fr", provider.getRootPagePath("/content/site/fr/products/product-x/jcr:content/my-component"));
        assertEquals("/content/site/de_de", provider.getRootPagePath("/content/site/de_de"));

        assertNull(provider.getRootPagePath("/content"));
        assertNull(provider.getRootPagePath("/content/en_us/products"));
        assertNull(provider.getRootPagePath("/content/123/site"));
    }

    @Test
    public void getRootPagePath_Order1() throws Exception {
        config.put("page.root.path", new String[]{"/content", "/content/a"});
        provider.activate(config);

        assertEquals("/content", provider.getRootPagePath("/content/a/b/c"));
    }

    @Test
    public void getRootPagePath_Order2() throws Exception {
        config.put("page.root.path", new String[]{"/content/a", "/content"});
        provider.activate(config);

        assertEquals("/content/a", provider.getRootPagePath("/content/a/b/c"));
        assertEquals("/content", provider.getRootPagePath("/content/b"));
    }
}