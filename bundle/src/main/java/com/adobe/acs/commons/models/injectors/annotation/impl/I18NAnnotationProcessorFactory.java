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

import com.adobe.acs.commons.models.injectors.annotation.I18N;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * The annotation processor for the {@link I18N} annotation
 * <p>
 * Note: This can only be used together with Sling Models API bundle in version 1.2.0 (due to the dependency on InjectionStrategy)
 */
@Component(service = StaticInjectAnnotationProcessorFactory.class)
public class I18NAnnotationProcessorFactory implements StaticInjectAnnotationProcessorFactory {

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement annotatedElement) {
        return Optional.ofNullable(annotatedElement.getAnnotation(I18N.class))
                .map(ContextServiceAnnotationProcessor::new)
                .orElse(null);
    }

    private static class ContextServiceAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final I18N annotation;

        public ContextServiceAnnotationProcessor(I18N annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }
    }

}
