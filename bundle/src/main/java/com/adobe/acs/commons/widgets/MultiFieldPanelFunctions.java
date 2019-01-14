/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.widgets;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import aQute.bnd.annotation.ProviderType;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import tldgen.Function;

/**
 * JSP functions for working with MultiFieldPanel widget.
 */
@ProviderType
public final class MultiFieldPanelFunctions {
    private static final Logger log = LoggerFactory.getLogger(MultiFieldPanelFunctions.class);

    private MultiFieldPanelFunctions() {
    }

    /**
     * Extract the value of a MultiFieldPanel property into a list of maps. Will never return
     * a null map, but may return an empty one. Invalid property values are logged and skipped.
     * 
     * @param resource the resource
     * @param name the property name
     * @return a list of maps.
     */
    @Function
    public static List<Map<String, String>> getMultiFieldPanelValues(Resource resource, String name) {
        Gson gson = new Gson();
        ValueMap map = resource.adaptTo(ValueMap.class);
        List<Map<String, String>> results = new ArrayList<>();
        if (map != null && map.containsKey(name)) {
            String[] values = map.get(name, new String[0]);
            for (String value : values) {

                try {
                    results.add(gson.fromJson(value, Map.class));

                } catch (JsonParseException e) {
                    log.error(
                            String.format("Unable to parse JSON in %s property of %s", name, resource.getPath()),
                            e);
                }

            }
        }
        return results;
    }
}
