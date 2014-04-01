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

import org.apache.sling.models.impl.ModelAdapterFactory;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Simple ModelAdapterFactory which is backed by a java.util.Map
 *
 * The key is the name of the property (or the value of the @Named annotation),
 * value is the value to be injected.
 *
 * @see MapBasedModelAdapterFactoryTest
 */
public class MapBasedModelAdapterFactory extends ModelAdapterFactory {

    public MapBasedModelAdapterFactory(Map<String, Object> map) {
        super();

        org.osgi.service.component.ComponentContext componentCtx = mock(org.osgi.service.component.ComponentContext.class);
        BundleContext bundleContext = mock(BundleContext.class);
        when(componentCtx.getBundleContext()).thenReturn(bundleContext);

        activate(componentCtx);

        MapBasedInjector injector = new MapBasedInjector(map);
        super.bindInjector(injector, Collections.<String, Object> singletonMap(Constants.SERVICE_ID, 1L));
    }

    // --- inner classes ---

    private class MapBasedInjector implements Injector {

        private Map<String, Object> map;

        public MapBasedInjector(Map<String, Object> map) {
            this.map = map;
        }

        @Override
        public String getName() {
            return "map-based";
        }

        @Override
        public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {
            return map.get(name);
        }
    }

}
