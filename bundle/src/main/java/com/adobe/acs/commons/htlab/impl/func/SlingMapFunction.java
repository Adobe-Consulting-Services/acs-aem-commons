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
package com.adobe.acs.commons.htlab.impl.func;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * Maps a path input value using the resource resolver and request from context. Forwards non-string input values.
 */
@Component
@Service
@Property(name = HTLabFunction.OSGI_FN_NAME, value = "sling:map", propertyPrivate = true)
public class SlingMapFunction implements HTLabFunction {

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        if (value instanceof String && context.getResolver() != null && context.getRequest() != null) {
            String path = (String) value;
            String mapped = context.getResolver().map(context.getRequest(), path);
            return HTLabMapResult.success(mapped);
        }
        return HTLabMapResult.forwardValue();
    }
}
