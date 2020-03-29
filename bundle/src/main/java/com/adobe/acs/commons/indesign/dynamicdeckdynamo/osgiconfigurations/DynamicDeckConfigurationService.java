package com.adobe.acs.commons.indesign.dynamicdeckdynamo.osgiconfigurations;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(immediate = true, service = DynamicDeckConfigurationService.class)
@Designate(ocd = DynamicDeckConfiguration.class)
public class DynamicDeckConfigurationService {

    private String placeholderImagePath;
    private String collectionQuery;


    @Activate
    protected void activate(DynamicDeckConfiguration config) {
        placeholderImagePath = config.placeholderImagePath();
        collectionQuery = config.collectionQuery();
    }

    @Modified
    protected void modified(DynamicDeckConfiguration config) {
        placeholderImagePath = config.placeholderImagePath();
        collectionQuery = config.collectionQuery();
    }


    public String getPlaceholderImagePath() {
        return placeholderImagePath;
    }

    public String getCollectionQuery() {
        return collectionQuery;
    }
}
