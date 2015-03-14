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

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;

import java.util.HashMap;
import java.util.Map;

@ProviderType
public class Form {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Form.class);
    private final Map<String, String> data;
    private final Map<String, String> errors;
    private String name;
    private String resourcePath;

    public Form(final String name, final String resourcePath) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = new HashMap<String, String>();
        this.errors = new HashMap<String, String>();
    }

    public Form(final String name, final String resourcePath, final Map<String, String> data) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = data;
        this.errors = new HashMap<String, String>();
    }

    public Form(final String name, final String resourcePath, final Map<String, String> data, final Map<String, String> errors) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = data;
        this.errors = errors;
    }

    /**
     * Get the Form's name
     * <p>
     * This should uniquely identify a Form on a Page
     *
     * @return
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the Form name
     * <p>
     * Typically this setter is not used and Form names are set in constructor.
     * <p>
     * This can be helpful for changing the flow or using Form X to populate Form Y
     *
     * @param name
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Get the Form's resource path
     *
     * @return
     */
    public String getResourcePath() {
        return this.resourcePath;
    }

    /**
     * Sets the Form's resource path
     *
     * @param resourcePath
     */
    public void setResourcePath(final String resourcePath) {
        this.resourcePath = resourcePath;
    }

    /**
     * Gets a Map of the Form data
     *
     * @return
     */
    public Map<String, String> getData() {
        return this.data;
    }

    /**
     * Gets a Map of the error data
     *
     * @return
     */
    public Map<String, String> getErrors() {
        return this.errors;
    }

    /**
     * Determines if a Form data key exists and has non-blank data
     *
     * @param key
     * @return
     */
    public boolean has(final String key) {
        final String val = this.get(key);
        return (StringUtils.isNotBlank(val));
    }

    /**
     * Gets the data associated with a Form data key
     *
     * @param key
     * @return
     */
    public String get(final String key) {
        final String val = this.data.get(key);
        return StringUtils.stripToEmpty(val);
    }

    /**
     * Sets Form data
     *
     * @param key
     * @param value
     */
    public void set(final String key, final String value) {
        this.data.put(key, value);
    }

    /**
     * Determines if any Form Data exists; atleast 1 key w non-blank data must exist in the data map.
     *
     * @return
     */
    public boolean hasData() {
        if (!this.data.isEmpty()) {
            return false;
        }

        for (String key : this.data.keySet()) {
            if (this.has(key)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Determines if an error exists
     *
     * @param key
     * @return
     */
    public boolean hasError(final String key) {
        return this.errors.containsKey(key);
    }

    /**
     * Gets the error message
     *
     * @param key
     * @return
     */
    public String getError(final String key) {
        final String val = this.errors.get(key);
        return StringUtils.stripToEmpty(val);
    }

    /**
     * Sets an error
     * <p>
     * This is used if no corresponding error message/data is required to be associated; and the only information required is that an error occurred against key X.
     *
     * @param key
     */
    public void setError(final String key) {
        this.errors.put(key, null);
    }

    /**
     * Sets an error for key with corresponding error message/data
     *
     * @param key
     * @param value
     */
    public void setError(final String key, final String value) {
        this.errors.put(key, value);
    }

    /**
     * Checks if has data
     *
     * @return
     */
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    /**
     * Get data as ValueMap
     *
     * @return
     */
    public ValueMap getValueMap() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(this.data);
        return new ValueMapDecorator(map);
    }

    /**
     * Get Errors as ValueMap
     *
     * @return
     */
    public ValueMap getErrorsValueMap() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(this.errors);
        return new ValueMapDecorator(map);
    }
}