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

package com.adobe.acs.commons.workflow.synthetic.impl;

import com.day.cq.workflow.metadata.MetaDataMap;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class SyntheticMetaDataMap implements MetaDataMap, com.adobe.granite.workflow.metadata.MetaDataMap {
    private final ValueMap metaDataMap;

    public SyntheticMetaDataMap() {
        this.metaDataMap = new ValueMapDecorator(new HashMap<String, Object>());
    }

    public SyntheticMetaDataMap(Map<String, Object> map) {
        if (map == null) {
            map = new HashMap<String, Object>();
        }

        this.metaDataMap = new ValueMapDecorator(map);
    }

    @Override
    public final <T> T get(final String s, final Class<T> tClass) {
        return this.metaDataMap.get(s, tClass);
    }

    @Override
    public final <T> T get(final String s, final T t) {
        return this.metaDataMap.get(s, t);
    }

    @Override
    public final Object get(final Object o) {
        return this.metaDataMap.get(o);
    }

    @Override
    public final int size() {
        return this.metaDataMap.size();
    }

    @Override
    public final boolean isEmpty() {
        return this.metaDataMap.isEmpty();
    }

    @Override
    public final boolean containsKey(final Object o) {
        return this.metaDataMap.containsKey(o);
    }

    @Override
    public final boolean containsValue(final Object o) {
        return this.metaDataMap.containsValue(o);
    }

    @Override
    public final Object put(final String s, final Object o) {
        return this.metaDataMap.put(s, o);
    }

    @Override
    public final Object remove(final Object o) {
        return this.metaDataMap.remove(o);
    }

    @Override
    public final void putAll(final Map<? extends String, ?> map) {
        this.metaDataMap.putAll(map);
    }

    @Override
    public final void clear() {
        this.metaDataMap.clear();
    }

    @Override
    public final Set<String> keySet() {
        return this.metaDataMap.keySet();
    }

    @Override
    public final Collection<Object> values() {
        return this.metaDataMap.values();
    }

    @Override
    public final Set<Entry<String, Object>> entrySet() {
        return this.metaDataMap.entrySet();
    }

    void resetTo(Map<String, ? extends Object> newData) {
        Map<String, ? extends Object> tmp = new HashMap(newData);
        this.metaDataMap.clear();
        this.metaDataMap.putAll(tmp);
    }
}
