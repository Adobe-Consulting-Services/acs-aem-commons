package com.adobe.acs.commons.models.injectors.annotation;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.models.annotations.Source;
import org.apache.sling.models.annotations.injectorspecific.InjectionStrategy;
import org.apache.sling.models.spi.injectorspecific.InjectAnnotation;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

@Target({METHOD, FIELD, PARAMETER})
@Retention(RUNTIME)
@InjectAnnotation
@Source(PageProperty.SOURCE)
public @interface PageProperty {

    /**
     * Source value used for this annotation.
     * @see Source
     */
    String SOURCE = "page-property";

    /**
     * Whether to use the current page (true) or the resource page (false).
     * @return
     */
    boolean useCurrentPage() default false;

    /**
     * Specifies the name of the value from the value map to take.
     * If empty, then the name is derived from the method or field.
     */
    String value() default StringUtils.EMPTY;

    /**
     * if set to REQUIRED injection is mandatory, if set to OPTIONAL injection is optional, in case of DEFAULT
     * the standard annotations ({@link org.apache.sling.models.annotations.Optional}, {@link org.apache.sling.models.annotations.Required}) are used.
     * If even those are not available the default injection strategy defined on the {@link org.apache.sling.models.annotations.Model} applies.
     * Default value = DEFAULT.
     */
    InjectionStrategy injectionStrategy() default InjectionStrategy.DEFAULT;

}
