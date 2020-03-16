package com.adobe.acs.commons.indesign.deckdynamo.osgiconfigurations;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.metatype.annotations.Designate;

@Component(immediate = true, service = DeckDynamoConfigurationService.class)
@Designate(ocd = DeckDynamoConfiguration.class)
public class DeckDynamoConfigurationService {

    private String templateRootPath;
    private String placeholderImagePath;
    private String collectionQuery;


    @Activate
    protected void activate(DeckDynamoConfiguration config) {
        templateRootPath = config.templateRootPath();
        placeholderImagePath = config.placeholderImagePath();
        collectionQuery = config.collectionQuery();
    }

    @Modified
    protected void modified(DeckDynamoConfiguration config) {
        templateRootPath = config.templateRootPath();
        placeholderImagePath = config.placeholderImagePath();
        collectionQuery = config.collectionQuery();
    }


    public String getTemplateRootPath() {
        return templateRootPath;
    }

    public String getPlaceholderImagePath() {
        return placeholderImagePath;
    }

    public String getCollectionQuery() {
        return collectionQuery;
    }
}
