package com.adobe.acs.commons.synth.impl.support;

import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**

 * @since 2018-10-09
 */
class SyntheticRequestParameterMap implements RequestParameterMap {
    private final Map<String, RequestParameter[]> delegate = new HashMap();

    SyntheticRequestParameterMap() {
    }

    public RequestParameter getValue(String name) {
        RequestParameter[] params = this.getValues(name);
        return params != null && params.length > 0 ? params[0] : null;
    }

    public RequestParameter[] getValues(String name) {
        return (RequestParameter[])this.delegate.get(name);
    }

    public int size() {
        return this.delegate.size();
    }

    public boolean isEmpty() {
        return this.delegate.isEmpty();
    }

    public boolean containsKey(Object key) {
        return this.delegate.containsKey(key);
    }

    public boolean containsValue(Object value) {
        return this.delegate.containsValue(value);
    }

    public RequestParameter[] get(Object key) {
        return (RequestParameter[])this.delegate.get(key);
    }

    public RequestParameter[] put(String key, RequestParameter[] value) {
        return (RequestParameter[])this.delegate.put(key, value);
    }

    public RequestParameter[] remove(Object key) {
        return (RequestParameter[])this.delegate.remove(key);
    }

    public void putAll(Map<? extends String, ? extends RequestParameter[]> m) {
        this.delegate.putAll(m);
    }

    public void clear() {
        this.delegate.clear();
    }

    public Set<String> keySet() {
        return this.delegate.keySet();
    }

    public Collection<RequestParameter[]> values() {
        return this.delegate.values();
    }

    public Set<Entry<String, RequestParameter[]>> entrySet() {
        return this.delegate.entrySet();
    }

    public boolean equals(Object o) {
        return this.delegate.equals(o);
    }

    public int hashCode() {
        return this.delegate.hashCode();
    }
}

