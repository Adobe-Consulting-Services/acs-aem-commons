/*
 * Copyright 2016 Adobe.
 *
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
 */
package com.adobe.acs.commons.functions;

import aQute.bnd.annotation.ConsumerType;

/**
 * Created work-alike for functionality not introduced until Java 8
 * Represents a function that accepts two arguments and produces a result.
 * This is the two-arity specialization of {@link Function}.
 *
 * @param <T> the type of the first argument to the function
 * @param <U> the type of the second argument to the function
 * @param <R> the type of the result of the function
 *
 * @see Function
 * @deprecated Use CheckedBiFunction instead
 */
@ConsumerType
@Deprecated
public abstract class BiFunction<T, U, R> implements CheckedBiFunction<T, U, R> {
    /**
     * Returns a composed function that first applies this function to
     * its input, and then applies the {@code after} function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of output of the {@code after} function, and of the
     *           composed function
     * @param after the function to apply after this function is applied
     * @return a composed function that first applies this function and then
     * applies the {@code after} function
     * @throws NullPointerException if after is null
     */
    public <V> BiFunction<T, U, V> andThen(final Function<? super R, ? extends V> after) {
        return adapt(andThen((CheckedFunction) after));
    }

    public static <X, Y, Z> BiFunction<X, Y, Z> adapt(CheckedBiFunction<X, Y, Z> delegate) {
        return new Adapter<>(delegate);
    }

    private static class Adapter<T, U, R> extends BiFunction<T, U, R> {

        private final CheckedBiFunction<T, U, R> delegate;

        public Adapter(CheckedBiFunction<T, U, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public R apply(T t, U u) throws Exception {
            return delegate.apply(t, u);
        }
    }
}