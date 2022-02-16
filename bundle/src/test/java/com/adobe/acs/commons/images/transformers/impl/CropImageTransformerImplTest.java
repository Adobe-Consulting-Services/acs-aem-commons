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

import com.day.image.Layer;

import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CropImageTransformerImplTest {

    CropImageTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new CropImageTransformerImpl();

        when(layer.getWidth()).thenReturn(1600);
        when(layer.getHeight()).thenReturn(900);
    }

    @After
    public void tearDown() throws Exception {
        reset(layer);
        map = null;
    }

    @Test
    public void testTransform() throws Exception {
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 100, 200);

        map.put("bounds", "0,0,100,200");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_smartBoundsWidth() throws Exception {
        // 1600 x 900
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 1600, 400);

        map.put("bounds", "0,0,2000,500");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_smartBoundsHeight() throws Exception {
        // 1600 x 900
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 400, 900);

        map.put("bounds", "0,0,800,1800");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_smartBoundsBoth_width() throws Exception {
        // 1600 x 900
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 1600, 400);

        map.put("bounds", "0,0,20000,5000");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_smartBoundsBoth_height() throws Exception {
        // 1600 x 900
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 400, 900);

        map.put("bounds", "0,0,8000,18000");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_emptyParams() throws Exception {
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verifyNoInteractions(layer);
    }

    @Test
    public void testTransform_nullParams() throws Exception {
        transformer.transform(layer, null);

        verifyNoInteractions(layer);
    }

    @Test
    public void testTransform_percentage_full() throws Exception {
        Rectangle expected = new Rectangle();
        expected.setBounds(0, 0, 1600, 900);

        map.put("bounds", "0%,0%,100%,100%");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_percentage_part() throws Exception {
        Rectangle expected = new Rectangle();
        expected.setBounds(160, 180, 480, 360);

        map.put("bounds", "10%,20%,30%,40%");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_percentage_mixed1() throws Exception {
        Rectangle expected = new Rectangle();
        expected.setBounds(10, 180, 30, 360);

        map.put("bounds", "10,20%,30,40%");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

    @Test
    public void testTransform_percentage_mixed2() throws Exception {
        Rectangle expected = new Rectangle();
        expected.setBounds(160, 20, 480, 40);

        map.put("bounds", "10%,20,30%,40");
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).crop(expected);
    }

}
