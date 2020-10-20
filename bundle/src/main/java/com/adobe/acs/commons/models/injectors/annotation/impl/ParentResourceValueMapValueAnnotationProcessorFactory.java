package com.adobe.acs.commons.models.injectors.annotation.impl;

import com.adobe.acs.commons.models.injectors.annotation.ParentResourceValueMapValue;
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
        public InjectionStrategy getInjectionStrategy() {
            return this.annotation.injectionStrategy();
        }

        public Integer maxLevel() {
            return this.annotation.maxLevel();
        }
    }
}
