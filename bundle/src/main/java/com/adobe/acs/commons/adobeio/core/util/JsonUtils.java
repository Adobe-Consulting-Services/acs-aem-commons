/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2018 Adobe
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
package com.adobe.acs.commons.adobeio.core.util;

import org.apache.commons.lang3.StringUtils;

import com.drew.lang.annotations.NotNull;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Class containing static methods to handle Json-objects and arrays
 */
public final class JsonUtils {

    private JsonUtils() {
    }

    /**
     * This method returns the property of a jsonObject as a JsonObject
     * @param jsonObject
     * @param key
     * @return
     */
    public static JsonObject getPropertyAsObject(@NotNull final JsonObject jsonObject, @NotNull final String key) {
        if (jsonObject == null) {
            return new JsonObject();
        }
        if (StringUtils.isBlank(key)) {
            return jsonObject;
        }

        JsonObject result = jsonObject.getAsJsonObject(key);
        if (result == null) {
            return new JsonObject();
        }
        return result;
    }

    public static <T> JsonObject parseToJson(T data) {
        Gson gson = new Gson();
        JsonParser parser = new JsonParser();

        return parser.parse(gson.toJson(data)).getAsJsonObject();
    }


    /**
     * This method returns the property of a jsonObject as a JsonObject
     * @param jsonObject
     * @param key
     * @return
     */
    public static JsonArray getPropertyAsArray(@NotNull final JsonObject jsonObject, @NotNull final String key) {
        if ((jsonObject == null) || StringUtils.isBlank(key)) {
            return new JsonArray();
        }

        JsonArray result = jsonObject.getAsJsonArray(key);
        if (result == null) {
            return new JsonArray();
        }
        return result;
    }


    /**
     * This method creates an JsonObject for the provided data
     * @param data the provided data
     * @return A JsonObject of the provided data
     */
    public static JsonObject getJsonObject(@NotNull final String data) {
        if (StringUtils.isNotBlank(data)) {
            JsonParser parser = new JsonParser();
            return parser.parse(data).getAsJsonObject();
        }
        return new JsonObject();
    }
    
    
    /**
     * @param key Key
     * @param value Value
     * @return A JsonObject created from key and value
     */
    public static JsonObject getJsonObject(@NotNull final String key, @NotNull final String value) {
        JsonObject object = new JsonObject();
        object.addProperty(key, value);
        return object;
    }
    

    /**
     * This method returns the property of a jsonObject as a String
     * @param jsonObject
     * @param key
     * @return
     */
    public static String getProperty(@NotNull final JsonObject jsonObject, @NotNull final String key) {
        String result = "";
        if ((jsonObject != null) && jsonObject.has(key)) {
            if (jsonObject.get(key).isJsonObject()) {
                result = jsonObject.get(key).getAsJsonObject().toString();
            } else {
                result = jsonObject.get(key).getAsString();
            }
        }
        return result;
    }

}
