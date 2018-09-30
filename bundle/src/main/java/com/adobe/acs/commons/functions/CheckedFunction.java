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

import aQute.bnd.annotation.ConsumerType;
import java.util.function.Function;

/**
 * Created work-alike for functionality not introduced until Java 8
 * Represents a function that accepts one argument and produces a result.
 *
 * @param <T> the type of the input to the function
 * @param <R> the type of the result of the function
 */
@ConsumerType
@FunctionalInterface
@SuppressWarnings("squid:S00112")
public interface CheckedFunction<T, R> {

    public static <T,R> CheckedFunction<T,R> from(Function<T,R> function) {
        return t -> function.apply(t);
    }
    
    /**
     * Applies this function to the given argument.
     *
     * @param t the function argument
     * @return the function result
     * @throws java.lang.Exception
     */
    public R apply(T t) throws Exception;

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
     * @see #andThen(IFunction)
     */
    default <V> CheckedFunction<V, R> compose(final CheckedFunction<? super V, ? extends T> before) {
        if (before == null) {
            throw new NullPointerException();
        }
        return (V t) -> apply(before.apply(t));
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
     * @see #compose(IFunction)
     */
    default <V> CheckedFunction<T, V> andThen(final CheckedFunction<? super R, ? extends V> after) {
        if (after == null) {
            throw new NullPointerException();
        }
        return (T t) -> after.apply(apply(t));
    }

    /**
     * Returns a function that always returns its input argument.
     *
     * @param <T> the type of the input and output objects to the function
     * @return a function that always returns its input argument
     */
    public static <T> CheckedFunction<T, T> identity() {
        return (T t) -> t;
    }

    public static <T> CheckedFunction<T, Boolean> or(CheckedFunction<T, Boolean>... functions) {
        return t -> {
            for (CheckedFunction<T, Boolean> f : functions) {
                if (f.apply(t)) {
                    return true;
                }
            }
            return false;
        };
    }    
}