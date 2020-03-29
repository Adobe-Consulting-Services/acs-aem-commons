package com.adobe.acs.commons.indesign.dynamicdeckdynamo.models;

import com.adobe.acs.commons.mcp.form.*;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.models.annotations.DefaultInjectionStrategy;
import org.apache.sling.models.annotations.Model;

import javax.inject.Inject;
import javax.inject.Named;

@Model(
        adaptables = {Resource.class, SlingHttpServletRequest.class},
        defaultInjectionStrategy = DefaultInjectionStrategy.OPTIONAL)
public class DeckDynamoInitiatorPageModel extends GeneratedDialog {

    @Inject
    @Named(value = "deckTitle")
    @FormField(
            name = "Deck Title",
            required = true,
            category = "General",
            description = "Enter a InDesign deck name. e.g. summer-2020",
            hint = "sample")
    private String deckTitle;

    @Inject
    @Named(value = "destinationPath")
    @FormField(
            name = "Deck Destination Folder Path",
            description = "Select the folder path where the InDesign deck will be generated.",
            required = true,
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someFolder",
            category = "General")
    private String destinationPath;

    @Inject
    @Named(value = "masterAssetPath")
    @FormField(
            name = "Master Asset Path",
            description = "Select the master asset. InDesign deck's header & footer info will be taken from master asset's metadata",
            component = PathfieldComponent.AssetSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someAsset",
            category = "General")
    private String masterAssetPath;

    @Inject
    @Named(value = "templatePath")
    @FormField(
            name = "Template Folder Path",
            description = "Select the InDesign template folder for generating the InDesign deck. Make sure that the folder must have a template file and the mapped XML file in the same folder.",
            required = true,
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someFolder",
            category = "General")
    private String templatePath;

    public enum Mode {
        COLLECTION, QUERY, TAGS
    }

    @Inject
    @Named(value = "operationMode")
    @FormField(
            name = "Operation Mode",
            description = "Select the operation mode to supply the assets to the deck dynamo.",
            required = false,
            component = RadioComponent.EnumerationSelector.class,
            options = {"horizontal", "default=COLLECTION"})
    private Mode operationMode;

    @Inject
    @Named(value = "collectionPath")
    @FormField(
            name = "Collection",
            description = "Select the collection from the drop-down below.",
            component = CollectionSelectComponent.class,
            category = "General")
    private String collectionPath;

    @Inject
    @Named(value = "assetQuery")
    @FormField(
            name = "Query to fetch assets",
            component = GenericListSelectComponent.class,
            category = "General",
            description = "Select the query from the drop-down below. These queries are listed from the 'Query List' page linked on top-right of this page.",
            options = {GenericListSelectComponent.GENERIC_LIST_PATH + "=/etc/acs-commons/lists/dynamic-deck-query-list"})
    private String assetQuery;

    @Inject
    @Named(value = "assetTag")
    @FormField(
            name = "Tags to fetch assets",
            component = TagPickerComponent.class,
            category = "General",
            description = "Select the tags from the system. It will fetch the assets associated with the respective tags.")
    private String assetTag;

    public DeckDynamoInitiatorPageModel() {
    }

    public String getDeckTitle() {
        return deckTitle;
    }

    public void setDeckTitle(String deckTitle) {
        this.deckTitle = deckTitle;
    }

    public String getTemplatePath() {
        return templatePath;
    }

    public void setTemplatePath(String templatePath) {
        this.templatePath = templatePath;
    }

    public String getCollectionPath() {
        return collectionPath;
    }

    public void setCollectionPath(String collectionPath) {
        this.collectionPath = collectionPath;
    }

    public String getMasterAssetPath() {
        return masterAssetPath;
    }

    public void setMasterAssetPath(String masterAssetPath) {
        this.masterAssetPath = masterAssetPath;
    }

    public String getDestinationPath() {
        return destinationPath;
    }

    public void setDestinationPath(String destinationPath) {
        this.destinationPath = destinationPath;
    }

    public Mode getOperationMode() {
        return operationMode;
    }

    public void setOperationMode(Mode operationMode) {
        this.operationMode = operationMode;
    }
}
