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
package com.adobe.acs.commons.wcm.comparisons.impl.lines;

import com.google.common.base.Function;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.Iterables;

import java.io.Serializable;
import java.util.List;

class Stepper<T> {

    private final List<Serializable> ids;
    private final Iterable<T> values;
    private final Function<T, Serializable> toId;

    private int step = 0;

    Stepper(Iterable<T> steps, Function<T, Serializable> toId) {
        this.toId = toId;
        this.values = steps;
        this.ids = FluentIterable.from(steps).transform(toId).toList();
    }

    public T next() {
        T ret = Iterables.size(values) > step ? Iterables.get(values, step) : null;
        step++;
        return ret;
    }

    public int positionOfIdAfterCurrent(T t) {
        if (t != null) {
            Serializable searchId = toId.apply(t);
            for (int i = step; i < ids.size(); i++) {
                if (ids.get(i).equals(searchId)) {
                    return i + 1 - step;
                }
            }
        }
        return -1;
    }

    public boolean isEmpty() {
        return step >= ids.size();
    }
}
