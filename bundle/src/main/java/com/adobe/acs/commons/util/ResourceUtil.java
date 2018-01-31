/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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
package com.adobe.acs.commons.util;

import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Utils for JCR resources.
 */
public abstract class ResourceUtil {
    /**
     * Get a single-value property from a resource.
     *
     * If the property does not exist, this function will return null instead
     * of throwing a RepositoryException.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Property getProperty(Resource resource, String namePattern) throws RepositoryException {
        try {
            return resource.adaptTo(Node.class).getProperty(namePattern);
        } catch (PathNotFoundException p) {
            return null;
        }
    }

    /**
     * Convenience method for getting a single-value boolean property from
     * a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value if present, else false.
     */
    public static boolean getPropertyBoolean(Resource resource, String namePattern) throws RepositoryException {
        Property prop = getProperty(resource, namePattern);
        return prop != null && prop.getBoolean();
    }

    /**
     * Convenience method for getting a single-value Date property from
     * a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Calendar getPropertyDate(Resource resource, String namePattern) throws RepositoryException {
        Property prop = getProperty(resource, namePattern);
        return prop != null ? prop.getDate() : null;
    }

    /**
     * Conventience method for getting a single-value Long property from a resource.
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Long getPropertyLong(Resource resource, String namePattern) throws RepositoryException {
        Property prop = getProperty(resource, namePattern);
        return prop != null ? prop.getLong() : null;
    }

    /**
     * Get a Resource from a path specified in a resource property.
     *
     * Returns null if the path cannot be resolved to a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Name of the property storing the resource path.
     * @return The referenced resource.
     */
    public static Resource getPropertyReference(Resource resource, String namePattern) throws RepositoryException {
        String referencePath = getPropertyString(resource, namePattern);
        if (StringUtils.isNotBlank(referencePath)) {
            return resource.getResourceResolver().getResource(referencePath);
        }
        return null;
    }

    /**
     * Convenience method for getting a single-value String property from a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static String getPropertyString(Resource resource, String namePattern) throws RepositoryException {
        Property prop = getProperty(resource, namePattern);
        return prop != null ? prop.getString() : null;
    }

    /**
     * Convenience method for getting a multi-value String property from a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property values.
     */
    public static List<String> getPropertyStrings(Resource resource, String namePattern) throws RepositoryException {
        List<String> props = new ArrayList<>();
        for (Value val : getPropertyValues(resource, namePattern)) {
            props.add(val.getString());
        }
        return props;
    }

    /**
     * Internal method to get all values for a multi-value property.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property values.
     */
    private static Value[] getPropertyValues(Resource resource, String namePattern) throws RepositoryException {
        Property prop = getProperty(resource, namePattern);
        if (prop != null) {
            if (prop.isMultiple()) {
                return prop.getValues();
            } else {
                return (new Value[]{prop.getValue()});
            }
        }
        return new Value[0];
    }
}
