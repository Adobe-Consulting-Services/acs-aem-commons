package com.adobe.acs.commons.jcrpersist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to provide a custom name to a property.
 * 
 * @author sangupta
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AEMProperty {

	/**
	 * @return the desired name of the field when it is serialized or
	 *         deserialized from JCR
	 * 
	 */
	String value();

}
