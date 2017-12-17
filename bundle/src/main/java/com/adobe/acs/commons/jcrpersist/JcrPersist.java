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

package com.adobe.acs.commons.jcrpersist;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.net.URI;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.jcrpersist.annotation.AEMProperty;
import com.adobe.acs.commons.jcrpersist.annotation.AEMPropertyExclude;
import com.adobe.acs.commons.jcrpersist.extension.ValueMapReader;
import com.adobe.acs.commons.jcrpersist.util.ReflectionUtils;
import com.day.cq.commons.jcr.JcrConstants;
import com.day.cq.wcm.api.Page;

/**
 * Main class that does the magic of reading/writing POJO directly
 * using the JCR without any intrusion. This also allows the system
 * to be more extensible.
 * 
 */
public class JcrPersist {
	
	/**
	 * My private logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JcrPersist.class);
	
	/**
	 * Classes that correspond to basic parameter types, which are handled directly in code
	 */
	static Set<Class<?>> BASIC_PARAMS = new HashSet<>();
	
	/**
	 * List of all registered custom value-map readers
	 */
	static Map<String, ValueMapReader> CUSTOM_VALUE_MAP_READERS = new HashMap<>();
	
	static Map<Class<?>, ResourceType> NODE_TYPE_FOR_CLASS = new HashMap<>();
	
	static final ResourceType NT_UNSTRUCTURED_RESOURCE_TYPE = new ResourceType(JcrConstants.NT_UNSTRUCTURED, null);
	
	/**
	 * Add default values
	 */
	static {
		// primitives
		BASIC_PARAMS.add(byte.class);
		BASIC_PARAMS.add(char.class);
		BASIC_PARAMS.add(short.class);
		BASIC_PARAMS.add(int.class);
		BASIC_PARAMS.add(long.class);
		BASIC_PARAMS.add(float.class);
		BASIC_PARAMS.add(double.class);
		BASIC_PARAMS.add(boolean.class);
		
		// primitive boxed
		BASIC_PARAMS.add(Byte.class);
		BASIC_PARAMS.add(Character.class);
		BASIC_PARAMS.add(Short.class);
		BASIC_PARAMS.add(Integer.class);
		BASIC_PARAMS.add(Long.class);
		BASIC_PARAMS.add(Float.class);
		BASIC_PARAMS.add(Double.class);
		BASIC_PARAMS.add(Boolean.class);
		
		// other basic types
		BASIC_PARAMS.add(String.class);
		BASIC_PARAMS.add(Calendar.class);
		BASIC_PARAMS.add(Date.class);
		BASIC_PARAMS.add(URI.class);
		BASIC_PARAMS.add(BigDecimal.class);
		
		// primitives array
		BASIC_PARAMS.add(byte[].class);
		BASIC_PARAMS.add(char[].class);
		BASIC_PARAMS.add(short[].class);
		BASIC_PARAMS.add(int[].class);
		BASIC_PARAMS.add(long[].class);
		BASIC_PARAMS.add(float[].class);
		BASIC_PARAMS.add(double[].class);
		BASIC_PARAMS.add(boolean[].class);
		
		// primitive boxed arrays
		BASIC_PARAMS.add(Byte[].class);
		BASIC_PARAMS.add(Character[].class);
		BASIC_PARAMS.add(Short[].class);
		BASIC_PARAMS.add(Integer[].class);
		BASIC_PARAMS.add(Long[].class);
		BASIC_PARAMS.add(Float[].class);
		BASIC_PARAMS.add(Double[].class);
		BASIC_PARAMS.add(Boolean[].class);
		
		// add custom readers
		CUSTOM_VALUE_MAP_READERS.put("cq:Page", new ValueMapReader() {
			
			@Override
			public ValueMap readValueMap(Resource resource) {
				Page page = resource.adaptTo(Page.class);
				return page.getProperties();
			}
			
		});
	}
	
	/**
	 * Register a custom value map reader that allows to read properties from a given
	 * node type for setting values in object attributes.
	 * 
	 * @param resourceType
	 * @param reader
	 * @return
	 */
	public static boolean addCustomValueMapReader(String resourceType, ValueMapReader reader) {
		if(CUSTOM_VALUE_MAP_READERS.containsKey(resourceType)) {
			return false;
		}
		
		CUSTOM_VALUE_MAP_READERS.put(resourceType, reader);
		return true;
	}
	
	// facade functions for reading/writing start here
	
	public static <T> T read(String nodePath, Class<T> classOfT, ResourceResolver resourceResolver) throws InstantiationException, IllegalAccessException {
		return JcrReader.read(nodePath, classOfT, resourceResolver);
	}
	
	public static <T> T read(Resource resource, Class<T> classOfT) throws InstantiationException, IllegalAccessException {
		return JcrReader.read(resource, classOfT);
	}
	
	public static void readToInstance(Resource resource, Object instance) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		JcrReader.readToInstance(resource, instance);
	}
	
	public static void persist(String nodePath, Object object, ResourceResolver resourceResolver) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
		JcrWriter.persist(nodePath, object, resourceResolver, true);
	}
	
	public static void persist(String nodePath, Object object, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
		JcrWriter.persist(nodePath, object, resourceResolver, deepPersist);
	}
	
	public static void persist(String nodePath, Object object, ResourceResolver resourceResolver, String[] propertiesToSave) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
		
	}
	
	public static boolean includeField(Class<?> clazz, String fieldName) {
		return false;
	}
	
	public static boolean excludeField(Class<?> clazz, String fieldName) {
		return false;
	}
	
	public static boolean setFieldName(Class<?> clazz, String fieldName, String propertyName) {
		return false;
	}
	
	public static boolean setType(Class<?> clazz, String primaryType) {
		return false;
	}
	
	public static boolean setImplicitCollection(Class<?> clazz, String fieldName) {
		return false;
	}
	
	public static boolean addPropertyConverter(Class<?> sourceClass, Class<?> destinationClass, Object converter) {
		return false;
	}
	
	public static boolean refresh(Object instance, ResourceResolver resourceResolver) {
		return false;
	}
	
	public static boolean save(Object instance, ResourceResolver resourceResolver) {
		return false;
	}
	
	// Utility function common to reading/writing start here

	/**
	 * Check if a given field is transient. A field is considered transient if
	 * and only if the field is marked with `transient` keyword and no
	 * annotation of type {@link AEMProperty} exists over the field; or if the
	 * field is marked with {@link AEMPropertyExclude} annotation.
	 * 
	 * @param field
	 *            the non-<code>null</code> field to check.
	 * 
	 * @return <code>true</code> if field is to be considered transient,
	 *         <code>false</code> otherwise
	 */
	static boolean isTransient(Field field) {
		boolean nativeTransient = ReflectionUtils.isTransient(field);
		
		if(nativeTransient) {
			// if property is covered using @AEMProperty annotation it shall not be excluded
			AEMProperty aemProperty = field.getAnnotation(AEMProperty.class);
			if(aemProperty == null) {
				return true;
			}
			
			return false;
		}
		
		// is the property annotated with @AEMExclude ?
		AEMPropertyExclude exclude = field.getAnnotation(AEMPropertyExclude.class);
		if(exclude != null) {
			return true;
		}
		
		return false;
	}

	/**
	 * Returns the name of the field to look for in JCR.
	 * 
	 * @param field
	 * @return
	 */
	static String getFieldName(Field field) {
		AEMProperty annotation = field.getAnnotation(AEMProperty.class);
		if(annotation == null) {
			return field.getName();
		}
		
		return annotation.value();
	}

	public static boolean isPrimitiveFieldType(Class<?> fieldType) {
		return JcrPersist.BASIC_PARAMS.contains(fieldType);
	}

	static class ResourceType {
		
		final String primaryType;
		
		final String childType;
		
		public ResourceType(String primaryType, String childType) {
			this.primaryType = primaryType;
			this.childType = childType;
		}
		
	}

}
