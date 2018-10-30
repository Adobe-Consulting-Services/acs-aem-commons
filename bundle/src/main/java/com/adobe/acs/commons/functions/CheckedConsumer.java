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

/**
 * Created work-alike for functionality not introduced until Java 8
 * Represents an operation that accepts a single input argument and returns no
 * result. Unlike most other functional interfaces, {@code Consumer} is expected
 * to operate via side-effects.
 *
 * @param <T> the type of the input to the operation
 */
@ConsumerType
@FunctionalInterface
@SuppressWarnings("squid:S00112")
public interface CheckedConsumer<T> {
    static <T> CheckedConsumer<T> from(java.util.function.Consumer<T> consumer) {
        return consumer == null ? null : t -> consumer.accept(t);
    }

    void accept(T t) throws Exception;

    default CheckedConsumer<T> andThen(final CheckedConsumer<? super T> after) {
        if (after == null) {
            throw new NullPointerException();
        }
        return (T t) -> {
            accept(t);
            after.accept(t);
        };
    }
}
