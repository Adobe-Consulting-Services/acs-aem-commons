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

import com.adobe.acs.commons.images.transformers.impl.RotateImageTransformerImpl;
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

@RunWith(MockitoJUnitRunner.class)
public class RotateImageTransformerImplTest {

    RotateImageTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new RotateImageTransformerImpl();
    }

    @After
    public void tearDown() throws Exception {
        map = null;
        reset(layer);
    }

    @Test
    public void testTransform() throws Exception {
        final int degrees = 50;

        map.put("degrees", degrees);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).rotate(degrees);
        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_mod() throws Exception {
        final int degrees = 5;

        map.put("degrees", 360 + degrees);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).rotate(degrees);
        verifyNoMoreInteractions(layer);
    }


    @Test
    public void testTransform_neg() throws Exception {
        final int degrees = -15;

        map.put("degrees", -360 + degrees);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).rotate(degrees);
        verifyNoMoreInteractions(layer);
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
