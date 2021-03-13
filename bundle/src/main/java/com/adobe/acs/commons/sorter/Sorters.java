package com.adobe.acs.commons.sorter;

import org.osgi.annotation.versioning.ProviderType;

import java.util.Collection;

@ProviderType
public interface Sorters {
    /**
     * This is intended to power the Sorter configuration UI.
     *
     * @return a collection of available sorters registered in AEM.
     */
    Collection<NodeSorter> getAvailableSorters();
}
