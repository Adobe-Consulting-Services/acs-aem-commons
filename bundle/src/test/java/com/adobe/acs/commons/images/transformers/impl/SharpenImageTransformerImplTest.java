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

import java.util.HashMap;

import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@RunWith(MockitoJUnitRunner.class)
public class SharpenImageTransformerImplTest {

    SharpenImageTransformerImpl transformer;

    @Mock
    Layer layer;

    ValueMap properties = null;

    @Before
    public void setUp() throws Exception {
        properties = new ValueMapDecorator(new HashMap<String, Object>());
        transformer = new SharpenImageTransformerImpl();
    }

    @After
    public void tearDown() throws Exception {
        properties = null;
        reset(layer);
    }

    @Test
    public void testTransform() throws Exception {
        properties.put("op_usm", "2.0,1.0");

        transformer.transform(layer, properties);

        verify(layer, times(1)).sharpen(2.0f,1.0f);
        verifyNoMoreInteractions(layer);
    }

    @Test
    public void testTransform_withFourValues() throws Exception {
        properties.put("op_usm", "2.0,1.0,3,5");

        transformer.transform(layer, properties);

        verify(layer, times(0)).sharpen(2.0f,1.0f);
        verifyNoMoreInteractions(layer);
    }
}
