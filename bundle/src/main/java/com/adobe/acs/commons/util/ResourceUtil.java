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

import aQute.bnd.annotation.ProviderType;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.resource.Resource;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Utils for JCR resources.
 */
@ProviderType
public final class ResourceUtil {
    /**
     * Private ctor to hide the defualt public ctor.
     */
    private ResourceUtil() {
    }

    /**
     * Convenience method for getting a single-value boolean property from
     * a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value if present, else false.
     */
    public static boolean getPropertyBoolean(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, false);
    }

    /**
     * Convenience method for getting a single-value Calendar property from
     * a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Calendar getPropertyCalendar(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, Calendar.class);
    }

    /**
     * Convenience method for getting a single-value Date property from
     * a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Date getPropertyDate(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, Date.class);
    }

    /**
     * Conventience method for getting a single-value BigDecimal property from a resource.
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static BigDecimal getPropertyDecimal(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, BigDecimal.class);
    }

    /**
     * Conventience method for getting a single-value Double property from a resource.
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Double getPropertyDouble(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, Double.class);
    }

    /**
     * Conventience method for getting a single-value Long property from a resource.
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property value.
     */
    public static Long getPropertyLong(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, Long.class);
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
    public static Resource getPropertyReference(Resource resource, String namePattern) {
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
    public static String getPropertyString(Resource resource, String namePattern) {
        return resource.getValueMap().get(namePattern, String.class);
    }

    /**
     * Convenience method for getting a multi-value String property from a resource.
     *
     * @param resource The resource from which to get the property.
     * @param namePattern Property name.
     * @return Property values.
     */
    public static List<String> getPropertyStrings(Resource resource, String namePattern) {
        String[] vals = resource.getValueMap().get(namePattern, String[].class);
        return vals != null ? Arrays.asList(vals) : Collections.EMPTY_LIST;
    }
}
