package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Annotation to be used on either methods, fields to let Sling Models inject an inherited property from the
 * parent resource
 */
@Target({METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
@InjectAnnotation
@Source(ParentResourceValueMapValue.SOURCE)
public @interface ParentResourceValueMapValue {

    String SOURCE = "inherited-property";

    InjectionStrategy injectionStrategy();

    int maxLevel() default -1;
}
