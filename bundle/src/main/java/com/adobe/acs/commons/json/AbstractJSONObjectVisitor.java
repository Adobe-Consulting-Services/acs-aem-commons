/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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
package com.adobe.acs.commons.json;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map.Entry;

public abstract class AbstractJSONObjectVisitor {
    private int currentDepth = 0;
    
    public int getCurrentDepth() {
        return currentDepth;
    }
    
    /**
     * Visit the given JSON Object and all its descendants.
     *
     * @param jsonObject The JSON Object
     */
    public void accept(final JsonObject jsonObject) {
        if (jsonObject != null) {
            this.visit(jsonObject);
            currentDepth++;
            this.traverseJSONObject(jsonObject);
            currentDepth--;
        }
    }

    /**
     * Visit the given JSON Array and all its descendants.
     *
     * @param jsonArray The JSON Object
     */
    public void accept(final JsonArray jsonArray) {
        if (jsonArray != null) {
            currentDepth++;
            this.traverseJSONArray(jsonArray);
            currentDepth--;
        }
    }

    /**
     * Visit each JSON Object in the JSON Array.
     *
     * @param jsonObject The JSON Array
     */
    protected final void traverseJSONObject(final JsonObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        for (Entry<String, JsonElement> elem : jsonObject.entrySet()) {
            if (elem.getValue().isJsonArray()) {
                accept(elem.getValue().getAsJsonArray());
            } else if (elem.getValue().isJsonObject()) {
                accept(elem.getValue().getAsJsonObject());
            }
        }
    }

    /**
     * Visit each JSON Object in the JSON Array.
     *
     * @param jsonArray The JSON Array
     */
    protected final void traverseJSONArray(final JsonArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (int i = 0; i < jsonArray.size(); i++) {
            if (jsonArray.get(i).isJsonObject()) {
                this.accept(jsonArray.get(i).getAsJsonObject());
            } else if (jsonArray.get(i).isJsonArray()) {
                this.accept(jsonArray.get(i).getAsJsonArray());
            }
        }
    }

    /**
     * Implement this method to do actual work on the JSON Object.
     *
     * @param jsonObject The JSON Object
     */
    protected abstract void visit(final JsonObject jsonObject);

}
