package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services;

import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface DynamicDeckConfigurationService {

    String getPlaceholderImagePath();

    String getCollectionQuery();

}
