package com.adobe.acs.commons.cloudservices;

import org.osgi.annotation.versioning.ProviderType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collection;

@ProviderType
public interface Meta {

    @Nonnull
    boolean isFolder();

    @Nullable
    String getTitle();

    @Nonnull
    Collection<String> getActionsRels();
}