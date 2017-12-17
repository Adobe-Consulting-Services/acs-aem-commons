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
import java.net.URI;
import java.net.URISyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility methods around object reflection.
 * 
 * Part of the code of this class has been borrowed from the open-source project
 * <code>jerry-core</code> from https://github.com/sangupta/jerry-core.
 *
 */
public abstract class ReflectionBindUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(ReflectionBindUtils.class);

    public static void bindURI(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}
    	
    	if(value instanceof URI) {
    		field.set(instance, (URI) value);
    		return;
    	}
    	
    	if(value instanceof String) {
    		try {
				URI uri = new URI((String) value);
				field.set(instance, uri);
			} catch (URISyntaxException e) {
				LOGGER.debug("Unable to convert value to URI: {}", value);
			}
    		
    		return;
    	}
    }

    public static void bindBoolean(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Boolean) {
			field.setBoolean(instance, (Boolean) value);
		} else {
			field.setBoolean(instance, StringUtils.getBoolean(value.toString(), field.getBoolean(instance)));
		}
    }
    
    public static void bindBooleanObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Boolean) {
			field.set(instance, (Boolean) value);
		} else {
			Boolean boolValue = Boolean.parseBoolean(value.toString());
			field.set(instance, boolValue);
		}
    }

    public static void bindByte(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Byte) {
			field.setByte(instance, (Byte) value);
		} else {
			field.setByte(instance, StringUtils.getByteValue(value.toString(), field.getByte(instance)));
		}
    }
    
    public static void bindByteObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Byte) {
			field.set(instance, (Byte) value);
		} else {
			Byte byteValue = Byte.parseByte(value.toString());
			field.set(instance, byteValue);
		}
    }

    public static void bindShort(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Short) {
			field.setShort(instance, (Short) value);
		} else {
			field.setShort(instance, StringUtils.getShortValue(value.toString(), field.getShort(instance)));
		}
    }
    
    public static void bindShortObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Short) {
			field.set(instance, (Short) value);
		} else {
			Short shortValue = Short.parseShort(value.toString());
			field.set(instance, shortValue);
		}
    }

    public static void bindInteger(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		return;
    	}
    	
    	if(value instanceof Integer) {
			field.setInt(instance, (Integer) value);
		} else {
			field.setInt(instance, StringUtils.getIntValue(value.toString(), field.getInt(instance)));
		}
    }
    
    public static void bindIntegerObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Integer) {
			field.set(instance, (Integer) value);
		} else {
			Integer intValue = Integer.parseInt(value.toString());
			field.set(instance, intValue);
		}
    }

    public static void bindLong(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Long) {
			field.setLong(instance, (Long) value);
		} else {
			field.setLong(instance, StringUtils.getLongValue(value.toString(), field.getLong(instance)));
		}
    }
    
    public static void bindLongObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Long) {
			field.set(instance, (Long) value);
		} else {
			Long longValue = Long.parseLong(value.toString());
			field.set(instance, longValue);
		}
    }

    public static void bindChar(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Character) {
			field.setChar(instance, (Character) value);
		} else {
			field.setChar(instance, StringUtils.getCharValue(value.toString(), field.getChar(instance)));
		}
    }
    
    public static void bindCharacterObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Character) {
			field.set(instance, (Character) value);
		} else {
			Character charValue = value.toString().toCharArray()[0];
			field.set(instance, charValue);
		}
    }

    public static void bindFloat(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Float) {
			field.setFloat(instance, (Float) value);
		} else {
			field.setFloat(instance, StringUtils.getFloatValue(value.toString(), field.getFloat(instance)));
		}
    }
    
    public static void bindFloatObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Float) {
			field.set(instance, (Float) value);
		} else {
			Float floatValue = Float.parseFloat(value.toString());
			field.set(instance, floatValue);
		}
    }

    public static void bindDouble(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

    	if(value instanceof Double) {
			field.setDouble(instance, (Double) value);
		} else {
			field.setDouble(instance, StringUtils.getDoubleValue(value.toString(), field.getDouble(instance)));
		}
    }
    
    public static void bindDoubleObject(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}
    	
    	if(value == null) {
    		field.set(instance, null);
    		return;
    	}

    	if(value instanceof Double) {
			field.set(instance, (Double) value);
		} else {
			Double doubleValue = Double.parseDouble(value.toString());
			field.set(instance, doubleValue);
		}
    }

    public static void bindByteArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof byte[]) {
			field.set(instance, (byte[]) value);
		} else {
			if(value instanceof String) {
				byte[] array = StringUtils.deStringifyByteArray((String) value);
				field.set(instance, array);
			}
		}
	}

    public static void bindCharArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof char[]) {
			field.set(instance, (char[]) value);
		} else {
			if(value instanceof String) {
				char[] array = StringUtils.deStringifyCharArray((String) value);
				field.set(instance, array);
			}
		}
	}

    public static void bindShortArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof short[]) {
			field.set(instance, (short[]) value);
		} else {
			if(value instanceof String) {
				short[] array = StringUtils.deStringifyShortArray((String) value);
				field.set(instance, array);
			}
		}
	}

    public static void bindIntArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof int[]) {
			field.set(instance, (int[]) value);
		} else if(value instanceof Integer[]) {
			Integer[] array = (Integer[]) value;
			int[] newArray = new int[array.length];
			
			for(int index = 0; index < array.length; index++) {
				newArray[index] = array[index];
			}
			
			field.set(instance, newArray);
		} else if(value instanceof String) {
			int[] array = StringUtils.deStringifyIntArray((String) value);
			field.set(instance, array);
		}
	}

    public static void bindLongArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof long[]) {
			field.set(instance, (long[]) value);
		} else if(value instanceof Long[]) {
			Long[] array = (Long[]) value;
			long[] newArray = new long[array.length];
			
			for(int index = 0; index < array.length; index++) {
				newArray[index] = array[index];
			}
			
			field.set(instance, newArray);
		} else if(value instanceof String) {
			long[] array = StringUtils.deStringifyLongArray((String) value);
			field.set(instance, array);
		}
	}

	public static void bindFloatArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof float[]) {
			field.set(instance, (float[]) value);
		} else if(value instanceof Float[]) {
			Float[] array = (Float[]) value;
			float[] newArray = new float[array.length];
			
			for(int index = 0; index < array.length; index++) {
				newArray[index] = array[index];
			}
			
			field.set(instance, newArray);
		} else if(value instanceof String) {
			float[] array = StringUtils.deStringifyFloatArray((String) value);
			field.set(instance, array);
		}
	}

	public static void bindDoubleArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof double[]) {
			field.set(instance, (double[]) value);
		} else if(value instanceof Double[]) {
			Double[] array = (Double[]) value;
			double[] newArray = new double[array.length];
			
			for(int index = 0; index < array.length; index++) {
				newArray[index] = array[index];
			}
			
			field.set(instance, newArray);
		} else if(value instanceof String) {
			double[] array = StringUtils.deStringifyDoubleArray((String) value);
			field.set(instance, array);
		}
	}

	public static void bindBooleanArray(Field field, Object instance, Object value) throws IllegalArgumentException, IllegalAccessException {
    	if(field == null) {
    		return;
    	}

    	if(instance == null) {
    		return;
    	}

		if(value instanceof boolean[]) {
			field.set(instance, (boolean[]) value);
		} else if(value instanceof Boolean[]) {
			Boolean[] array = (Boolean[]) value;
			boolean[] newArray = new boolean[array.length];
			
			for(int index = 0; index < array.length; index++) {
				newArray[index] = array[index];
			}
			
			field.set(instance, newArray);
		} else if(value instanceof String) {
			boolean[] array = StringUtils.deStringifyBooleanArray((String) value);
			field.set(instance, array);
		}
	}

}
