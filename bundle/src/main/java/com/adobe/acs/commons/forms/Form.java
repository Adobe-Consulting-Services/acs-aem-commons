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
package com.adobe.acs.commons.forms;

import aQute.bnd.annotation.ProviderType;
import org.apache.sling.api.resource.ValueMap;

import java.util.Map;

@ProviderType
public interface Form {
    /**
     * Get the Form's name
     * <p>
     * This should uniquely identify a Form on a Page
     *
     * @return
     */
    String getName();

    /**
     * Sets the Form name
     * <p>
     * Typically this setter is not used and Form names are set in constructor.
     * <p>
     * This can be helpful for changing the flow or using Form X to populate Form Y
     *
     * @param name
     */
    void setName(String name);

    /**
     * Get the Form's resource path
     *
     * @return
     */
    String getResourcePath();

    /**
     * Sets the Form's resource path
     *
     * @param resourcePath
     */
    void setResourcePath(String resourcePath);

    /**
     * Gets a Map of the Form data
     *
     * @return
     */
    Map<String, String> getData();

    /**
     * Gets a Map of the error data
     *
     * @return
     */
    Map<String, String> getErrors();

    /**
     * Determines if a Form data key exists and has non-blank data
     *
     * @param key
     * @return
     */
    boolean has(String key);

    /**
     * Gets the data associated with a Form data key
     *
     * @param key
     * @return
     */
    String get(String key);

    /**
     * Sets Form data
     *
     * @param key
     * @param value
     */
    void set(String key, String value);

    /**
     * Determines if any Form Data exists; atleast 1 key w non-blank data must exist in the data map.
     *
     * @return
     */
    boolean hasData();

    /**
     * Determines if an error exists
     *
     * @param key
     * @return
     */
    boolean hasError(String key);

    /**
     * Gets the error message
     *
     * @param key
     * @return
     */
    String getError(String key);

    /**
     * Sets an error
     * <p>
     * This is used if no corresponding error message/data is required to be associated; and the only information required is that an error occurred against key X.
     *
     * @param key
     */
    void setError(String key);

    /**
     * Sets an error for key with corresponding error message/data
     *
     * @param key
     * @param value
     */
    void setError(String key, String value);

    /**
     * Checks if has data
     *
     * @return
     */
    boolean hasErrors();

    /**
     * Get data as ValueMap
     *
     * @return
     */
    ValueMap getValueMap();

    /**
     * Get Errors as ValueMap
     *
     * @return
     */
    ValueMap getErrorsValueMap();
}
