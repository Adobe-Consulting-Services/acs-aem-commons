/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
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
