/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import aQute.bnd.annotation.ProviderType;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@ProviderType
public final class TextUtil {
    
    private static final Pattern RICH_TEXT_PATTERN = Pattern.compile("<[^>]+>");

    private TextUtil() {
    }

    /**
     * Returns first non-null value from the parameter list
     * <p>
     * Ex. TextUtil.getFirstNonNull(x.getLastModifiedDate(),
     * x.getCreatedDate())
     * <p>
     * If getLastModifiedDate() returns null, and getCreatedDate() returns not-null,
     * the value for getCreatedDate() is returned.
     *
     * @param <T>
     * @param values
     * @return
     */
    public static <T> T getFirstNonNull(T... values) {
        if (values == null || values.length < 1) {
            return null;
        }
        List<T> list = Arrays.asList(values);

        for (T item : list) {
            if (item != null) {
                return item;
            }
        }

        return null;
    }


    /**
     * Returns the first non-null and non-empty String from the parameter list of strings.
     * <p>
     * Ex. TextUtil.getFirstNonEmpty(x.getPageTitle(),
     * x.getNavigationTitle(),
     * x.getTitle(),
     * x.getName())
     *
     * @param values
     * @return
     */
    public static String getFirstNonEmpty(String... values) {
        if (values == null || values.length < 1) {
            return null;
        }
        List<String> list = Arrays.asList(values);

        for (String item : list) {
            if (StringUtils.isNotBlank(item)) {
                return item;
            }
        }

        return null;
    }

    /**
     * Returns first non-null value from the resource property.
     *
     * @param <T>
     * @param resource
     * @param klass
     * @param keys
     * @return
     */
    public static <T> T getFirstProperty(Resource resource, Class<T> klass, String... keys) {
        return getFirstProperty(resource.adaptTo(ValueMap.class), klass, keys);
    }

    /**
     * Returns first non-null value from the resource property value map.
     * <p>
     * Ex. TextUtil.getFirstProperty(Date.class,
     * "jcr:lastModified",
     * "jcr:created")
     * <p>
     * If getLastModifiedDate() returns null, and getCreatedDate() returns not-null,
     * the value for getCreatedDate() is returned.
     *
     * @param <T>
     * @param valueMap of resource properties
     * @param klass    data type to return
     * @param keys     list of property names to evaluate
     * @return
     */
    public static <T> T getFirstProperty(ValueMap valueMap, Class<T> klass, String... keys) {
        if (valueMap == null || keys == null || keys.length < 1) {
            return null;
        }

        List<String> keyList = Arrays.asList(keys);

        for (String key : keyList) {
            if (valueMap.containsKey(key) && valueMap.get(key) != null) {
                return valueMap.get(key, klass);
            }
        }

        return null;
    }

    /**
     * Looks for <..> substrings in the parameter string. If any are found it assume Rich text.
     *
     * @param str
     * @return
     */
    public static boolean isRichText(String str) {
        Matcher m = RICH_TEXT_PATTERN.matcher(str);

        return m.find();
    }
}
