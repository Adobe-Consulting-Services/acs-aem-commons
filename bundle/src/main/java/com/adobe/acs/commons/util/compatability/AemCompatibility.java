package com.adobe.acs.commons.util.compatability;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface AemCompatibility {

    boolean isCloudService();

    boolean isQuickStart();
}
