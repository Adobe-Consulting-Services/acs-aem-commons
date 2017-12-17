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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Utility methods around object reflection.
 * 
 * Part of the code of this class has been borrowed from the open-source project
 * <code>jerry-core</code> from https://github.com/sangupta/jerry-core.
 *
 */
public abstract class ReflectionUtils {

    public static boolean isTransient(Field field) {
    	if(field == null) {
    		return false;
    	}

    	return Modifier.isTransient(field.getModifiers());
    }
    
    public static boolean bindValue(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return false;
    	}

    	if(instance == null) {
    		return false;
    	}

    	Class<?> type = field.getType();

		// set accessible so that we can work with private fields
		field.setAccessible(true);
		
		if(value != null && type.equals(value.getClass())) {
			field.set(instance, value);
			return true;
		}

		if(type.equals(URI.class)) {
			ReflectionBindUtils.bindURI(field, instance, value);
			return true;
		}
		
		if(type.equals(boolean.class)) {
			ReflectionBindUtils.bindBoolean(field, instance, value);
			return true;
		}
		if(type.equals(Boolean.class)) {
			ReflectionBindUtils.bindBooleanObject(field, instance, value);
			return true;
		}

		if(type.equals(byte.class)) {
			ReflectionBindUtils.bindByte(field, instance, value);
			return true;
		}
		if(type.equals(Byte.class)) {
			ReflectionBindUtils.bindByteObject(field, instance, value);
			return true;
		}

		if(type.equals(short.class)) {
			ReflectionBindUtils.bindShort(field, instance, value);
			return true;
		}
		if(type.equals(Short.class)) {
			ReflectionBindUtils.bindShortObject(field, instance, value);
			return true;
		}

		if(type.equals(char.class)) {
			ReflectionBindUtils.bindChar(field, instance, value);
			return true;
		}
		if(type.equals(Character.class)) {
			ReflectionBindUtils.bindCharacterObject(field, instance, value);
			return true;
		}

		if(type.equals(int.class)) {
			ReflectionBindUtils.bindInteger(field, instance, value);
			return true;
		}
		if(type.equals(Integer.class)) {
			ReflectionBindUtils.bindIntegerObject(field, instance, value);
			return true;
		}

		if(type.equals(long.class)) {
			ReflectionBindUtils.bindLong(field, instance, value);
			return true;
		}
		if(type.equals(Long.class)) {
			ReflectionBindUtils.bindLongObject(field, instance, value);
			return true;
		}

		if(type.equals(float.class)) {
			ReflectionBindUtils.bindFloat(field, instance, value);
			return true;
		}
		if(type.equals(Float.class)) {
			ReflectionBindUtils.bindFloatObject(field, instance, value);
			return true;
		}

		if(type.equals(double.class)) {
			ReflectionBindUtils.bindDouble(field, instance, value);
			return true;
		}
		if(type.equals(Double.class)) {
			ReflectionBindUtils.bindDoubleObject(field, instance, value);
			return true;
		}

		// check if this is an array and the value is of type String
		if(type.equals(byte[].class)) {
			ReflectionBindUtils.bindByteArray(field, instance, value);
			return true;
		}
		if(type.equals(char[].class)) {
			ReflectionBindUtils.bindCharArray(field, instance, value);
			return true;
		}
		if(type.equals(short[].class)) {
			ReflectionBindUtils.bindShortArray(field, instance, value);
			return true;
		}
		if(type.equals(int[].class)) {
			ReflectionBindUtils.bindIntArray(field, instance, value);
			return true;
		}
		if(type.equals(long[].class)) {
			ReflectionBindUtils.bindLongArray(field, instance, value);
			return true;
		}
		if(type.equals(float[].class)) {
			ReflectionBindUtils.bindFloatArray(field, instance, value);
			return true;
		}
		if(type.equals(double[].class)) {
			ReflectionBindUtils.bindDoubleArray(field, instance, value);
			return true;
		}
		if(type.equals(boolean[].class)) {
			ReflectionBindUtils.bindBooleanArray(field, instance, value);
			return true;
		}
		
		// we have not handled the value
		return false;
    }

	public static <T> T getFieldForName(Object instance, String name, Class<T> castTo) {
		if(instance == null) {
			return null;
		}
		
		if(AssertUtils.isEmpty(name)) {
			return null;
		}
		
		Class<?> clazz = instance.getClass();
		List<Field> fields = getAllFields(clazz);
		if(fields.isEmpty()) {
			return null;
		}
		
		for(Field field : fields) {
			if(field.getName().equals(name)) {
				try {
					field.setAccessible(true);
					Object object = field.get(instance);
					if(object == null) {
						return null;
					}
					
					return castTo.cast(object);
				} catch (IllegalArgumentException | IllegalAccessException e) {
					throw new RuntimeException(e);
				}
			}
		}
		
		return null;
	}

	/**
	 * Return all fields including all-private and all-inherited fields for the
	 * given class.
	 * 
	 * @param clazz
	 *            the class for which fields are needed
	 * 
	 * @return the {@link List} of {@link Field} objects in no certain order
	 * 
	 * @throws IllegalArgumentException if given class is <code>null</code>
	 */
	public static List<Field> getAllFields(Class<?> clazz) {
		if(clazz == null) {
			throw new IllegalArgumentException("Class to read fields from cannot be null");
		}
		
		// TODO: implement caching for faster retrievals
        List<Field> fields = new ArrayList<>();
        populateAllFields(clazz, fields);
        return fields;
    }
    
    public static void populateAllFields(Class<?> clazz, List<Field> fields) {
        if(clazz == null) {
            return;
        }
        
        Field[] array = clazz.getDeclaredFields();
        if(array != null && array.length > 0) {
            fields.addAll(Arrays.asList(array));
        }
        
        if(clazz.getSuperclass() == null) {
            return;
        }
        
        populateAllFields(clazz.getSuperclass(), fields);
    }
}
