package com.adobe.acs.commons.util;

import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class PathInfoUtilTest {

    public PathInfoUtilTest() {
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

    /**
     * Test of getQueryParam method, of class PathInfoUtil.
     */
    @Test
    public void testGetQueryParam_HttpServletRequest_String() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show", "html", "simple", "ghz=2.4");

        String key = "ghz";
        String expResult = "2.4";
        String result = PathInfoUtil.getQueryParam(request, key);
        //assertEquals(expResult, result);
    }

    /**
     * Test of getQueryParam method, of class PathInfoUtil.
     */
    @Test
    public void testGetQueryParam_withDefault() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show", "html", "simple", "cpu=i7&ghz=2.4");

        String key = "ghz";
        String expResult = "2.4";
        String result = PathInfoUtil.getQueryParam(request, key, "3");
        //assertEquals(expResult, result);

        key = "doesnt-exist";
        expResult = "3";
        result = PathInfoUtil.getQueryParam(request, key, "3");
        //assertEquals(expResult, result);

    }

    /**
     * Test of getSelector method, of class PathInfoUtil.
     */
    @Test
    public void testGetSelector() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test", "html", "simple", "cpu=i7&ghz=2.4");

        String expResult = "show";
        String result = PathInfoUtil.getSelector(request, 0);
        assertEquals(expResult, result);

        expResult = "test";
        result = PathInfoUtil.getSelector(request, 1);
        assertEquals(expResult, result);

        result = PathInfoUtil.getSelector(request, -1);
        assertNull(result);

        result = PathInfoUtil.getSelector(request, 10);
        assertNull(result);
    }

    /**
     * Test of getSuffixSegment method, of class PathInfoUtil.
     */
    @Test
    public void testGetSuffixSegment() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test", "html", "super/simple", "cpu=i7&ghz=2.4");

        String expResult = "super";
        String result = PathInfoUtil.getSuffixSegment(request, 0);
        assertEquals(expResult, result);

        expResult = "simple";
        result = PathInfoUtil.getSuffixSegment(request, 1);
        assertEquals(expResult, result);

        result = PathInfoUtil.getSuffixSegment(request, -1);
        assertNull(result);

        result = PathInfoUtil.getSuffixSegment(request, 10);
        assertNull(result);
    }

    /**
     * Test of getSuffix method, of class PathInfoUtil.
     */
    @Test
    public void testGetSuffix() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test", "html", "super/simple", "cpu=i7&ghz=2.4");

        String expResult = "super/simple";
        String result = PathInfoUtil.getSuffix(request);
        assertEquals(expResult, result);
    }
}
