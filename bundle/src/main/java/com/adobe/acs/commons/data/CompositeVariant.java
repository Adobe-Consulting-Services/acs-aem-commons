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
package com.adobe.acs.commons.data;

import aQute.bnd.annotation.ProviderType;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a value which could be either a list of variants or a single variant. The idea is that this supports a
 * transition from a singular to a multi-value property for easier conversion.
 *
 * @param <T> Can be of any class supported by Variant, used for direct conversion in getValue, etc.
 */
@ProviderType
public final class CompositeVariant<T> {

    private final Class type;
    private final List<Variant> values = new ArrayList<>();

    /**
     * Create a variant either as a preferred type (set value later with addValue) or
     * with an initial value and the preferred type is assumed by the value provided.
     * @param initial 
     */
    public CompositeVariant(T initial) {
        if (initial instanceof Class) {
            this.type = (Class) initial;
        } else {
            this.type = initial.getClass();
            addValue(initial);
        }
    }

    public boolean isArray() {
        return type.isArray();
    }

    public Class<T> getSingularType() {
        if (isArray()) {
            return (Class<T>) type.getComponentType();
        } else {
            return type;
        }
    }

    public boolean isEmpty() {
        return values.isEmpty() || getValue() == null;
    }

    public final void addValue(Object val) {
        if (val instanceof Variant) {
            values.add((Variant) val);
        } else {
            values.add(new Variant(val));
        }
    }

    public T getValue() {
        return (T) getValueAs(getSingularType());
    }

    public <U> U getValueAs(Class<U> otherType) {
        return values.isEmpty() ? null : (U) values.get(0).asType(otherType);
    }

    public List<T> getValues() {
        return getValuesAs(getSingularType());
    }

    public <U> List<U> getValuesAs(Class<U> otherType) {
        return values.stream().map(v -> getValueAsType(v, otherType)).collect(Collectors.toList());
    }
    
    private <U> U getValueAsType(Variant v, Class<U> type) {
        // This shouldn't be necessary but it helps disambiguate a runtime lambda issue
        return v.asType(type);
    }

    @Override
    public String toString() {
        if (isArray()) {
            return getValues().toString();
        } else {
            return getValueAs(String.class);
        }
    }

    public Object toPropertyValue() {
        if (isArray()) {
            Object arr = Array.newInstance(getSingularType(), 0);
            return getValues().toArray((Object[]) arr);
        } else {
            return getValue();
        }
    }
}
