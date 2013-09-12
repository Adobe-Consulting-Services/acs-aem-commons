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
