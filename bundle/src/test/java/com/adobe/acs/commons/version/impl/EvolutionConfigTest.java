package com.adobe.acs.commons.version.impl;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class EvolutionConfigTest {

    public EvolutionConfigTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test
    public void testHandleProperties() {
        String[] ignoreProps = { "jcr:.*", "cq:.*", "par/cq:.*" };
        String[] ignoreRes = {};

        EvolutionConfig config = new EvolutionConfig(ignoreProps, ignoreRes);
        assertEquals(false, config.handleProperty("jcr:title"));
        assertEquals(true, config.handleProperty("test/jcr:title"));
        assertEquals(false, config.handleProperty("cq:name"));
        assertEquals(true, config.handleProperty("test/cq:name"));
        assertEquals(false, config.handleProperty("par/cq:name"));
    }

    @Test
    public void testHandleResources() {
        String[] ignoreProps = {};
        String[] ignoreRes = { "image", "par/image", ".*test.*" };

        EvolutionConfig config = new EvolutionConfig(ignoreProps, ignoreRes);
        assertEquals(false, config.handleResource("image"));
        assertEquals(false, config.handleResource("par/image"));
        assertEquals(true, config.handleResource("bert/image"));
        assertEquals(false, config.handleResource("test"));
        assertEquals(false, config.handleResource("tester"));
        assertEquals(false, config.handleResource("par/test"));
        assertEquals(false, config.handleResource("par/testen"));
    }

}
