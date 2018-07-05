package com.adobe.acs.commons.wcm.pwa.impl;

import java.util.Calendar;
import java.util.Set;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.osgi.annotation.versioning.ProviderType;

/**
 * Represents an FDM configuration item from {@code /conf}.
 * <p>
 * The configuration might be of type {@code sling:Folder} or cq:Page
 *
 * </p>
 */
@ProviderType
public interface PWAConfiguration {

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

}