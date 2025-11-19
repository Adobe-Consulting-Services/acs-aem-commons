/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.models.injectors.annotation.impl;

import com.adobe.acs.commons.models.injectors.annotation.ChildResourceFromRequest;
import org.osgi.service.component.annotations.Component;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

import java.lang.reflect.AnnotatedElement;

/**
 * The annotation processor for the {@link ChildResourceFromRequest} annotation
 *
 * Note: This can only be used together with Sling Models API bundle in version 1.2.0 (due to the dependency on InjectionStrategy)
 */
@Component(service = StaticInjectAnnotationProcessorFactory.class)
public class ChildResourceFromRequestAnnotationProcessorFactory implements StaticInjectAnnotationProcessorFactory {

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(final AnnotatedElement element) {
        // check if the element has the expected annotation
        ChildResourceFromRequest annotation = element.getAnnotation(ChildResourceFromRequest.class);
        if (annotation != null) {
            return new ChildResourceFromRequestAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class ChildResourceFromRequestAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {
        private final ChildResourceFromRequest annotation;

        public ChildResourceFromRequestAnnotationProcessor(ChildResourceFromRequest annotation) {
            this.annotation = annotation;
        }

        public String getName() {
            return this.annotation.name().isEmpty() ? null : this.annotation.name();
        }

        public InjectionStrategy getInjectionStrategy() {
            return this.annotation.injectionStrategy();
        }
    }

}
