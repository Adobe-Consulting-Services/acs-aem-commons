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

package com.adobe.acs.commons.images.impl;

import com.adobe.acs.commons.images.NamedImageTransformer;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class NamedTransformImageServletTest {
    final String TEST_TRANSFORM_NAME = "test";

    @Spy
    private NamedImageTransformer mockNamedImageTransformer = new StaticNamedImageTransformer();

    @Spy
    private Map<String, NamedImageTransformer> namedImageTransformers = new HashMap<String, NamedImageTransformer>();;

    @InjectMocks
    private NamedTransformImageServlet servlet;

    private MockSlingHttpServletRequest mockRequest;

    @Before
    public void setUp() throws Exception {
        servlet = new NamedTransformImageServlet();

        namedImageTransformers.put(TEST_TRANSFORM_NAME, mockNamedImageTransformer);

        mockRequest = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                TEST_TRANSFORM_NAME + "/" + new Random().nextInt() + "/image.png",
                "");

        MockitoAnnotations.initMocks(this);
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void testAccepts() throws Exception {
        final boolean result = servlet.accepts(mockRequest);

        assertTrue(result);
    }

    @Test
    public void testAccepts_invalidNamedTransform() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                "unknown/image.png",
                "");

        final boolean result = servlet.accepts(request);

        assertFalse(result);
    }

    @Test
    public void testAccepts_invalidLastSuffix() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                TEST_TRANSFORM_NAME + "/" + new Random().nextInt() + "/foo",
                "");

        final boolean result = servlet.accepts(request);

        assertFalse(result);
    }

    /* Testing for resolveImage requires too much orchestration/mocking to be useful */
}
