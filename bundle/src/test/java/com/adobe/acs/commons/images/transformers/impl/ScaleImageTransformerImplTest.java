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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ScaleImageTransformerImplTest {

    @Mock
    Layer layer;

    @Spy
    ResizeImageTransformerImpl resizeImageTransformer;

    @InjectMocks
    ScaleImageTransformerImpl transformer;


   ValueMap properties = null;

    @Before
    public void setUp() throws Exception {
        properties = new ValueMapDecorator(new HashMap<String, Object>());
        resizeImageTransformer = new ResizeImageTransformerImpl();
        transformer = new ScaleImageTransformerImpl();

        MockitoAnnotations.initMocks(this);

        when(layer.getWidth()).thenReturn(1600);
        when(layer.getHeight()).thenReturn(900);
    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        reset(layer);
    }

    @Test
    public void testTransform_withMalformedScale() throws Exception {
        properties.put("scale", "50%");

        transformer.transform(layer, properties);

        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_scaleAsDouble() throws Exception {
        properties.put("scale", ".50");

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(800, 450);
    }

    @Test
    public void testTransform_scaleAsDouble2() throws Exception {
        properties.put("scale", "1.50");

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(2400, 1350);
    }


    @Test
    public void testTransform_roundDefault() throws Exception {
        properties.put("scale", ".3333");

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(533, 300);
    }

    @Test
    public void testTransform_roundUp() throws Exception {
        properties.put("scale", ".3333");
        properties.put("round", "up");

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(534, 300);
    }

    @Test
    public void testTransform_roundDown() throws Exception {
        properties.put("scale", ".3333");
        properties.put("round", "down");

        transformer.transform(layer, properties);

        verify(layer, times(1)).resize(533, 299);
    }

    @Test
    public void testTransform_emptyParams() throws Exception {
        transformer.transform(layer, properties);

        verifyNoInteractions(layer);
    }

    @Test
    public void testTransform_nullParams() throws Exception {
        transformer.transform(layer, null);

        verifyNoInteractions(layer);
    }
}
