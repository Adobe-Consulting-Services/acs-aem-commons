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

import com.adobe.acs.commons.genericlists.GenericList;
import com.day.cq.wcm.api.NameConstants;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;

public final class GenericListImpl implements GenericList {

    static final String RT_GENERIC_LIST = "acs-commons/components/utilities/genericlist";
    static final String PN_VALUE = "value";
    static final String TITLE_PREFIX = NameConstants.PN_TITLE + ".";

    public static final class ItemImpl implements Item {

        private final String title;
        private final String value;
        private final ValueMap props;

        public ItemImpl(String t, String v, ValueMap props) {
            this.title = t;
            this.value = v;
            this.props = props;
        }

        @Override
        public String getTitle() {
            return title;
        }

        @Override
        public String getTitle(Locale locale) {
            // no locale - return default title
            if (locale == null) {
                return getTitle();
            }

            // no language - return default title
            String language = locale.getLanguage();
            if (language.length() == 0) {
                return getTitle();
            }

            String localizedTitle = null;

            // try property name like jcr:title.de_ch
            if (locale.getCountry().length() > 0) {
                localizedTitle = getLocalizedTitle(locale);
            }
            // then just jcr:title.de
            if (localizedTitle == null) {
                localizedTitle = getLocalizedTitle(new Locale(language));
            }
            if (localizedTitle == null) {
                return getTitle();
            } else {
                return localizedTitle;
            }
        }

        private String getLocalizedTitle(Locale locale) {
            return props.get(TITLE_PREFIX + locale.toString().toLowerCase(), String.class);
        }

        @Override
        public String getValue() {
            return value;
        }

    }

    private final List<Item> items;

    private final Map<String, Item> valueMapping;

    public GenericListImpl(Resource listParsys) {
        List<Item> tempItems = new ArrayList<>();
        Map<String, Item> tempValueMapping = new HashMap<>();
        Iterator<Resource> children = listParsys.listChildren();
        while (children.hasNext()) {
            Resource res = children.next();
            ValueMap map = res.getValueMap();
            String title = map.get(NameConstants.PN_TITLE, String.class);
            String value = map.get(PN_VALUE, String.class);
            if (title != null && value != null) {
                ItemImpl item = new ItemImpl(title, value, map);
                tempItems.add(item);
                tempValueMapping.put(value, item);
            }
        }
        items = Collections.unmodifiableList(tempItems);
        valueMapping = Collections.unmodifiableMap(tempValueMapping);
    }

    @Override
    public List<Item> getItems() {
        return items;
    }

    @Override
    public String lookupTitle(String value) {
        Item item = valueMapping.get(value);
        if (item != null) {
            return item.getTitle();
        } else {
            return null;
        }
    }

    @Override
    public String lookupTitle(String value, Locale locale) {
        Item item = valueMapping.get(value);
        if (item != null) {
            return item.getTitle(locale);
        } else {
            return null;
        }
    }

}
