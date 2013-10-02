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

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

/**
 * A generic list of title/value pairs.
 */
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
         * Get the item's value.
         * 
         * @return the value
         */
        @Nonnull
        String getValue();
    }
}
