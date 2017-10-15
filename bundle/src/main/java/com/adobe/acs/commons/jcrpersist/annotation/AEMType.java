package com.adobe.acs.commons.jcrpersist.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.TYPE, ElementType.METHOD })
public @interface AEMType {

	/**
	 * @return the desired name of the field when it is serialized or
	 *         deserialized from JCR
	 * 
	 */
	String primaryType();
	
	String childType();

}
