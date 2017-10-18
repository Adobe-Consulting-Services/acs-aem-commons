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
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.jcrpersist.annotation.AEMImplicitCollection;
import com.adobe.acs.commons.jcrpersist.annotation.AEMPathProperty;
import com.adobe.acs.commons.jcrpersist.extension.ValueMapReader;
import com.adobe.acs.commons.jcrpersist.util.ReflectionUtils;
import com.adobe.acs.commons.jcrpersist.util.StringUtils;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * Code to read a given object from a JCR node.
 * 
 */
class JcrReader {
	
	/**
	 * My private logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JcrReader.class);

	/**
	 * Read from the given node path and populate a new instance of the given type
	 * using the provided resource resolver.
	 * 
	 * @param nodePath
	 *            the path of the node to read data from
	 * 
	 * @param classOfT
	 *            the type which needs to be instantiated for populating fields
	 * 
	 * @param resourceResolver
	 *            the {@link ResourceResolver} to use
	 * 
	 * @return a new {@link Object} of type represented by <code>classOfT</code>
	 * 
	 * @throws InstantiationException
	 *             if we cannot create a new instance
	 * 
	 * @throws IllegalAccessException
	 * 
	 * @throws {@link
	 *             IllegalArgumentException} if node path is <code>null</code> or
	 *             empty, of <code>classOfT</code> is <code>null</code>, or
	 *             <code>resourceResolver</code> is <code>null</code>
	 */
	static <T> T read(String nodePath, Class<T> classOfT, ResourceResolver resourceResolver) throws InstantiationException, IllegalAccessException {
		if(nodePath == null || nodePath.trim().isEmpty()) {
			throw new IllegalArgumentException("Node path cannot be null/empty");
		}
		
		if(classOfT == null) {
			throw new IllegalArgumentException("Object to save cannot be null");
		}
		
		if(resourceResolver == null) {
			throw new IllegalArgumentException("ResourceResolver cannot be null");
		}
		
		Resource resource = resourceResolver.getResource(nodePath);
		if(resource == null || ResourceUtil.isNonExistingResource(resource)) {
			return null;
		}
		
		return read(resource, classOfT);
	}
	
	/**
	 * Populate the properties of the given {@link Resource} to a new {@link Object}
	 * of given type.
	 * 
	 * @param resource
	 *            the {@link Resource} to read from
	 * 
	 * @param classOfT
	 *            the type to instantiate
	 * 
	 * @return a new instantiated type with properties from {@link Resource}
	 * 
	 * @throws InstantiationException
	 * 
	 * @throws IllegalAccessException
	 */
	static <T> T read(Resource resource, Class<T> classOfT) throws InstantiationException, IllegalAccessException {
		if(classOfT == null) {
			throw new IllegalArgumentException("Object type to bind resource to cannot be null");
		}
		
		if(resource == null) {
			return null;
		}
		
		// create a new instance
		T instance = instantiate(classOfT);
		
		// read values from resource
		readToInstance(resource, instance);
		
		// return
		return instance;
	}
	
	/**
	 * Populate the given object instance properties by reading the given resource.
	 * The existing properties are not cleaned before population starts. If the
	 * resource is <code>null</code> the values remain unmodified on the instance.
	 * 
	 * @param resource
	 *            the {@link Resource} to be read.
	 * 
	 * @param instance
	 *            the object instance whose properties are to be populated
	 * 
	 * @throws IllegalArgumentException
	 *             if instance is <code>null</code>
	 * 
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	static void readToInstance(Resource resource, Object instance) throws IllegalArgumentException, IllegalAccessException, InstantiationException {
		if(instance == null) {
			throw new IllegalArgumentException("Object to be populated cannot be null");
		}
		
		if(resource == null) {
			return;
		}
		
		// get type of instance we need to create
		final Class<?> classOfT = instance.getClass();
		
		// find all fields
		List<Field> fields = ReflectionUtils.getAllFields(classOfT);
		if(fields == null || fields.isEmpty()) {
			return;
		}
		
		// read values
		ValueMap values;

		// get the custom value-map reader
		String resourceType = resource.getResourceType();
		ValueMapReader customReader = JcrPersist.CUSTOM_VALUE_MAP_READERS.get(resourceType);
		if(customReader != null) {
			values = customReader.readValueMap(resource);
		} else {
			values = resource.getValueMap();
		}
		
		// start reading each field
		for(final Field field : fields) {
			if(JcrPersist.isTransient(field)) {
				// skip transient fields
				continue;
			}
			
			// find the type of field
			final Class<?> fieldType = field.getType();
			final String fieldName = JcrPersist.getFieldName(field);
			
			// set accessible
			field.setAccessible(true);
			
			// handle AEMPathProperty annotation
			AEMPathProperty pathProperty = field.getAnnotation(AEMPathProperty.class);
			if(pathProperty != null) {
				field.set(instance, resource.getPath());
				continue;
			}

			// handle the value as primitive first
			if(JcrPersist.isPrimitiveFieldType(fieldType)) {
				boolean handled = populateField(field, instance, values);
				if(!handled) {
					LOGGER.warn("Unable to bind value for primitive attribute: {}", field.getName());
				}
				
				// let's move back
				continue;
			}
			
			// the value could not be handled - may its 
			final boolean isCollection = Collection.class.isAssignableFrom(fieldType);

			// check if this is a collection of child objects - or a single object
			if(isCollection) {
				Type type = field.getGenericType();
				if(!(type instanceof ParameterizedType)) {
					// TODO: don't know how to create a list of non-parameterized type
					continue;
				}
				
				// find collection type
				ParameterizedType pt = (ParameterizedType) type;
				Class<?> collectionType = (Class<?>) pt.getActualTypeArguments()[0];
				
				// check if this is an implicit collection or an explicit collection
				Resource subResource;

				AEMImplicitCollection implicitCollection = field.getAnnotation(AEMImplicitCollection.class);
				if(implicitCollection == null) {
					// check if we have a property node with the same name
					subResource = getChildResource(resource, fieldName);
					if(subResource == null || ResourceUtil.isNonExistingResource(subResource)) {
						field.set(instance, null);
						continue;
					}
				} else {
					// we are an implicit collection
					// read direct descendants
					subResource = resource;
				}
				
				// check if there are children available for the subResource
				Iterator<Resource> iterator = subResource.listChildren();
				if(iterator == null) {
					field.set(instance, getCollectionOfType(fieldType));
					continue;
				}
				
				Collection<Object> collection = getCollectionOfType(fieldType);
				while(iterator.hasNext()) {
					Resource child = iterator.next();
					
					LOGGER.debug("Child name: {}", child.getName());
					if(child.getPath().endsWith("/jcr:content")) {
						// skip the jcr:content node
						continue;
					}
					
					Object childObject = read(child, collectionType);
					if(childObject != null) {
						collection.add(childObject);
					}
				}
				
				field.set(instance, collection);
				continue;
			}

			// this is a composed object directly as a child node
			Resource subResource = getChildResource(resource, fieldName);
			if(subResource == null || ResourceUtil.isNonExistingResource(subResource)) {
				field.set(instance, null);
				continue;
			}
			
			Object subInstance = read(subResource, fieldType);
			field.set(instance, subInstance);
		}
	}
	
	/**
	 * Return the child resource with the given child name from the given
	 * {@link Resource}
	 * 
	 * @param resource
	 *            the {@link Resource} to read child from
	 * 
	 * @param childName
	 *            the name of the child
	 * 
	 * @return the child {@link Resource} instance if available, or
	 *         <code>null</code>
	 */
	private static Resource getChildResource(Resource resource, String childName) {
		Resource subResource = resource.getChild(childName);
		if(subResource != null && !ResourceUtil.isNonExistingResource(subResource)) {
			return subResource;
		}
		
		subResource = resource.getChild(JcrConstants.JCR_CONTENT);
		if(subResource == null || ResourceUtil.isNonExistingResource(subResource)) {
			return null;
		}
		
		return subResource.getChild(childName);
	}

	/**
	 * Return a new collection of the provided type where the type is either an
	 * abstract class or an interface. For example, we instantiate an
	 * {@link ArrayList} for a {@link List}, and a {@link HashSet} for a {@link Set}
	 * implementation.
	 * 
	 * @param type
	 *            the type to instantiate
	 * 
	 * @return <code>null</code> if the provided type is <code>null</code> or
	 *         un-handled, else the instantiated type is returned
	 */
	private static Collection<Object> getCollectionOfType(Class<?> type) {
		if(type == null) {
			return null;
		}
		
		if(type.equals(List.class)) {
			return new ArrayList<>();
		}
		
		if(type.equals(Set.class)) {
			return new HashSet<>();
		}
		
		return null;
	}
	
	/**
	 * Populate the provided field in the given instance
	 * @param field
	 * @param instance
	 * @param values
	 * @return
	 * @throws IllegalArgumentException
	 * @throws IllegalAccessException
	 */
	private static <T> boolean populateField(Field field, T instance, ValueMap values) throws IllegalArgumentException, IllegalAccessException {
		String name = JcrPersist.getFieldName(field);
		Object value = values.get(name);
		
		if(value == null) {
			return ReflectionUtils.bindValue(field, instance, null);
		}
		
		Class<?> fieldType = field.getType();

		if(value instanceof Calendar) {
			Calendar calendar = (Calendar) value;

			// check field type
			if(fieldType.equals(Date.class)) {
				value = new Date(calendar.getTimeInMillis());
			}
			
			if(fieldType.equals(long.class)) {
				value = calendar.getTimeInMillis();
			}
			
			if(fieldType.equals(String.class)) {
				value = new Date(calendar.getTimeInMillis()).toString();
			}
		}
		
		if(value instanceof String) {
			String str = (String) value;
			
			if(fieldType.equals(Date.class)) {
				value = new Date(StringUtils.getLongValue(str, 0l));
			}
			
			if(fieldType.equals(Calendar.class)) {
				Calendar calendar = Calendar.getInstance();
				calendar.setTimeInMillis(StringUtils.getLongValue(str, 0l));
				value = calendar;
			}
		}
		
		return ReflectionUtils.bindValue(field, instance, value);
	}
	
	/**
	 * Create a new instance for the given class. This method is used to make sure
	 * that we can use custom extension points for using factories tomorrow.
	 * 
	 * @param classOfT
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 */
	private static <T> T instantiate(Class<T> classOfT) throws InstantiationException, IllegalAccessException {
		// TODO: allow extension point for using custom factories and argument-constructors
		return classOfT.newInstance();
	}

}
