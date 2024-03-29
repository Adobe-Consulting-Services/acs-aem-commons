package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
@InjectAnnotation
@Source(ContentPolicyValue.SOURCE)
public @interface ContentPolicyValue {

    /**
     * Source value used for this annotation.
     * @see Source
     */
    String SOURCE = "design";

    String name() default "";

    /**
     * Whether to use the page policy (true) or the component policy (false).
     * @return
     */
    boolean usePagePolicy() default false;

    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

}
