/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
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

import org.osgi.annotation.versioning.ConsumerType;
import java.util.function.Supplier;

/**
 * Supplier function which allows throwing exceptions
 * @param <T> Type being supplied
 */
@ConsumerType
@FunctionalInterface
@SuppressWarnings("squid:S00112")
public interface CheckedSupplier<T> {
    static <T> CheckedSupplier<T> fromSupplier(Supplier<T> supplier) {
        return supplier::get;
    }

    /**
     * Gets a result.
     *
     * @return a result
     */
    T get() throws Exception;
}