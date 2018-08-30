package com.adobe.acs.commons.cloudservices.pwa;

import com.day.cq.wcm.api.Page;
import org.apache.sling.api.resource.ValueMap;
import org.osgi.annotation.versioning.ProviderType;

@ProviderType
public interface Configuration {

    boolean isReady();

    Page getConfPage();

    ValueMap getProperties();

    String getScopePath();

    String[] getServiceWorkerJsCategories();

    String[] getPwaJsCategories();

    Page getRootPage();
}