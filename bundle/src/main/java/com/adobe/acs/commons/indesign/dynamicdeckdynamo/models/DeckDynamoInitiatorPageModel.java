package com.adobe.acs.commons.indesign.dynamicdeckdynamo.models;

import com.adobe.acs.commons.mcp.form.*;
import com.fasterxml.jackson.annotation.JsonProperty;
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
    @JsonProperty(value = "deckTitle")
    @FormField(
            name = "Deck Title",
            required = true,
            category = "General",
            description = "Enter deck name",
            hint = "sample")
    private String deckTitle;

    @Inject
    @Named(value = "masterAssetPath")
    @JsonProperty(value = "masterAssetPath")
    @FormField(
            name = "Master Asset Path",
            description = "Select master asset path",
            component = PathfieldComponent.AssetSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someAsset",
            category = "General")
    private String masterAssetPath;

    @Inject
    @Named(value = "templatePath")
    @JsonProperty(value = "templatePath")
    @FormField(
            name = "Template Path",
            description = "Select Template path",
            required = true,
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someFolder",
            category = "General")
    private String templatePath;

    @Inject
    @Named(value = "destinationPath")
    @JsonProperty(value = "destinationPath")
    @FormField(
            name = "Destination Path",
            description = "Select Destination path",
            required = true,
            component = PathfieldComponent.FolderSelectComponent.class,
            options = {"base=/content/dam"},
            hint = "/content/dam/someFolder",
            category = "General")
    private String destinationPath;

    public enum Mode {
        COLLECTION, QUERY, TAGS
    }

    @Inject
    @Named(value = "operationMode")
    @JsonProperty(value = "operationMode")
    @FormField(
            name = "Operation Mode",
            description = "Select the operation mode to supply the assets",
            required = false,
            component = RadioComponent.EnumerationSelector.class,
            options = {"horizontal", "default=COLLECTION"})
    private Mode operationMode;

    @Inject
    @Named(value = "collectionPath")
    @JsonProperty(value = "collectionPath")
    @FormField(
            name = "Collection Path",
            description = "Select Collection Path",
            component = CollectionSelectComponent.class,
            category = "General")
    private String collectionPath;

    @Inject
    @Named(value = "assetQuery")
    @JsonProperty(value = "assetQuery")
    @FormField(
            name = "Query to fetch assets",
            component = GenericListSelectComponent.class,
            category = "General",
            description = "Select query",
            options = {GenericListSelectComponent.GENERIC_LIST_PATH + "=/etc/acs-commons/lists/dynamic-deck-query-list"})
    private String assetQuery;

    @Inject
    @Named(value = "assetTag")
    @JsonProperty(value = "assetTag")
    @FormField(
            name = "Tags to fetch assets",
            component = TagPickerComponent.class,
            category = "General",
            description = "Select Tags")
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
