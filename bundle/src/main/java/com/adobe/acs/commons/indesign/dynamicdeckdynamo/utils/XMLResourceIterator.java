package com.adobe.acs.commons.indesign.dynamicdeckdynamo.utils;

import org.apache.sling.api.resource.Resource;

import java.util.ListIterator;

/**
 * The Iterator Class which is used for Dynamic Deck Indd XML Generation from Annotation based XML.
 */
public class XMLResourceIterator {

    // key is the attribute which has to be there on ITERABLE nodes of
    // template for iterator to run for those nodes.
    private String key;

    // ListIterator for the Resource Data
    private ListIterator<Resource> iterator;

    public XMLResourceIterator(String key, ListIterator<Resource> iterator) {
        this.key = key;
        this.iterator = iterator;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public ListIterator<Resource> getIterator() {
        return iterator;
    }

    public void setIterator(ListIterator<Resource> iterator) {
        this.iterator = iterator;
    }
}
