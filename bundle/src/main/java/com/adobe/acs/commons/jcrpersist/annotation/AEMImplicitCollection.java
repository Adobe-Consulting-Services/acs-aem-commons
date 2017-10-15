package com.adobe.acs.commons.jcrpersist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marker annotation to indicate that collection elements
 * are direct descendants of this node and do not have an extra
 * wrapper child node around.
 * 
 * @author sangupta
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD })
public @interface AEMImplicitCollection {

}
