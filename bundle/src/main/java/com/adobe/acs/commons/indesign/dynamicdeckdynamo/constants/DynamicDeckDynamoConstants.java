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

package com.adobe.acs.commons.indesign.dynamicdeckdynamo.constants;

/**
 * This class will be used to store global Constants
 */
public class DynamicDeckDynamoConstants {

    /**
     * Private Constructor will prevent the instantiation of this class directly
     */
    private DynamicDeckDynamoConstants() {

    }

    public static final String XML_MIME_TYPE = "text/xml";
    public static final String INDESIGN_MIME_TYPE = "application/x-indesign";
    public static final String FILE_PATH_PREFIX = "file:///";
    public static final String DECK_TYPE = "DYNAMIC DECK";

    // XML attributes and values constants 
    public static final String XML_ATTR_SECTION_TYPE = "section-type";
    public static final String XML_SECTION_TYPE_GENERIC = "generic";
    public static final String XML_SECTION_TYPE_ITERABLE = "iterable";
    public static final String XML_SECTION_TYPE_MASTER = "master";

    public static final String XML_ATTR_FIELD_TYPE = "field-type";
    public static final String XML_FIELD_TYPE_IMAGE = "image";
    public static final String XML_FIELD_TYPE_TEXT = "text";

    public static final String XML_ATTR_ASSETPATH = "asset-path";
    public static final String XML_ATTR_DATA_SYNC = "is-data-sync";
    public static final String XML_ATTR_IS_ARRAY = "is-array";

    public static final String XML_ATTR_PROPERTY_PATH = "property-path";
    public static final String XML_ATTR_ITERABLE_TYPE = "iterable-type";
    public static final String XML_ATTR_VAL_SELF = "_SELF_";

    //AEM properties and values
    public static final String PN_LIGHTBOX_COLLECTION = "Lightbox";
    public static final String PN_INDD_TEMPLATE_TYPE = "dam:templateType";


}
