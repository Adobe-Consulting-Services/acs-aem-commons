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


import javax.json.JsonObject;

import static org.apache.jackrabbit.JcrConstants.JCR_CONTENT;

/**
 * A Json object describing a resource to sync.
 *
 * Required fields:
 *  - path
 *  - jcr:primaryType
 *  - exportUri
 */
public class CatalogItem {
    private final JsonObject object;

    public CatalogItem(JsonObject object){
        this.object = object;
    }

    public String getPath(){
        return object.getString("path");
    }

    public String getPrimaryType(){
        return object.getString("jcr:primaryType");
    }

    public boolean hasContentResource(){
        return getContentUri().endsWith("/" + JCR_CONTENT + ".infinity.json");
    }

    public String getContentUri(){
        return object.getString("exportUri");
    }

    public String getString(String key){
        return object.containsKey(key) ? object.getString(key) : null;
    }

    public long getLong(String key){
        return object.containsKey(key) ? object.getJsonNumber(key).longValue() : 0L;
    }

    public String getCustomExporter(){
        return object.containsKey("renderServlet") ? object.getString("renderServlet") : null;

    }

    public JsonObject getJsonObject(){
        return object;
    }
}
