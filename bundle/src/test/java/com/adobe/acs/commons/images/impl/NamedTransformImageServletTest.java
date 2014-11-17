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

import com.adobe.acs.commons.images.ImageTransformer;
import com.adobe.acs.commons.images.NamedImageTransformer;
import com.day.image.Layer;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NamedTransformImageServletTest {

    final String NAMED_TRANSFORM_FEATURE = "feature";
    final String NAMED_TRANSFORM_SMALL = "small";

    final String IMAGE_TRANSFORM_RESIZE = "resize";
    final String IMAGE_TRANSFORM_GREYSCALE = "greyscale";

    @Spy
    private FeaturedNamedImageTransformer featureImageTransformer = new FeaturedNamedImageTransformer();

    @Spy
    private SmallNamedImageTransformer smallImageTransformer = new SmallNamedImageTransformer();

    @Spy
    private Map<String, NamedImageTransformer> namedImageTransformers = new HashMap<String, NamedImageTransformer>();

    @Spy
    private EmptyImageTransformer resizeImageTransformer = new EmptyImageTransformer();

    @Spy
    private EmptyImageTransformer greyscaleImageTransformer = new EmptyImageTransformer();

    @Spy
    private Map<String, ImageTransformer> imageTransformers = new HashMap<String, ImageTransformer>();

    @InjectMocks
    private NamedTransformImageServlet servlet;

    private MockSlingHttpServletRequest mockRequest;

    @Before
    public void setUp() throws Exception {
        servlet = new NamedTransformImageServlet();

        imageTransformers.put(IMAGE_TRANSFORM_RESIZE, resizeImageTransformer);
        imageTransformers.put(IMAGE_TRANSFORM_GREYSCALE, greyscaleImageTransformer);

        namedImageTransformers.put(NAMED_TRANSFORM_FEATURE, featureImageTransformer);
        namedImageTransformers.put(NAMED_TRANSFORM_SMALL, smallImageTransformer);

        mockRequest = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                NAMED_TRANSFORM_FEATURE + "/" + new Random().nextInt() + "/image.png",
                "");

        MockitoAnnotations.initMocks(this);
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
                NAMED_TRANSFORM_FEATURE + "/" + new Random().nextInt() + "/foo",
                "");

        final boolean result = servlet.accepts(request);

        assertFalse(result);
    }

    @Test
    public void testAccepts_multipleTransforms() throws Exception {
        MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                NAMED_TRANSFORM_FEATURE + "/" + NAMED_TRANSFORM_SMALL + "/image.png",
                "");

        final boolean result = servlet.accepts(request);

        assertTrue(result);
    }

    @Test
    public void test_getImageTransformersWithParams() {

        List<NamedImageTransformer> selectedNamedImageTransformers = new ArrayList<NamedImageTransformer>();
        selectedNamedImageTransformers.add(featureImageTransformer);
        selectedNamedImageTransformers.add(smallImageTransformer);

        final ValueMap imageTransformersWithParams = servlet.getImageTransformersWithParams(selectedNamedImageTransformers);

        assertEquals(IMAGE_TRANSFORM_RESIZE, imageTransformersWithParams.keySet().toArray()[0]);
        ValueMap resize = (ValueMap) imageTransformersWithParams.values().toArray()[0];
        assertEquals("width", resize.keySet().toArray()[0]);
        assertEquals("10", resize.values().toArray()[0]);
        assertEquals(1, resize.keySet().size());

        assertEquals(IMAGE_TRANSFORM_GREYSCALE, imageTransformersWithParams.keySet().toArray()[1]);
        ValueMap greyscale = (ValueMap) imageTransformersWithParams.values().toArray()[1];
        assertEquals("greyscale", greyscale.keySet().toArray()[0]);
        assertEquals("true", greyscale.values().toArray()[0]);
    }

    @Test
    public void test_multipleTransforms() throws Exception {
        List<NamedImageTransformer> selectedNamedImageTransformers = new ArrayList<NamedImageTransformer>();
        selectedNamedImageTransformers.add(featureImageTransformer);
        selectedNamedImageTransformers.add(smallImageTransformer);

        final ValueMap imageTransformersWithParams = servlet.getImageTransformersWithParams(selectedNamedImageTransformers);

        servlet.transform(mock(Layer.class), imageTransformersWithParams);

        org.mockito.Mockito.verify(resizeImageTransformer, times(1)).transform(any(Layer.class), any(ValueMap.class));
        org.mockito.Mockito.verify(greyscaleImageTransformer, times(1)).transform(any(Layer.class), any(ValueMap.class));
    }

    /* Testing for resolveImage requires too much orchestration/mocking to be useful */
}
