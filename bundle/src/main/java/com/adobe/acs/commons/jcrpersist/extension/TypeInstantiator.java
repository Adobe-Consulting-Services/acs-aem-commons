package com.adobe.acs.commons.jcrpersist.extension;

/**
 * Instantiate a new instance for a given type. This may be used
 * to extend functionality for classes that cannot be instantiated
 * using a default no-argument constructor.
 * 
 * @author sangupta
 *
 * @param <T>
 */
public interface TypeInstantiator<T> {
	
	public T getNewInstance();

}
