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
