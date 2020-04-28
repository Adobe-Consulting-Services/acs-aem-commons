package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import com.adobe.acs.commons.indesign.dynamicdeckdynamo.osgiconfigurations.DynamicDeckConfiguration;
import com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.DynamicDeckConfigurationService;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.Designate;

@Component(service = DynamicDeckConfigurationService.class, immediate = true)
@Designate(ocd = DynamicDeckConfiguration.class)
public class DynamicDeckConfigurationServiceImpl implements DynamicDeckConfigurationService {

    private DynamicDeckConfiguration config;


    @Activate
    protected void activate(DynamicDeckConfiguration config) {
        this.config = config;
    }


    public String getPlaceholderImagePath() {
        return config.placeholderImagePath();
    }

    public String getCollectionQuery() {
        return config.collectionQuery();
    }
}
