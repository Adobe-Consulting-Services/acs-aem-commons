/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */

package com.adobe.acs.commons.images.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.sling.api.request.RequestPathInfo;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.testing.sling.MockSlingHttpServletRequest;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.*;
import java.util.*;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import com.adobe.acs.commons.images.ImageTransformer;
import com.adobe.acs.commons.images.NamedImageTransformer;
import com.day.image.Layer;
import org.apache.sling.api.resource.Resource;

@RunWith(MockitoJUnitRunner.class)
public final class NamedTransformImageServletTest {
    static final String TIFF_ORIENTATION = "tiff:Orientation";
    static final String JCR_CONTENT_METADATA = "jcr:content/metadata";

    private static final String NAMED_TRANSFORM_FEATURE = "feature";
    private static final String NAMED_TRANSFORM_SMALL = "small";

    private static final String IMAGE_TRANSFORM_RESIZE = "resize";
    private static final String IMAGE_TRANSFORM_GREYSCALE = "greyscale";

    @Spy
    private final FeaturedNamedImageTransformer featureImageTransformer = new FeaturedNamedImageTransformer();

    @Spy
    private final SmallNamedImageTransformer smallImageTransformer = new SmallNamedImageTransformer();

    @Spy
    private final EmptyImageTransformer resizeImageTransformer = new EmptyImageTransformer();

    @Spy
    private final EmptyImageTransformer greyscaleImageTransformer = new EmptyImageTransformer();

    @Spy
    private final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
                "/path",
                "",
                "transform",
                NAMED_TRANSFORM_FEATURE + "/" + new Random().nextInt() + "/image.png",
                "");

    private final NamedTransformImageServlet servlet = new NamedTransformImageServlet();

    @Mock
    private Layer layer;

    @Mock
    private Rectangle rectangle;

    @Mock
    private Resource mockImageResource;

    @Mock
    private Resource metadataResource;

    private ValueMap metadataValueMap;

    @Before
    public void setUp() {
        final Map<Object, Object> props = new HashMap<>();
        servlet.bindImageTransformers(null, props);
        servlet.bindNamedImageTransformers(null, props);

        props.put(ImageTransformer.PROP_TYPE, IMAGE_TRANSFORM_RESIZE);
        servlet.bindImageTransformers(resizeImageTransformer, props);

        props.put(ImageTransformer.PROP_TYPE, IMAGE_TRANSFORM_GREYSCALE);
        servlet.bindImageTransformers(greyscaleImageTransformer, props);

        props.put(NamedImageTransformer.PROP_NAME, NAMED_TRANSFORM_FEATURE);
        servlet.bindNamedImageTransformers(featureImageTransformer, props);

        props.put(NamedImageTransformer.PROP_NAME, NAMED_TRANSFORM_SMALL);
        servlet.bindNamedImageTransformers(smallImageTransformer, props);
    }

    @Test
    public void testBinders() {
        final Map<Object, Object> props = new HashMap<>();

        servlet.bindImageTransformers(null, props);
        servlet.bindNamedImageTransformers(null, props);

        servlet.unbindImageTransformers(null, props);
        servlet.unbindNamedImageTransformers(null, props);

        props.put(ImageTransformer.PROP_TYPE, IMAGE_TRANSFORM_RESIZE);
        servlet.unbindImageTransformers(resizeImageTransformer, props);

        props.put(NamedImageTransformer.PROP_NAME, NAMED_TRANSFORM_FEATURE);
        servlet.unbindNamedImageTransformers(featureImageTransformer, props);

        assertFalse(servlet.accepts(request));
    }

    @Test
    public void testAccepts() {
        assertTrue(servlet.accepts(request));
        assertFalse(servlet.accepts(null));

        final RequestPathInfo rpi = Mockito.mock(RequestPathInfo.class);
        when(rpi.getSuffix()).thenReturn(null);
        when(request.getRequestPathInfo()).thenReturn(rpi);
        assertFalse(servlet.accepts(request));
    }

    @Test
    public void testAccepts_invalidNamedTransform() {
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
    public void testAccepts_invalidLastSuffix() {
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
    public void testAccepts_multipleTransforms() {
        final MockSlingHttpServletRequest request = new MockSlingHttpServletRequest(
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
    public void test_multipleTransforms() {
        List<NamedImageTransformer> selectedNamedImageTransformers = new ArrayList<NamedImageTransformer>();
        selectedNamedImageTransformers.add(featureImageTransformer);
        selectedNamedImageTransformers.add(smallImageTransformer);

        final ValueMap imageTransformersWithParams = servlet.getImageTransformersWithParams(selectedNamedImageTransformers);

        servlet.transform(mock(Layer.class), imageTransformersWithParams, mockRequest);

        verify(resizeImageTransformer, times(1)).transform(any(Layer.class), any(ValueMap.class));
        verify(greyscaleImageTransformer, times(1)).transform(any(Layer.class), any(ValueMap.class));
    }

    @Test
    public void test_getQuality() {
        ValueMap qualityTransforms = new ValueMapDecorator(new HashMap<String, Object>());

        qualityTransforms.put("quality", 0);
        assertEquals(0D, servlet.getQuality("image/jpg", qualityTransforms), 0);

        qualityTransforms.put("quality", 100);
        assertEquals(1D, servlet.getQuality("image/jpg", qualityTransforms), 0);

        qualityTransforms.put("quality", 50);
        assertEquals(0.5D, servlet.getQuality("image/jpg", qualityTransforms), 0);

        qualityTransforms.put("quality", 101);
        assertEquals(.82D, servlet.getQuality("image/jpg", qualityTransforms), 0);

        qualityTransforms.put("quality", -1);
        assertEquals(.82D, servlet.getQuality("image/jpg", qualityTransforms), 0);

        /* Gifs */
        
        qualityTransforms.put("quality", 0);
        assertEquals(0D, servlet.getQuality("image/gif", qualityTransforms), 0);

        qualityTransforms.put("quality", 100);
        assertEquals(255D, servlet.getQuality("image/gif", qualityTransforms), 0);

        qualityTransforms.put("quality", 50);
        assertEquals(127.5D, servlet.getQuality("image/gif", qualityTransforms), 0);

        qualityTransforms.put("quality", 101);
        assertEquals(209.1D, servlet.getQuality("image/gif", qualityTransforms), 0);

        qualityTransforms.put("quality", -1);
        assertEquals(209.1D, servlet.getQuality("image/gif", qualityTransforms), 0);

    }

    @Test
    public void test_prcoessImageOrientation_imageMetadataValueMapIsNull() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(null);

        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verifyZeroInteractions(layer);
    }

    @Test
    public void test_prcoessImageOrientation_noOrientationMetadata() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);

        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verifyZeroInteractions(layer);
    }

    @Test
    public void test_prcoessImageOrientation_normalOrientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("1");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verifyZeroInteractions(layer);
    }

    @Test
    public void test_prcoessImageOrientation_mirrorHorizontalOrientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("2");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).flipHorizontally();
    }

    @Test
    public void test_prcoessImageOrientation_rotate180Orientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("3");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).rotate(180);
    }

    @Test
    public void test_prcoessImageOrientation_mirrorVerticalOrientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("4");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).flipVertically();
    }

    @Test
    public void test_prcoessImageOrientation_mirrorHorizontalRotate279Orientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("5");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).flipHorizontally();
        verify(layer, times(1)).rotate(270);
    }

    @Test
    public void test_prcoessImageOrientation_rotate90Orientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("6");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).rotate(90);
    }

    @Test
    public void test_prcoessImageOrientation_mirrorHorizontalRotate90Orientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("7");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).flipHorizontally();
        verify(layer, times(1)).rotate(90);
    }

    @Test
    public void test_prcoessImageOrientation_rotate270Orientation() throws Exception {
        when(mockImageResource.getChild(JCR_CONTENT_METADATA)).thenReturn(metadataResource);
        initValueMap("8");
        when(metadataResource.adaptTo(ValueMap.class)).thenReturn(metadataValueMap);
        Layer layer = mock(Layer.class);
        servlet.processImageOrientation(mockImageResource, layer);

        verify(layer, times(1)).rotate(270);
    }

   
    public void test_isProgressiveJpeg() {
        ValueMap progressiveTransforms = new ValueMapDecorator(new HashMap<String, Object>());

        // Disabled

        progressiveTransforms.put("enabled", false);
        assertFalse(servlet.isProgressiveJpeg("image/png", progressiveTransforms));

        progressiveTransforms.put("enabled", false);
        assertFalse(servlet.isProgressiveJpeg("image/jpg", progressiveTransforms));

        progressiveTransforms.put("enabled", false);
        assertFalse(servlet.isProgressiveJpeg("image/jpeg", progressiveTransforms));

        // Enabled

        progressiveTransforms.put("enabled", true);
        assertFalse(servlet.isProgressiveJpeg("image/png", progressiveTransforms));

        progressiveTransforms.put("enabled", true);
        assertTrue(servlet.isProgressiveJpeg("image/jpg", progressiveTransforms));

        progressiveTransforms.put("enabled", true);
        assertTrue(servlet.isProgressiveJpeg("image/jpeg", progressiveTransforms));

        // Invalid

        progressiveTransforms.put("enabled", true);
        assertFalse(servlet.isProgressiveJpeg(null, progressiveTransforms));

        progressiveTransforms.put("enabled", true);
        assertFalse(servlet.isProgressiveJpeg("", progressiveTransforms));

        progressiveTransforms.remove("enabled");
        assertFalse(servlet.isProgressiveJpeg("", progressiveTransforms));

    }

    private void initValueMap(String num) {
        Map<String, Object> metadataMap = new HashMap<String, Object>();
        metadataMap.put(TIFF_ORIENTATION, num);
        metadataValueMap = new ValueMapDecorator(metadataMap);
    }

    /* Testing for resolveImage requires too much orchestration/mocking to be useful */
}
