/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */
package com.adobe.acs.commons.util;

import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.*;

import static org.junit.Assert.assertArrayEquals;
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
        // MockSlingHttpServletRequest doesn't natively support getParameter
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show", "html", "simple", null) {
            public String getParameter(String name) {
                if (name.equals("ghz")) {
                    return "2.4";
                } else {
                    return null;
                }
            };
        };

        String key = "ghz";
        String expResult = "2.4";
        String result = PathInfoUtil.getQueryParam(request, key);
        assertEquals(expResult, result);
    }

    /**
     * Test of getQueryParam method, of class PathInfoUtil.
     */
    @Test
    public void testGetQueryParam_withDefault() {
        // MockSlingHttpServletRequest doesn't natively support getParameter
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show", "html", "simple", "cpu=i7&ghz=2.4") {
            public String getParameter(String name) {
                if (name.equals("ghz")) {
                    return "2.4";
                } else {
                    return null;
                }
            };
        };

        String key = "ghz";
        String expResult = "2.4";
        String result = PathInfoUtil.getQueryParam(request, key, "3");
        assertEquals(expResult, result);

        key = "doesnt-exist";
        expResult = "3";
        result = PathInfoUtil.getQueryParam(request, key, "3");
        assertEquals(expResult, result);

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

    @Test
    public void testGetSuffixSegments() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test", "html", "super/simple", "cpu=i7&ghz=2.4");

        String[] expResult = new String[] { "super", "simple" };
        String[] result = PathInfoUtil.getSuffixSegments(request);
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testGetSuffixSegments_empty() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test", "html", "", "cpu=i7&ghz=2.4");

        String[] expResult = new String[] { };
        String[] result = PathInfoUtil.getSuffixSegments(request);
        assertArrayEquals(expResult, result);
    }

    @Test
    public void testGetFirstSuffixSegments() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test",
                "html", "first/second", "cpu=i7&ghz=2.4");

        String expResult = "first";
        String result = PathInfoUtil.getFirstSuffixSegment(request);
        assertEquals(expResult, result);
    }

    @Test
    public void testGetLastSuffixSegments() {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest("/apple/macbookair", "show.test",
                "html", "first/second/third", "cpu=i7&ghz=2.4");

        String expResult = "third";
        String result = PathInfoUtil.getLastSuffixSegment(request);
        assertEquals(expResult, result);
    }
}