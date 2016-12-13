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

import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import com.adobe.acs.commons.htlab.HTLabContext;
import com.adobe.acs.commons.htlab.HTLabFunction;
import com.adobe.acs.commons.htlab.HTLabMapResult;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.ISO8601;
import org.apache.sling.api.adapter.Adaptable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Formats a {@link java.util.Calendar} value to conform with JavaScript's {@code Date.toJSON()}.
 */
@Component
@Service
@Property(name = HTLabFunction.OSGI_FN_NAME, value = "jsonDate")
public class JsonDateFunction implements HTLabFunction {
    private static final Logger LOG = LoggerFactory.getLogger(JsonDateFunction.class);

    @Nonnull
    @Override
    public HTLabMapResult apply(@Nonnull HTLabContext context, @Nonnull String key, @CheckForNull Object value) {
        LOG.debug("[jsonDate] key={}, value={}", key, value);
        // The happy path is when the value is already a Calendar instance.
        if (value instanceof Calendar) {
            return HTLabMapResult.success(ISO8601.format((Calendar) value));
        } else if (value instanceof Date) {
            // but we can also deal with a Date by assuming GMT.
            Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT"));
            cal.setTime((Date) value);
            return HTLabMapResult.success(ISO8601.format(cal));
        } else if (value instanceof Adaptable) {
            // It is also possible that a path resolves to a JCR property resource,
            // so try to adapt to Calendar
            Calendar cal = ((Adaptable) value).adaptTo(Calendar.class);
            if (cal != null) {
                return HTLabMapResult.success(ISO8601.format(cal));
            }
        } else if (value instanceof String){
            // if the value is already string, we can sanity check the value by failing
            // if it does not parse as ISO8601
            return HTLabMapResult.notNullOrFailure(ISO8601.parse((String) value));
        }

        // jsonDate has a very deterministic output, and should be expected to fail when
        // preconditions are not met.
        return HTLabMapResult.failure();
    }
}
