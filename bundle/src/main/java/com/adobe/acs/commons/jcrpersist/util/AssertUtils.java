/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

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
