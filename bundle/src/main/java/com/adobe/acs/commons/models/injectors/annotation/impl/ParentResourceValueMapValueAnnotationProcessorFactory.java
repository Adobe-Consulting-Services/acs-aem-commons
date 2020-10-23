/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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

import com.adobe.acs.commons.models.injectors.annotation.ParentResourceValueMapValue;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;

/**
 * The annotation processor for the {@link ParentResourceValueMapValue} annotation
 *
 * Note: This can only be used together with Sling Models API bundle in version 1.2.0 (due to the dependency on InjectionStrategy)
 */
@Component(service = StaticInjectAnnotationProcessorFactory.class)
public class ParentResourceValueMapValueAnnotationProcessorFactory implements StaticInjectAnnotationProcessorFactory {

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement annotatedElement) {
        ParentResourceValueMapValue annotation = annotatedElement.getAnnotation(ParentResourceValueMapValue.class);
        if (annotation != null) {
            return new ParentResourceValueMapValueAnnotationProcessor(annotation);
        }
        return null;
    }

    private static class ParentResourceValueMapValueAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {
        private final ParentResourceValueMapValue annotation;

        public ParentResourceValueMapValueAnnotationProcessor(final ParentResourceValueMapValue annotation) {
            this.annotation = annotation;
        }

        @Override
        public String getName() {
            return StringUtils.isNotBlank(annotation.name()) ? annotation.name() : super.getName();
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return this.annotation.injectionStrategy();
        }

        public Integer maxLevel() {
            return this.annotation.maxLevel();
        }
    }
}
