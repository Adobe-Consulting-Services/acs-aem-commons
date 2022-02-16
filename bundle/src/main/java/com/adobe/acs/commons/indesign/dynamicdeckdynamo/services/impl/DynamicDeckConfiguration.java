/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2020 Adobe
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.services.impl;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.AttributeType;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

/**
 * These are some important configurations for generation InDesign deck dynamically.
 */
@ObjectClassDefinition(
        name = "Dynamic Deck Dynamo Configurations",
        description = "These are some important configurations for generation InDesign deck dynamically."
)
public @interface DynamicDeckConfiguration {
    @AttributeDefinition(name = "Placeholder Image Path", description = "This placeholder image is used if deck image is not available", type = AttributeType.STRING)
    public String placeholderImagePath() default "/content/dam/dynamic-deck-dynamo/placeholder-images/APPAREL_PLACEHOLDER_TRANSPARENT.png";

    @AttributeDefinition(name = "Collection Query", description = "This query is used to fetch the collection and smart collection", type = AttributeType.STRING)
    public String collectionQuery() default "SELECT s.* from [nt:base] AS s WHERE ISDESCENDANTNODE(s, '/content/dam/collections') AND s.[sling:resourceType] ='dam/collection' OR s.[sling:resourceType] = 'dam/smartcollection'";
}
