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
package com.adobe.acs.commons.htlab.use;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import javax.script.Bindings;

import aQute.bnd.annotation.ConsumerType;
import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.sling.api.adapter.Adaptable;
import org.apache.sling.scripting.sightly.pojo.Use;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple example of a Use Function, implementing both {@link Use} and {@link HTLabFunction}. Use this as a placeholder
 * for functions that must produce a string, but which are not yet implemented. All it does is produce a String for an
 * input value, and logs it for good measure. It even accepts an onNull= parameter, which sets a default return value
 * so that null is not returned.
 */
@ConsumerType
public class ToStringUseFn implements Use, HTLabFunction {
    private static final Logger LOG = LoggerFactory.getLogger(ToStringUseFn.class);

    private static final String P_ON_NULL = "onNull";

    private HTLabContext context;
    private String onNull;

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        String result = value == null ? this.onNull : getString(value);
        getLog().info("[ToStringUseFn.apply] key={} value={}. Isn't there a better function to use here?",
                key, result);
        return HTLabMapResult.success(result);
    }

    @Override
    public void init(Bindings bindings) {
        this.context = HTLabContext.fromBindings(bindings);
        this.onNull = this.context.get(P_ON_NULL, String.class);
    }

    private Logger getLog() {
        return this.context.getLog() != null ? this.context.getLog() : LOG;
    }

    private String getString(@Nonnull Object value) {
        if (value instanceof Adaptable) {
            String result = ((Adaptable) value).adaptTo(String.class);
            if (result != null) {
                return result;
            }
        }
        return String.valueOf(value);
    }
}
