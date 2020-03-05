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

import com.adobe.acs.commons.images.transformers.impl.ResizeImageTransformerImpl;
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

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ResizeImageTransformerImplTest {
    ResizeImageTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new ResizeImageTransformerImpl();

        when(layer.getWidth()).thenReturn(1600);
        when(layer.getHeight()).thenReturn(900);
    }

    @After
    public void tearDown() throws Exception {
        map = null;
        reset(layer);
    }

    @Test
    public void testTransform() throws Exception {
        final int width = 100;
        final int height = 200;

        map.put("width", width);
        map.put("height", height);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(width, height);
        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_onlyWidth() throws Exception {
        final int width = 160;

        map.put("width", width);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(width, 90);
    }

    @Test
    public void testTransform_onlyHeight() throws Exception {
        final int height = 90;

        map.put("height", height);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(160, height);
    }

    @Test
    public void testTransform_invalidHeightAndWidth() throws Exception {
        final int width = -100;
        final int height = -200;

        map.put("width", width);
        map.put("height", height);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(1600, 900);
    }

    @Test
    public void testTransform_invalidWifth() throws Exception {
        final int width = -100;
        final int height = 90;

        map.put("width", width);
        map.put("height", height);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(160, height);
    }

    @Test
    public void testTransform_invalidHeight() throws Exception {
        final int width = 160;
        final int height = -100;

        map.put("width", width);
        map.put("height", height);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(width, 90);
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
}
