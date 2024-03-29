/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.JsonValueMapValue;
import com.adobe.acs.commons.models.injectors.annotation.TagProperty;
import org.apache.sling.models.spi.DisposalCallbackRegistry;
import org.apache.sling.models.spi.Injector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.osgi.framework.Constants;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

@Component(
        service = {Injector.class},
        property = {
                Constants.SERVICE_RANKING + ":Integer=5520"
        }
)
public class TagPropertyInjector implements Injector {
    @NotNull
    @Override
    public String getName() {
        return TagProperty.SOURCE;
    }

    @Nullable
    @Override
    public Object getValue(Object adaptable, String name, Type declaredType, AnnotatedElement element, DisposalCallbackRegistry callbackRegistry) {

        if (element.isAnnotationPresent(TagProperty.class)) {

            // only inject directly
            TagProperty annotation = element.getAnnotation(TagProperty.class);
             return annotation.value();
        }

        return null;
    }
}
