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
package com.adobe.acs.commons.forms.impl;

import com.adobe.acs.commons.forms.Form;
import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class FormImpl implements Form {
    @SuppressWarnings("unused")
    private static final Logger log = LoggerFactory.getLogger(Form.class);
    private final Map<String, String> data;
    private final Map<String, String> errors;
    private String name;
    private String resourcePath;

    public FormImpl(final String name, final String resourcePath) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = new HashMap<String, String>();
        this.errors = new HashMap<String, String>();
    }

    public FormImpl(final String name, final String resourcePath, final Map<String, String> data) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = data;
        this.errors = new HashMap<String, String>();
    }

    public FormImpl(final String name, final String resourcePath, final Map<String, String> data, final Map<String, String> errors) {
        this.name = name;
        this.resourcePath = resourcePath;
        this.data = data;
        this.errors = errors;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public void setName(final String name) {
        this.name = name;
    }

    @Override
    public String getResourcePath() {
        return this.resourcePath;
    }

    @Override
    public void setResourcePath(final String resourcePath) {
        this.resourcePath = resourcePath;
    }

    @Override
    public Map<String, String> getData() {
        return this.data;
    }

    @Override
    public Map<String, String> getErrors() {
        return this.errors;
    }

    @Override
    public boolean has(final String key) {
        final String val = this.get(key);
        return (StringUtils.isNotBlank(val));
    }

    @Override
    public String get(final String key) {
        final String val = this.data.get(key);
        return StringUtils.stripToEmpty(val);
    }

    @Override
    public void set(final String key, final String value) {
        this.data.put(key, value);
    }

    @Override
    public boolean hasData() {
        if (this.data.isEmpty()) {
            return false;
        }

        for (String key : this.data.keySet()) {
            if (this.has(key)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasError(final String key) {
        return this.errors.containsKey(key);
    }

    @Override
    public String getError(final String key) {
        final String val = this.errors.get(key);
        return StringUtils.stripToEmpty(val);
    }

    @Override
    public void setError(final String key) {
        this.errors.put(key, null);
    }

    @Override
    public void setError(final String key, final String value) {
        this.errors.put(key, value);
    }

    @Override
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    @Override
    public ValueMap getValueMap() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(this.data);
        return new ValueMapDecorator(map);
    }

    @Override
    public ValueMap getErrorsValueMap() {
        final Map<String, Object> map = new HashMap<String, Object>();
        map.putAll(this.errors);
        return new ValueMapDecorator(map);
    }
}