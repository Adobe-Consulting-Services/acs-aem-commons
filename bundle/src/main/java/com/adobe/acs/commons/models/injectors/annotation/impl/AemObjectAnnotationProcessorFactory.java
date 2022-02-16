/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.models.injectors.annotation.impl;

import java.lang.reflect.AnnotatedElement;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;

import com.adobe.acs.commons.models.injectors.annotation.AemObject;

/**
 * The annotation processor for the {@link AemObject} annotation
 *
 * Note: This can only be used together with Sling Models API bundle in version 1.2.0 (due to the dependency on InjectionStrategy)
 */
@Component
@Service
public class AemObjectAnnotationProcessorFactory implements StaticInjectAnnotationProcessorFactory{

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(final AnnotatedElement element) {
        // check if the element has the expected annotation
        AemObject annotation = element.getAnnotation(AemObject.class);
        if (annotation != null) {
            return new AemObjectAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class AemObjectAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {

        private final AemObject annotation;

        public AemObjectAnnotationProcessor(final AemObject annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }
    }

}
