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
 * Represents a function that accepts one argument and produces a result.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 * @deprecated Use CheckedFunction instead
 */
@ConsumerType
@Deprecated
public abstract class Function<T, R> implements CheckedFunction<T, R> {
    /**
     * Returns a composed function that first applies the {@code before}
     * function to its input, and then applies this function to the result.
     * If evaluation of either function throws an exception, it is relayed to
     * the caller of the composed function.
     *
     * @param <V> the type of input to the {@code before} function, and to the
     *           composed function
     * @param before the function to apply before this function is applied
     * @return a composed function that first applies the {@code before}
     * function and then applies this function
     * @throws NullPointerException if before is null
     *
     * @see #andThen(Function)
     */
    public <V> Function<V, R> compose(final Function<? super V, ? extends T> before) {
        return adapt(compose((CheckedFunction) before));
    }

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
     *
     * @see #compose(Function)
     */
    public <V> Function<T, V> andThen(final Function<? super R, ? extends V> after) {
        return adapt(compose((CheckedFunction) after));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    public static <T> Function<T, T> identity() {
        return adapt(CheckedFunction.identity());
    }

    public static <X, Y> Function<X, Y> adapt(CheckedFunction<X, Y> delegate) {
        return new Adapter<>(delegate);
    }

    private static class Adapter<T, R> extends Function<T, R> {

        private final CheckedFunction<T, R> delegate;

        public Adapter(CheckedFunction<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public R apply(T t) throws Exception {
            return delegate.apply(t);
        }
    }
}