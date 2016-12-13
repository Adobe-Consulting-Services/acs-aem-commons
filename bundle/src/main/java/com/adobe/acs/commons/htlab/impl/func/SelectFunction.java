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
import javax.script.Bindings;

import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import com.adobe.acs.commons.htlab.use.RSUse;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;

/**
 * Return a new instance of {@link RSUse} using the context bindings and wrapping the input value.
 */
@Component
@Service
@Property(name = HTLabFunction.OSGI_FN_NAME, value = "htlab:select", propertyPrivate = true)
public class SelectFunction implements HTLabFunction {

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        Bindings bindings = context.cloneBindings();
        bindings.remove(RSUse.B_PATH);
        bindings.remove(RSUse.B_WRAP);
        bindings.put(RSUse.B_WRAP, value);
        RSUse rs = new RSUse();
        rs.init(bindings);
        return HTLabMapResult.success(rs);
    }
}
