/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.htlab;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.adobe.acs.commons.htlab.use.MapUse;

/**
 * Interface for the service that coordinates binding of OSGi-registered {@link HTLabFunction} services to fnNames,
 * exposing a single apply method for the {@link MapUse} class.
 */
public interface HTLabMapService {

    /**
     * Loads the function specified by {@code fnName} and applies it to the input value,
     * and returns the appropriate result value. If no such function is found, this service will return
     * {@link HTLabMapResult#forwardValue()}. If an exception is caught, the service will return
     * {@link HTLabMapResult#failure(Throwable)}.
     * @param context the wrapped {@link javax.script.Bindings} map
     * @param fnName the name of the function to apply
     * @param key the original property key, if specified, or the empty string if the value is the target of
     *            the {@code path=} or {@code wrap=} attributes
     * @param value the input value
     * @return the function result
     */
    @Nonnull
    HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull final String fnName,
                         @Nonnull final String key, @CheckForNull final Object value);
}
