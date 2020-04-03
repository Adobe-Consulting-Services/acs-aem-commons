/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.mcp.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.Resource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utilities for serializing objects back to value maps
 */
public class ValueMapSerializer {

    private static final Logger LOG = LoggerFactory.getLogger(ValueMapSerializer.class);

    public static void serializeToResource(Resource r, Object sourceObject) {
        Map<String, Object> map = r.adaptTo(ModifiableValueMap.class);
        if (map == null) {
            LOG.error("Unable to get modifiable value map for resource " + r.getPath());
        } else {
            serializeToMap(map, sourceObject);
        }
    }

    public static void serializeToMap(Map<String, Object> map, Object sourceObject) {
        if (sourceObject == null) {
            return;
        }
        FieldUtils.getAllFieldsList(sourceObject.getClass()).stream()
                .filter(IntrospectionUtil::isSimple)
                .forEach(f -> {
                    try {
                        Object value = FieldUtils.readField(f, sourceObject, true);
                        if (value != null) {
                            map.put(f.getName(), value);
                        }
                    } catch (IllegalAccessException ex) {
                        LOG.error("Exception while serializing", ex);
                    }
                });
    }

    public static String[] serializeToStringArray(Object value) {
        if (value == null) {
            return new String[0];
        } else if (value instanceof String[]) {
            return (String[]) value;
        } else if (value.getClass().isArray()) {
            List<String> values = (List) Arrays.asList((Object[]) value).stream().map(String::valueOf).collect(Collectors.toList());
            return (String[]) values.toArray(new String[0]);
        } else if (value instanceof Collection) {
            List<String> values = (List) ((Collection) value).stream().map(String::valueOf).collect(Collectors.toList());
            return (String[]) values.toArray(new String[0]);
        } else {
            return new String[]{value.toString()};
        }
    }

    private ValueMapSerializer() {
        //Utility class, not to be instantiated directly
    }
}
