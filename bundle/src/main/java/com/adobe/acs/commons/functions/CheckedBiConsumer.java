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
import java.util.function.BiConsumer;

/**
 * Created work-alike for functionality not introduced until Java 8
 * Represents an operation that accepts two input arguments and returns no
 * result.  This is the two-arity specialization of {@link Consumer}.
 * Unlike most other functional interfaces, {@code BiConsumer} is expected
 * to operate via side-effects.
 *
 * @param <T> the type of the first argument to the operation
 * @param <U> the type of the second argument to the operation
 *
 * @see Consumer
 */
@ConsumerType
@FunctionalInterface
@SuppressWarnings("squid:S00112")
public interface CheckedBiConsumer<T, U> {

    static <T,U> CheckedBiConsumer<T,U> from(BiConsumer<T,U> handler) {
        return handler == null ? null : (t, u) -> handler.accept(t, u);
    }

    /**
     * Performs this operation on the given arguments.
     *
     * @param t the first input argument
     * @param u the second input argument
     * @throws java.lang.Exception
     */
    void accept(T t, U u) throws Exception;

    /**
     * Returns a composed {@code BiConsumer} that performs, in sequence, this
     * operation followed by the {@code after} operation. If performing either
     * operation throws an exception, it is relayed to the caller of the
     * composed operation.  If performing this operation throws an exception,
     * the {@code after} operation will not be performed.
     *
     * @param after the operation to perform after this operation
     * @return a composed {@code BiConsumer} that performs in sequence this
     * operation followed by the {@code after} operation
     * @throws NullPointerException if {@code after} is null
     */
    default CheckedBiConsumer<T, U> andThen(final CheckedBiConsumer<? super T, ? super U> after) {
        if (after == null) {
            throw new NullPointerException();
        }
        return (T t, U u) -> {
            accept(t, u);
            after.accept(t, u);
        };
    }
}