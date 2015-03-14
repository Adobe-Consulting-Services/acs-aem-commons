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
package com.adobe.acs.commons.genericlists;

import java.util.List;
import java.util.Locale;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import aQute.bnd.annotation.ProviderType;

/**
 * A generic list of title/value pairs.
 */
@ProviderType
public interface GenericList {

    /**
     * Return an ordered list of title/value pairs.
     * 
     * @return the item list
     */
    @Nonnull
    List<Item> getItems();

    /**
     * Get an item's title by its value.
     * 
     * @param value the list item's value
     * @return the title or null
     */
    @CheckForNull
    String lookupTitle(String value);

    /**
     * Get an item's localized title by its value.
     * 
     * @param value the list item's value
     * @param locale the locale for localization
     * @return the title or null
     */
    @CheckForNull
    String lookupTitle(String value, Locale locale);

    /**
     * A generic item/value pair within a list.
     *
     */
    interface Item {

        /**
         * Get the item's title.
         * 
         * @return the title
         */
        @Nonnull
        String getTitle();

        /**
         * Get the item's localized title.
         * 
         * @param locale the locale for localization
         * 
         * @return the title
         */
        @Nonnull
        String getTitle(Locale locale);

        /**
         * Get the item's value.
         * 
         * @return the value
         */
        @Nonnull
        String getValue();
    }
}
