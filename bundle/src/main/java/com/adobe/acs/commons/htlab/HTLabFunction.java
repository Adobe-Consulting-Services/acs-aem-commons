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

import com.adobe.acs.commons.htlab.use.RSUse;
import org.osgi.framework.Constants;

/**
 * Implement this interface to provide a map function for the {@link RSUse} use
 * class. The shape of the function used in HTL is essentially one which accepts a single untyped argument, the value
 * of which is evaluated from the left-hand side of the {@code $} operator, and which may be {@code null}.
 *
 * The function may return {@link HTLabMapResult#forwardValue()} to signify that the input value should be passed
 * through unmodified to the right-hand side of the expression. Otherwise, the value returned by the function will be
 * passed, untyped, to the {@code $}-delimited function specified to the immediate right in the expression, or will
 * stand as the evaluated result of the expression if no other function is specified.
 *
 * If the function wishes to cancel the result based on a non-null input value, such that evaluation of the broader
 * expression would fail a {@code data-sly-test} attribute, the function should return
 * {@link HTLabMapResult#failure()}.
 *
 * The function is also allowed to incorporate external state into the computation of the result, either via the
 * {@link javax.script.Bindings} provided during the construction of the {@link RSUse}
 * class, or via OSGi configuration specific to the function as a managed service.
 */
public interface HTLabFunction {

    String REGEX_FN_NAME = "^(\\w+:)?\\w+$";

    /**
     * This OSGi service property is required for registration. The value MUST consist of only of word characters plus
     * a single optional colon denoting an ad-hoc namespace. The value must satisfy the regular expression
     * defined in {@link #REGEX_FN_NAME}.
     */
    String OSGI_FN_NAME = "htlab.use.fnName";

    /**
     * The optional {@code service.ranking} property. The default value is zero. Higher values will override lower
     * values between {@link HTLabFunction} services of the same {@link #OSGI_FN_NAME}. See
     * {@link Constants#SERVICE_RANKING} for specifics.
     */
    String OSGI_FN_RANK = Constants.SERVICE_RANKING;

    /**
     * Applies the function to the input value and returns the appropriate result value.
     * @param context the wrapped {@link javax.script.Bindings} map
     * @param key the original property key, if specified, or the empty string if the value is the target of
     *            the {@code path=} or {@code wrap=} attributes
     * @param value the input value
     * @return the result of the function application
     */
    @Nonnull
    HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull final String key, @CheckForNull final Object value);
}
