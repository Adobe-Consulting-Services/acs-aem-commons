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
@Deprecated
public abstract class BiConsumer<T, U> implements CheckedBiConsumer<T, U> {
    public static <X, Y> BiConsumer<X, Y> adapt(CheckedBiConsumer<X, Y> delegate) {
        return new Adapter<>(delegate);
    }

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
    public BiConsumer<T, U> andThen(final BiConsumer<? super T, ? super U> after) {
        return new Adapter(andThen((CheckedBiConsumer) after));
    }
    
    private static class Adapter<T, R> extends BiConsumer<T, R> {

        final private CheckedBiConsumer<T, R> delegate;

        public Adapter(CheckedBiConsumer<T, R> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void accept(T t, R r) throws Exception {
            delegate.accept(t, r);
        }
    }
}