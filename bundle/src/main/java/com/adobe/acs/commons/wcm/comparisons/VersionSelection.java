package com.adobe.acs.commons.wcm.comparisons;

import aQute.bnd.annotation.ProviderType;

import java.util.Date;

@ProviderType
public interface VersionSelection {
    /**
     * @return the Date of the Version Selection
     */
    Date getDate();

    /**
     * @return the Name of the Version Selection
     */
    String getName();
}
