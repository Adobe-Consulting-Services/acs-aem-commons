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
package com.adobe.acs.commons.functions;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Provides a thread-safe iterator that loops though a list, useful for providing
 * a circular list for round-robin uses.
 * @param <T> Type being iterated in the list
 */
public class RoundRobin<T> implements Iterable<T> {
    private final List<T> items;

    public RoundRobin(final List<T> coll) {
        this.items = Collections.unmodifiableList(coll);
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private final AtomicInteger index = new AtomicInteger(0);

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized T next() {
                int idx = index.getAndUpdate(x -> {
                    // handle overflow
                    if (x == Integer.MAX_VALUE) {
                        return (x % items.size()) + 1;
                    } else {
                        return ++x;
                    }
                });
                return items.get(idx % items.size());
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("remove not allowed");
            }
        };
    }
}
