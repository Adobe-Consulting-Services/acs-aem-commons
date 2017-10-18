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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple functions to work around {@link String} objects.
 * 
 * Part of the code of this class has been borrowed from the open-source project
 * <code>jerry-core</code> from https://github.com/sangupta/jerry-core.
 * 
 */
public class StringUtils {

	private static final Logger LOGGER = LoggerFactory.getLogger(StringUtils.class);
	
	public static final String COMMA_SEPARATOR_CHAR = ",";

	public static boolean getBoolean(String boolString) {
	    return getBoolean(boolString, false);
 	}
	
	public static boolean getBoolean(String boolString, boolean defaultValue) {
		if (AssertUtils.isNotEmpty(boolString)) {
			boolString = boolString.toLowerCase();
			if ("yes".equals(boolString) || "true".equals(boolString) || "on".equals(boolString)) {
				return true;
			}

			if ("no".equals(boolString) || "false".equals(boolString) || "off".equals(boolString)) {
				return false;
			}
		}

		return defaultValue;
	}

	public static byte getByteValue(String string, byte defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Byte.parseByte(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting byte from string: " + string, e);
		}

		return defaultValue;
	}

	public static char getCharValue(String string, char defaultValue) {
		if (AssertUtils.isNotEmpty(string)) {
			return string.charAt(0);
		}

		return defaultValue;
	}

	public static short getShortValue(String string, short defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Short.parseShort(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting short from string: " + string, e);
		}

		return defaultValue;
	}

	public static int getIntValue(String string, int defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Integer.parseInt(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting integer from string: " + string, e);
		}

		return defaultValue;
	}

	public static long getLongValue(String string, long defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Long.parseLong(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting long from string: " + string, e);
		}

		return defaultValue;
	}

	public static float getFloatValue(String string, float defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Float.parseFloat(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting long from string: " + string, e);
		}

		return defaultValue;
	}

	public static double getDoubleValue(String string, double defaultValue) {
		try {
			if (AssertUtils.isNotEmpty(string)) {
				return Double.parseDouble(string);
			}
		} catch (NumberFormatException e) {
			LOGGER.debug("error getting long from string: " + string, e);
		}

		return defaultValue;
	}

	public static byte[] deStringifyByteArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		byte[] array = new byte[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Byte.parseByte(tokens[index]);
		}

		return array;
	}

	public static char[] deStringifyCharArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		char[] array = new char[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = tokens[index].charAt(0);
		}

		return array;
	}

	public static short[] deStringifyShortArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		short[] array = new short[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Short.valueOf(tokens[index]);
		}

		return array;
	}

	public static boolean[] deStringifyBooleanArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		boolean[] array = new boolean[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = getBoolean(tokens[index]);
		}

		return array;
	}

	public static int[] deStringifyIntArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		int[] array = new int[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Integer.parseInt(tokens[index]);
		}

		return array;
	}

	public static long[] deStringifyLongArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		long[] array = new long[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Long.valueOf(tokens[index]);
		}

		return array;
	}

	public static float[] deStringifyFloatArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		float[] array = new float[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Float.valueOf(tokens[index]);
		}

		return array;
	}

	public static double[] deStringifyDoubleArray(String value) {
		if (AssertUtils.isEmpty(value)) {
			return null;
		}

		String[] tokens = value.split(COMMA_SEPARATOR_CHAR);
		double[] array = new double[tokens.length];
		for (int index = 0; index < tokens.length; index++) {
			array[index] = Double.valueOf(tokens[index]);
		}

		return array;
	}
}
