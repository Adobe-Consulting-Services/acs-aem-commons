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

import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;

public abstract class AbstractJSONObjectVisitor {
    private static final Logger log = LoggerFactory.getLogger(AbstractJSONObjectVisitor.class);

    /**
     * Visit the given JSON Object and all its descendants.
     *
     * @param jsonObject The JSON Object
     */
    public void accept(final JSONObject jsonObject) {
        if (jsonObject != null) {
            this.visit(jsonObject);
            this.traverseJSONObject(jsonObject);
        }
    }

    /**
     * Visit the given JSON Array and all its descendants.
     *
     * @param jsonArray The JSON Object
     */
    public void accept(final JSONArray jsonArray) {
        if (jsonArray != null) {
            this.traverseJSONArray(jsonArray);
        }
    }

    /**
     * Visit each JSON Object in the JSON Array.
     *
     * @param jsonObject The JSON Array
     */
    protected final void traverseJSONObject(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return;
        }

        final Iterator<String> keys = jsonObject.keys();

        while (keys.hasNext()) {
            final String key = keys.next();

            if (jsonObject.optJSONObject(key) != null) {
                this.accept(jsonObject.optJSONObject(key));
            } else if (jsonObject.optJSONArray(key) != null) {
                this.accept(jsonObject.optJSONArray(key));
            }
        }
    }

    /**
     * Visit each JSON Object in the JSON Array.
     *
     * @param jsonArray The JSON Array
     */
    protected final void traverseJSONArray(final JSONArray jsonArray) {
        if (jsonArray == null) {
            return;
        }

        for (int i = 0; i < jsonArray.length(); i++) {

            if (jsonArray.optJSONObject(i) != null) {
                this.accept(jsonArray.optJSONObject(i));
            } else if (jsonArray.optJSONArray(i) != null) {
                this.accept(jsonArray.optJSONArray(i));
            }
        }
    }

    /**
     * Implement this method to do actual work on the JSON Object.
     *
     * @param jsonObject The JSON Object
     */
    protected abstract void visit(final JSONObject jsonObject);

}
