package com.adobe.acs.commons.cloudservices.pwa;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Calendar;
import java.util.Set;

public interface ConfItem {
    /**
     * Returns the title of the item.
     *
     * @return Item title or resource name if none found. Returns never
     *         {@code null}
     */
    @Nonnull
    String getTitle();

    /**
     * Indicates if item has children.
     *
     * @return {@code true} if item has children
     */
    @Nonnull
    boolean hasChildren();

    /**
     * Returns the last modified time stamp.
     *
     * @return Last modified time in milliseconds or {@code null}
     */
    @Nullable
    Calendar getLastModifiedDate();

    /**
     * Returns the user which last modified the item
     *
     * @return User identifier or {@code null}
     */
    @Nullable
    String getLastModifiedBy();

    /**
     * Returns the last published time stamp.
     *
     * @return Last published time in milliseconds or {@code null}
     */
    @Nonnull
    Set<String> getQuickactionsRels();

    /**
     * @return the CoralUI CSS Class name for the Conf item's icon.
     */
    String getIcon();

    /**
     * @return true if this item should be displayed by the consuming Conf UI script or false if should be hidden.
     */
    boolean isValid();

}
