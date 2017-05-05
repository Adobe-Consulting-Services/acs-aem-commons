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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class QualityImageTransformerImplTest {

    QualityImageTransformerImpl transformer;

    @Mock
    Layer layer;

    Map<String, Object> map = null;

    @Before
    public void setUp() throws Exception {
        map = new HashMap<String, Object>();
        transformer = new QualityImageTransformerImpl();
    }

    @Test
    public void testTransform() throws Exception {
        when(layer.getMimeType()).thenReturn("image/png");

        map.put("quality", 50);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).write(eq("image/png"), eq(0.5d), any(OutputStream.class));
    }

    @Test
    public void testTransform_Min() throws Exception {
        when(layer.getMimeType()).thenReturn("image/png");

        map.put("quality", 0);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).write(eq("image/png"), eq(0d), any(OutputStream.class));
    }

    @Test
    public void testTransform_UnderMin() throws Exception {
        when(layer.getMimeType()).thenReturn("image/png");

        map.put("quality", -20);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).write(eq("image/png"), eq(0d), any(OutputStream.class));
    }

    @Test
    public void testTransform_Max() throws Exception {
        when(layer.getMimeType()).thenReturn("image/png");

        map.put("quality", 100);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_OverMax() throws Exception {
        when(layer.getMimeType()).thenReturn("image/png");

        map.put("quality", 101);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_GIF() throws Exception {
        when(layer.getMimeType()).thenReturn("image/gif");

        map.put("quality", 50);
        ValueMap properties = new ValueMapDecorator(map);

        transformer.transform(layer, properties);

        verify(layer, times(1)).write(eq("image/gif"), eq(.5 * 255d), any(OutputStream.class));
    }
}
