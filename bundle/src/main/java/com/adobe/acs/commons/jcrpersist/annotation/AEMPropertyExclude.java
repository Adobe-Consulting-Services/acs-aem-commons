package com.adobe.acs.commons.jcrpersist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to signify that the attribute be excluded
 * from all serialization/deserialization workflows.
 * 
 * @author sangupta
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AEMPropertyExclude {

}
