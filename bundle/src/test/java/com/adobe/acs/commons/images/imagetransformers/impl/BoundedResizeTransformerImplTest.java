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

package com.adobe.acs.commons.images.imagetransformers.impl;

import com.adobe.acs.commons.images.transformers.impl.BoundedResizeTransformerImpl;
import com.day.image.Layer;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BoundedResizeTransformerImplTest {

    BoundedResizeTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new BoundedResizeTransformerImpl();
    }

    @After
    public void tearDown() throws Exception {
        map = null;
        reset(layer);
    }

    @Test
    public void testTransform_Landscape_only_width() throws Exception {
        prepareLandscape();

        map.put("width", 200);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(200, 131);
    }

    @Test
    public void testTransform_Landscape_to_Landscape_Ratio() throws Exception {
        prepareLandscape();

        map.put("width", 200);
        map.put("height", 10);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(15, 10);
    }

    @Test
    public void testTransform_Landscape_to_Portrait_Ratio() throws Exception {
        prepareLandscape();

        map.put("width", 10);
        map.put("height", 200);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(10, 7);
    }

    @Test
    public void testTransform_Portrait_to_Landscape_Ratio() throws Exception {
        preparePortrait();

        map.put("width", 300);
        map.put("height", 200);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(131, 200);
    }

    @Test
    public void testTransform_Portrait_to_Portrait_Ratio() throws Exception {
        preparePortrait();

        map.put("width", 200);
        map.put("height", 300);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(197, 300);
    }

    @Test
    public void testTransform_Square_to_Portrait_Ratio() throws Exception {
        prepareSquare();

        map.put("width", 200);
        map.put("height", 300);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(200, 200);
    }

    @Test
    public void testTransform_Square_to_Landscape_Ratio() throws Exception {
        prepareSquare();

        map.put("width", 300);
        map.put("height", 200);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(200, 200);
    }

    @Test
    public void testTransform_no_resize() throws Exception {
        prepareSquare();

        map.put("width", 800);
        map.put("height", 600);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(0)).resize(anyInt(), anyInt());
    }

    @Test
    public void testTransform_allow_resize() throws Exception {
        prepareSquare();

        map.put("width", 800);
        map.put("height", 600);
        map.put("upscale", true);

        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(600, 600);
    }

    private void prepareLandscape(){
        when(layer.getWidth()).thenReturn(640);
        when(layer.getHeight()).thenReturn(420);
    }

    private void preparePortrait(){
        when(layer.getWidth()).thenReturn(420);
        when(layer.getHeight()).thenReturn(640);
    }

    private void prepareSquare(){
        when(layer.getWidth()).thenReturn(420);
        when(layer.getHeight()).thenReturn(420);
    }
}
