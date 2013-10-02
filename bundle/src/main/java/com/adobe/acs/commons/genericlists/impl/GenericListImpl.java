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
package com.adobe.acs.commons.genericlists.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

import com.adobe.acs.commons.genericlists.GenericList;
import com.day.cq.wcm.api.NameConstants;

public class GenericListImpl implements GenericList {

    static final String TMPL_GENERIC_LIST = "/apps/acs-commons/templates/utilities/genericlist";
    static final String PN_VALUE = "value";

    public static class ItemImpl implements Item {

        private final String title;
        private final String value;

        public ItemImpl(String t, String v) {
            this.title = t;
            this.value = v;
        }

        public final String getTitle() {
            return title;
        }

        public final String getValue() {
            return value;
        }

    }

    private final List<Item> items;

    private final Map<String, String> valueMapping;

    public GenericListImpl(Resource listParsys) {
        List<Item> tempItems = new ArrayList<Item>();
        Map<String, String> tempValueMapping = new HashMap<String, String>();
        Iterator<Resource> children = listParsys.listChildren();
        while (children.hasNext()) {
            Resource res = children.next();
            ValueMap map = res.adaptTo(ValueMap.class);
            String title = map.get(NameConstants.PN_TITLE, String.class);
            String value = map.get(PN_VALUE, String.class);
            if (title != null && value != null) {
                tempItems.add(new ItemImpl(title, value));
                tempValueMapping.put(value, title);
            }
        }
        items = Collections.unmodifiableList(tempItems);
        valueMapping = Collections.unmodifiableMap(tempValueMapping);
    }

    public final List<Item> getItems() {
        return items;
    }

    public final String lookupTitle(String value) {
        return valueMapping.get(value);
    }

}
