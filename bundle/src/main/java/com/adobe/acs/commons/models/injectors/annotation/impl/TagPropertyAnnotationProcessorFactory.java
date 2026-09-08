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
package com.adobe.acs.commons.models.injectors.annotation.impl;


import com.adobe.acs.commons.models.injectors.annotation.TagProperty;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.AbstractInjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotationProcessor2;
import org.apache.sling.models.spi.injectorspecific.StaticInjectAnnotationProcessorFactory;
import org.osgi.service.component.annotations.Component;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

import static org.apache.commons.lang3.StringUtils.isBlank;

@Component(service = StaticInjectAnnotationProcessorFactory.class)
public class  TagPropertyAnnotationProcessorFactory  implements StaticInjectAnnotationProcessorFactory {

    @Override
    public InjectAnnotationProcessor2 createAnnotationProcessor(AnnotatedElement element) {
        return Optional.ofNullable(element.getAnnotation(TagProperty.class)).map(TagPropertyAnnotationProcessorFactory.PagePropertyAnnotationProcessor::new).orElse(null);
    }

    private class PagePropertyAnnotationProcessor extends AbstractInjectAnnotationProcessor2 {
        private final TagProperty annotation;

        public PagePropertyAnnotationProcessor(TagProperty annotation) {
            this.annotation = annotation;
        }

        @Override
        public InjectionStrategy getInjectionStrategy() {
            return annotation.injectionStrategy();
        }

        @Override
        public String getName() {
            if (isBlank(annotation.value())) {
                return null;
            }
            return annotation.value();
        }

    }
}
