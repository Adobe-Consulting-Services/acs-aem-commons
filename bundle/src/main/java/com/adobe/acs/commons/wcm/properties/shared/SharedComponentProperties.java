package com.adobe.acs.commons.wcm.properties.shared;

import aQute.bnd.annotation.ProviderType;

@ProviderType
public interface SharedComponentProperties {
    String SHARED_PROPERTIES = "sharedProperties";
    String GLOBAL_PROPERTIES = "globalProperties";
    String MERGED_PROPERTIES = "mergedProperties";

    String NN_GLOBAL_COMPONENT_PROPERTIES = "global-component-properties";
    String NN_SHARED_COMPONENT_PROPERTIES = "shared-component-properties";
}
