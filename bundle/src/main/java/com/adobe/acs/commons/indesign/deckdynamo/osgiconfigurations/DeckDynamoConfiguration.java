package com.adobe.acs.commons.indesign.deckdynamo.osgiconfigurations;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * These are some important configurations for generation InDesign deck dynamically.
 */
@ObjectClassDefinition(
        name = "Deck Dynamo Configurations",
        description = "These are some important configurations for generation InDesign deck dynamically."
)
public @interface DeckDynamoConfiguration {

    @AttributeDefinition(name = "Template Root Path", description = "This path is the root path of InDesign templates", type = AttributeType.STRING)
    public String templateRootPath() default "/content/dam/dynamic-deck-dynamo/templates";

    @AttributeDefinition(name = "Placeholder Image Path", description = "This placeholder image is used if deck image is not available", type = AttributeType.STRING)
    public String placeholderImagePath() default "/content/dam/dynamic-deck-dynamo/placeholder-images/APPAREL_PLACEHOLDER_TRANSPARENT.png";

    @AttributeDefinition(name = "Collection Query", description = "This query is used to fetch the collection and smart collection", type = AttributeType.STRING)
    public String collectionQuery() default "SELECT s.* from [nt:base] AS s WHERE ISDESCENDANTNODE(s, '/content/dam/collections') AND s.[sling:resourceType] ='dam/collection' OR s.[sling:resourceType] = 'dam/smartcollection'";


}
