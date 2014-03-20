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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import tldgen.Function;

/**
 * JSP functions for working with MultiFieldPanel widget.
 */
public class MultiFieldPanelFunctions {
    private static final Logger log = LoggerFactory.getLogger(MultiFieldPanelFunctions.class);

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
        ValueMap map = resource.adaptTo(ValueMap.class);
        List<Map<String, String>> results = new ArrayList<Map<String, String>>();
        if (map.containsKey(name)) {
            String[] values = map.get(name, new String[0]);
            for (String value : values) {

                try {
                    JSONObject parsed = new JSONObject(value);
                    Map<String, String> columnMap = new HashMap<String, String>();
                    for (Iterator<String> iter = parsed.keys(); iter.hasNext();) {
                        String key = iter.next();
                        String innerValue = parsed.getString(key);
                        columnMap.put(key, innerValue);
                    }

                    results.add(columnMap);

                } catch (JSONException e) {
                    log.error(
                            String.format("Unable to parse JSON in %s property of %s", name, resource.getPath()),
                            e);
                }

            }
        }
        return results;
    }
}
