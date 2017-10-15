package com.adobe.acs.commons.jcrpersist.util;

/**
 * Simple assertion functions.
 * 
 * Part of the code of this class has been borrowed from the open-source project
 * <code>jerry-core</code> from https://github.com/sangupta/jerry-core.
 * 
 * @author sangupta
 *
 */
public class AssertUtils {

	public static boolean isEmpty(String str) {
		if(str == null || str.isEmpty()) {
			return true;
		}
		
		return false;
	}
	
	public static boolean isEmpty(Object[] array) {
		if(array == null || array.length == 0) {
			return true;
		}
		
		return false;
	}

	public static boolean isNotEmpty(String str) {
		return !isEmpty(str);
	}

}
