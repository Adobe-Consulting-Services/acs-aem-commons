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
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import java.awt.Color;
import java.awt.Paint;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import com.day.image.Layer;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LetterPillarBoxImageTransformerImpl.class)
public class LetterPillarBoxImageTransformerImplTest {
    LetterPillarBoxImageTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    private static final int START_WIDTH = 1600;
    private static final int START_HEIGHT = 900;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new LetterPillarBoxImageTransformerImpl();

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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = new AtomicReference<Layer>();
        PowerMockito.whenNew(Layer.class).withParameterTypes(int.class, int.class, Paint.class)
                .withArguments(anyInt(), anyInt(), Matchers.eq(expected)).thenAnswer(new Answer<Layer>() {
                    @Override
                    public Layer answer(InvocationOnMock invocation) throws Throwable {
                        resultLayer.set(mockLayer);
                        return mockLayer;
                    }
                });

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, calcHeight);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        // Don't get confused, switching the dimensions forces Pillar boxing.
        when(layer.getWidth()).thenReturn(START_HEIGHT, START_HEIGHT, calcWidth);
        when(layer.getHeight()).thenReturn(START_WIDTH, START_WIDTH, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH);
        when(layer.getHeight()).thenReturn(START_HEIGHT);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, targetWidth);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, height);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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
        ValueMap properties = new ValueMapDecorator(map);

        final Layer mockLayer = mock(Layer.class);

        final AtomicReference<Layer> resultLayer = setupMockLayer(mockLayer);

        when(layer.getWidth()).thenReturn(START_WIDTH, START_WIDTH, width);
        when(layer.getHeight()).thenReturn(START_HEIGHT, START_HEIGHT, targetheight);

        doNothing().when(mockLayer).blit(any(Layer.class), anyInt(), anyInt(), anyInt(), anyInt(), anyInt(), anyInt());

        transformer.transform(layer, properties);

        assertNotNull("Layer constructor was not intercepted.", resultLayer.get());
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

        transformer.transform(layer, properties);

        verifyZeroInteractions(layer);
    }

    @Test
    public void testTransform_nullParams() throws Exception {
        transformer.transform(layer, null);

        verifyZeroInteractions(layer);
    }

    private AtomicReference<Layer> setupMockLayer(final Layer mockLayer) throws Exception {
        final AtomicReference<Layer> resultLayer = new AtomicReference<Layer>();
        PowerMockito.whenNew(Layer.class).withParameterTypes(int.class, int.class, Paint.class)
                .withArguments(anyInt(), anyInt(), Matchers.isA(Paint.class)).thenAnswer(new Answer<Layer>() {
                    @Override
                    public Layer answer(InvocationOnMock invocation) throws Throwable {
                        resultLayer.set(mockLayer);
                        return mockLayer;
                    }
                });
        return resultLayer;
    }

}
