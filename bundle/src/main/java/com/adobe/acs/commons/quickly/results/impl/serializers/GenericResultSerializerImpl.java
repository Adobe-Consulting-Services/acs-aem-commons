/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.quickly.results.impl.serializers;

import org.apache.sling.api.resource.ValueMap;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.ResultSerializer;
import com.google.gson.JsonObject;

/**
 * ACS AEM Commons - Quickly - Generic Result Serializer
 */
@Component(service = ResultSerializer.class, property= {
		ResultSerializer.PROP_TYPE + "=" + GenericResultSerializerImpl.TYPE
})
public class GenericResultSerializerImpl extends AbstractResultSerializer implements ResultSerializer {
    private static final Logger log = LoggerFactory.getLogger(GenericResultSerializerImpl.class);

    public static final String TYPE = "GENERIC";

    public JsonObject toJSON(final Result result, final ValueMap config) {
        log.trace("Entering Generic Result Serializer for {}", result.getPath());
        return super.toJSON(result);
    }
}
