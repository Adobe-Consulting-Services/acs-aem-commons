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
package com.adobe.acs.commons.models.injectors.impl;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.impl.ModelAdapterFactory;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class MapBasedModelAdapterFactoryTest {

    @Test
    public void testRectangleInjection() {
        final SomeService someService = mock(SomeService.class);
        when(someService.sayHelloWorld()).thenReturn("hello world!");
        Map<String, Object> testValues = new HashMap<String, Object>() {{
            put("width", 6);
            put("height", 7);
            put("someService", someService);
        }};

        ModelAdapterFactory factory = new MapBasedModelAdapterFactory(testValues);

        Resource resource = mock(Resource.class);
        Rectangle rectangle = factory.getAdapter(resource, Rectangle.class);

        assertEquals(6, rectangle.getWidth());
        assertEquals(7, rectangle.getHeight());
        assertEquals(42, rectangle.getArea());
        assertEquals("hello world!", rectangle.greet());
    }

    // --- inner classes ---

    @Model(adaptables = Resource.class)
    public static class Rectangle {

        @Inject
        private int width;

        @Inject
        private int height;

        @Inject
        private SomeService someService;

        public int getWidth() {
            return width;
        }

        public int getHeight() {
            return height;
        }

        public int getArea() {
            return width * height;
        }

        public String greet() {
            return someService.sayHelloWorld();
        }
    }

    public interface SomeService {
        String sayHelloWorld();
    }
}
