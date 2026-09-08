/*-
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2022 Adobe
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
package com.adobe.acs.commons.contentsync;


import javax.json.JsonArray;
import javax.json.JsonObject;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;
import static org.apache.jackrabbit.JcrConstants.JCR_MIXINTYPES;

/**
 * Represents a JSON object describing a resource to be synchronized in ACS Commons Content Sync.
 * <p>
 * Required fields in the JSON object:
 * <ul>
 *   <li><b>path</b>: the JCR path of the resource</li>
 *   <li><b>jcr:primaryType</b>: the primary type of the resource</li>
 *   <li><b>exportUri</b>: the URI to export the resource's content</li>
 * </ul>
 * Optional fields include <b>renderServlet</b> for custom exporters and <b>jcr:mixinTypes</b> for mixins.
 */
public class CatalogItem {
    /**
     * The underlying JSON object describing the resource.
     */
    private final JsonObject object;

    /**
     * Optional message describing sync status or details.
     */
    private String message;

    /**
     * Flag indicating whether the resource was updated during sync.
     */
    private boolean updated;

    /**
     * Constructs a CatalogItem from a JSON object.
     *
     * @param object the JSON object describing the resource
     */
    public CatalogItem(JsonObject object){
        this.object = object;
    }

    /**
     * Returns the JCR path of the resource.
     *
     * @return the resource path
     */
    public String getPath(){
        return object.getString("path");
    }

    /**
     * Returns the JCR primary type of the resource.
     *
     * @return the primary type
     */
    public String getPrimaryType(){
        return object.getString("jcr:primaryType");
    }

    /**
     * Returns true if the export URI points to a content resource (ends with "/jcr:content.infinity.json").
     *
     * @return true if the resource has a content resource, false otherwise
     */
    public boolean hasContentResource(){
        return getContentUri().endsWith("/" + JCR_CONTENT + ".infinity.json");
    }

    /**
     * Returns the export URI for the resource's content.
     *
     * @return the export URI
     */
    public String getContentUri(){
        return object.getString("exportUri");
    }

    /**
     * Returns the value of a string property from the JSON object, or null if not present.
     *
     * @param key the property key
     * @return the string value, or null
     */
    public String getString(String key){
        return object.containsKey(key) ? object.getString(key) : null;
    }

    /**
     * Returns the value of a long property from the JSON object, or 0 if not present.
     *
     * @param key the property key
     * @return the long value, or 0
     */
    public long getLong(String key){
        return object.containsKey(key) ? object.getJsonNumber(key).longValue() : 0L;
    }

    /**
     * Returns the custom exporter servlet name, if present.
     *
     * @return the custom exporter servlet name, or null
     */
    public String getCustomExporter(){
        return object.containsKey("renderServlet") ? object.getString("renderServlet") : null;
    }

    /**
     * Returns the underlying JSON object.
     *
     * @return the JSON object
     */
    public JsonObject getJsonObject(){
        return object;
    }

    /**
     * Returns the mixin types for the resource as a string array.
     *
     * @return the mixin types, or an empty array if none
     */
    public String[] getMixins(){
        JsonArray mixins = object.getJsonArray(JCR_MIXINTYPES);
        if(mixins == null){
            return new String[0];
        }
        String[] mixinArray = new String[mixins.size()];
        for(int i = 0; i < mixins.size(); i++){
            mixinArray[i] = mixins.getString(i);
        }
        return mixinArray;
    }

    /**
     * Returns the sync message for this item.
     *
     * @return the message
     */
    public String getMessage(){
        return message;
    }

    /**
     * Sets the sync message for this item.
     *
     * @param message the message to set
     */
    public void setMessage(String message){
        this.message = message;
    }

    /**
     * Returns true if the resource was updated during sync.
     *
     * @return true if updated, false otherwise
     */
    public boolean isUpdated(){
        return updated;
    }

    /**
     * Sets the updated flag for this item.
     *
     * @param flag true if updated, false otherwise
     */
    public void setUpdated(boolean flag){
        updated = flag;
    }
}
