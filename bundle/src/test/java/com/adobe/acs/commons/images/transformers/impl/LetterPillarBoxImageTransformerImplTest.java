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

package com.adobe.acs.commons.images.transformers.impl;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doNothing;

import java.awt.Color;
import java.awt.Dimension;
import java.util.HashMap;
import java.util.Map;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.day.image.Layer;

@RunWith(MockitoJUnitRunner.class)
public class LetterPillarBoxImageTransformerImplTest {

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    private static final int START_WIDTH = 1600;
    private static final int START_HEIGHT = 900;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<>();

    }

    @After
    public void tearDown() throws Exception {
        map = null;
        reset(layer);
    }

    @Test
    public void testTransform_withColor() throws Exception {
        final int width = 400;
        final int height = 225;
        final String color = "ABCDEF";
        final float alpha = 0.5f;

        final int alphaint = Math.round(255 * alpha);
        Color expected = new Color(171, 205, 239, alphaint);

        map.put("width", width);
        map.put("height", height);
        map.put("color", color);
        map.put("alpha", alpha);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        final Transformer transformer = new Transformer(mockLayer);

        transformer.transform(layer, properties);

        assertTrue("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, height);
        verify(mockLayer, times(1)).blit(layer, 0, 0, width, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_withLetterBoxing() throws Exception {
        final int width = 600;
        final int height = 600;

        final int calcHeight = 338;
        final int startPos = (width - calcHeight) / 2;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, calcHeight);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertTrue("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, calcHeight);
        verify(mockLayer, times(1)).blit(layer, 0, startPos, width, calcHeight, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_withPillarBoxing() throws Exception {
        final int width = 600;
        final int height = 600;

        final int calcWidth = 338;
        final int startPos = (width - calcWidth) / 2;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        // Don't get confused, switching the dimensions forces Pillar boxing.
        when(layer.getWidth()).thenReturn(START_HEIGHT, START_HEIGHT, calcWidth);
        when(layer.getHeight()).thenReturn(START_WIDTH, START_WIDTH, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(calcWidth, height);
        verify(mockLayer, times(1)).blit(layer, startPos, 0, calcWidth, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform() throws Exception {
        final int width = 400;
        final int height = 225;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, height);
        verify(mockLayer, times(1)).blit(layer, 0, 0, width, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);

    }

    @Test
    public void testTransform_onlyWidth() throws Exception {
        final int width = 400;
        final int height = 225;

        map.put("width", width);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, height);
        verify(mockLayer, times(1)).blit(layer, 0, 0, width, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_onlyHeight() throws Exception {
        final int width = 400;
        final int height = 225;

        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, height);
        verify(mockLayer, times(1)).blit(layer, 0, 0, width, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_invalidHeightAndWidth() throws Exception {
        final int width = -100;
        final int height = -200;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH);
        when(layer.getHeight()).thenReturn(START_HEIGHT);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(START_WIDTH, START_HEIGHT);
        verify(mockLayer, times(1)).blit(layer, 0, 0, START_WIDTH, START_HEIGHT, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);

    }

    @Test
    public void testTransform_invalidWifth() throws Exception {
        final int width = -100;
        final int height = 225;

        final int targetWidth = 400;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, targetWidth);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(targetWidth, height);
        verify(mockLayer, times(1)).blit(layer, 0, 0, targetWidth, height, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_invalidHeight() throws Exception {
        final int width = 400;
        final int height = -100;

        final int targetheight = 225;

        map.put("width", width);
        map.put("height", height);
        final ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final Transformer transformer = new Transformer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, targetheight);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertTrue("Layer constructor was not intercepted.", transformer.layerCreated);
        verify(layer, times(3)).getWidth();
        verify(layer, times(3)).getHeight();
        verify(layer, times(1)).resize(width, targetheight);
        verify(mockLayer, times(1)).blit(layer, 0, 0, width, targetheight, 0, 0);
        verifyNoMoreInteractions(layer);
        verifyNoMoreInteractions(mockLayer);
    }

    @Test
    public void testTransform_emptyParams() throws Exception {
        ValueMap properties = new ValueMapDecorator(map);


        new LetterPillarBoxImageTransformerImpl().transform(layer, properties);

        verifyNoInteractions(layer);
    }

    @Test
    public void testTransform_nullParams() throws Exception {
        new LetterPillarBoxImageTransformerImpl().transform(layer, null);

        verifyNoInteractions(layer);
    }

    private class Transformer extends LetterPillarBoxImageTransformerImpl {
        private boolean layerCreated;
        private Layer mockLayer;

        private Transformer(Layer mockLayer) {
            this.mockLayer = mockLayer;
        }

        @Override
        Layer createLayer(Dimension size, Color color) {
            layerCreated = true;
            return mockLayer;
        }
    }

}
