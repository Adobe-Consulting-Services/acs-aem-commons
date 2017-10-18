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
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.jcr.RepositoryException;

import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.PersistenceException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.jcrpersist.JcrPersist.ResourceType;
import com.adobe.acs.commons.jcrpersist.annotation.AEMImplicitCollection;
import com.adobe.acs.commons.jcrpersist.annotation.AEMPathProperty;
import com.adobe.acs.commons.jcrpersist.annotation.AEMType;
import com.adobe.acs.commons.jcrpersist.util.AssertUtils;
import com.adobe.acs.commons.jcrpersist.util.ReflectionUtils;
import com.day.cq.commons.jcr.JcrConstants;

/**
 * Code to persist a given object instance to a JCR node.
 * 
 */
class JcrWriter {

	/**
	 * My private logger
	 */
	private static final Logger LOGGER = LoggerFactory.getLogger(JcrWriter.class);
	
	static final Map<String, Object> RESOURCE_TYPE_NT_UNSTRUCTURED = new HashMap<>();
	
	static {
		RESOURCE_TYPE_NT_UNSTRUCTURED.put(JcrConstants.JCR_PRIMARYTYPE, JcrConstants.NT_UNSTRUCTURED);
	}
	
	static void persist(final String nodePath, final Object instance, ResourceResolver resourceResolver, boolean deepPersist) throws RepositoryException, PersistenceException, IllegalArgumentException, IllegalAccessException {
		if(nodePath == null || nodePath.trim().isEmpty()) {
			throw new IllegalArgumentException("Node path cannot be null/empty");
		}
		
		if(instance == null) {
			throw new IllegalArgumentException("Object to save cannot be null");
		}
		
		if(resourceResolver == null) {
			throw new IllegalArgumentException("ResourceResolver cannot be null");
		}
		
		// get the resource
		Resource resource = resourceResolver.getResource(nodePath);
		
		final boolean exists = (resource != null) && !ResourceUtil.isNonExistingResource(resource);
		
		final ResourceType resourceType = getResourceType(instance.getClass());
		
		// let's create the resource first
		LOGGER.debug("Creating node at: {} of type: {}", nodePath, resourceType.primaryType);
		resource = ResourceUtil.getOrCreateResource(resourceResolver, nodePath, asMap(resourceType.primaryType), JcrConstants.NT_UNSTRUCTURED, true);
		
		if(AssertUtils.isNotEmpty(resourceType.childType)) {
			LOGGER.debug("Needs a child node, creating node at: {} of type: {}", nodePath, resourceType.childType);
			resource = ResourceUtil.getOrCreateResource(resourceResolver, nodePath + "/" + JcrConstants.JCR_CONTENT, resourceType.childType, JcrConstants.NT_UNSTRUCTURED, true);
		}
		
		// read the existing resource map
		ModifiableValueMap values = resource.adaptTo(ModifiableValueMap.class);
		
		// find properties to be saved
		List<Field> fields = ReflectionUtils.getAllFields(instance.getClass());
		if(fields == null || fields.isEmpty()) {
			// TODO: remove all properties
		} else {
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

				// handle the value as primitive first
				if(JcrPersist.isPrimitiveFieldType(fieldType)) {
					Object value = field.get(instance);
					
					if(value == null) {
						// remove the attribute that is null
						values.remove(fieldName);
						continue;
					}
					
					values.put(fieldName, value);
					continue;
				}
				
				// do not persist compound objects if they are not needed
				if(!deepPersist) {
					continue;
				}
				
				// the value could not be handled - may its 
				final boolean isCollection = Collection.class.isAssignableFrom(fieldType);
				if(isCollection) {
					String collectionRoot = nodePath;
					
					// check if this is an implicit collection
					AEMImplicitCollection implicitCollection = field.getAnnotation(AEMImplicitCollection.class);
					if(implicitCollection == null) {
						// create a child collection of required type
						// and persist all instances
						
						// create the first child node first
						persist(nodePath + "/" + fieldName, new Object(), resourceResolver, false);
						
						// update collection root
						collectionRoot = nodePath + "/" + fieldName;
					}
					
					// now for each child in the collection - create a new node
					Collection<Object> collection = (Collection<Object>) field.get(instance);
					if(collection == null) {
						// TODO: remove the object if it exists on this resource
						continue;
					}
					
					Field pathField = null;
					for(Object childObject : collection) {
						if(pathField == null) {
							pathField = findAEMPathField(childObject.getClass());
						}
						
						String childName = null;
						if(pathField != null) {
							pathField.setAccessible(true);
							childName = extractNodeNameFromPath(pathField, childObject);
						}
						
						if(childName == null) {
							childName = UUID.randomUUID().toString();
						}
						
						persist(collectionRoot + "/" + childName, childObject, resourceResolver, true);
					}
				} else {
					// this is a single compound object
					// create a child node and persist all its values
					Object value = field.get(instance);
					if(value == null) {
						continue;
					}
					
					persist(nodePath + "/" + fieldName, value, resourceResolver, true);
				}
			}
		}
		
		// save back
		resourceResolver.commit();
	}

	private static String extractNodeNameFromPath(Field pathField, Object childObject) throws IllegalArgumentException, IllegalAccessException {
		Object pathObject = pathField.get(childObject);
		if(pathObject == null) {
			return null;
		}
		
		String pathValue = pathObject.toString();
		if(AssertUtils.isEmpty(pathValue)) {
			return null;
		}
		
		int lastIndex = pathValue.lastIndexOf('/');
		if(lastIndex >= 0) {
			pathValue = pathValue.substring(lastIndex + 1);
		}

		return pathValue;
	}

	private static Field findAEMPathField(Class<? extends Object> clazz) {
		// TODO: implement caching
		List<Field> fields = ReflectionUtils.getAllFields(clazz);
		if(fields == null || fields.isEmpty()) {
			LOGGER.warn("Object of type {} does NOT contain a AEMPathProperty attribute - multiple instances may conflict", clazz);
			return null;
		}
		
		for(Field field : fields) {
			if(field.isAnnotationPresent(AEMPathProperty.class)) {
				return field;
			}
		}
		
		LOGGER.warn("Object of type {} does NOT contain a AEMPathProperty attribute - multiple instances may conflict", clazz);
		return null;
	}

	private static ResourceType getResourceType(Class<?> clazz) {
		if(clazz == null) {
			return JcrPersist.NT_UNSTRUCTURED_RESOURCE_TYPE;
		}
		
		ResourceType type = JcrPersist.NODE_TYPE_FOR_CLASS.get(clazz);
		if(type != null) {
			return type;
		}
		
		AEMType aemType = clazz.getAnnotation(AEMType.class);
		if(aemType != null) {
			return new ResourceType(aemType.primaryType(), aemType.childType());
		}
		
		return JcrPersist.NT_UNSTRUCTURED_RESOURCE_TYPE;
	}

	private static Map<String, Object> asMap(String value) {
		Map<String, Object> map = new HashMap<>();
		map.put(JcrConstants.JCR_PRIMARYTYPE, value);
		
		return map;
	}

}
