package com.adobe.acs.commons.forms;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Form {
    @SuppressWarnings("unused")
	private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

	private final String name;
	private final Map<String, String> data;
	private final Map<String, String> errors;
    private final Resource resource;

	public Form(final String name, final Resource resource) {
		this.name = name;
        this.resource = resource;
		this.data = new HashMap<String, String>();
		this.errors = new HashMap<String, String>();
	}
	
	public Form(final String name, final Resource resource, final Map<String, String> data) {
		this.name = name;
        this.resource = resource;
		this.data = data;
		this.errors = new HashMap<String, String>();
	}

	public Form(final String name, final Resource resource, final Map<String, String> data, final Map<String, String> errors) {
		this.name = name;
        this.resource = resource;
        this.data = data;
		this.errors = errors;
	}

    /**
     * @return the form's name
     */
	public String getName() {
		return this.name;
	}

	public Map<String, String> getData() {
		return this.data;
	}

	public Map<String, String> getErrors() {
		return this.errors;
	}

	public boolean has(final String key) {
		final String val = this.get(key);
		return (StringUtils.isNotBlank(val));
	}

	public String get(final String key) {
		final String val = this.data.get(key);
		return StringUtils.stripToEmpty(val);
	}

	public void set(final String key, final String value) {
		this.data.put(key, value);
	}

	public boolean hasData() {
		return !this.data.isEmpty();
	}

	public boolean hasError(final String key) {
		return this.errors.containsKey(key);
	}

	public String getError(final String key) {
		final String val = this.errors.get(key);
		return StringUtils.stripToEmpty(val);
	}

	public void setError(final String key) {
		this.errors.put(key, null);
	}

	public void setError(final String key, final String value) {
		this.errors.put(key, value);
	}

	public boolean hasErrors() {
		return !this.errors.isEmpty();
	}

	public ValueMap getValueMap() {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.putAll(this.data);
		return new ValueMapDecorator(map);
	}

	public ValueMap getErrorsValueMap() {
		final Map<String, Object> map = new HashMap<String, Object>();
		map.putAll(this.errors);
		return new ValueMapDecorator(map);
	}

    public Resource getResource() {
        return this.resource;
    }
}