package com.adobe.acs.commons.functions;

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
        this.items = coll;
    }

    @Override
    public Iterator<T> iterator() {
        return new Iterator<T>() {

            private AtomicInteger index = new AtomicInteger(0);

            @Override
            public boolean hasNext() {
                return true;
            }

            @Override
            public synchronized T next() {
                int idx = index.getAndIncrement() % items.size();
                return items.get(idx);
            }

            @Override
            public void remove() {
                throw new IllegalArgumentException("remove not allowed");
            }
        };
    }
}
