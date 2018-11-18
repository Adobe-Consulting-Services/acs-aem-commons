/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2018 Adobe
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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.Iterator;
import java.util.Map.Entry;

import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.jcr.Node;
import org.apache.sling.api.resource.Resource;

/**
 * Provide convenience methods to help use GSON JsonObjects similar to the
 * deprecated JSONObject in Sling Json.
 */
public class JsonObjectUtil {

    private JsonObjectUtil() {
        // private constructor for a utility class
    }

    public static JsonObject toJsonObject(String json) {
        JsonParser parse = new JsonParser();
        return parse.parse(json).getAsJsonObject();
    }

    public static JsonObject toJsonObject(Resource resource) {
        return toJsonObject(resource.adaptTo(Node.class));
    }

    public static JsonObject toJsonObject(Object source) {
        if (source instanceof String) {
            return toJsonObject((String) source);
        } else if (source instanceof Resource) {
            return toJsonObject((Resource) source);
        } else {
            Gson gson = new GsonBuilder().registerTypeHierarchyAdapter(Node.class, new JcrJsonAdapter()).create();
            return gson.toJsonTree(source).getAsJsonObject();
        }
    }
    
    public static <T> JsonObject toJsonObject(T source, int depth) {
        JsonObject obj = toJsonObject(source);
        pruneToDepth(obj, depth);
        return obj;
    }    
    
    public static void pruneToDepth(JsonObject obj, int depth) {
        AbstractJSONObjectVisitor prune = new AbstractJSONObjectVisitor() {
            @Override
            protected void visit(JsonObject jsonObject) {
                if (getCurrentDepth() >= depth) {
                    Set<String> allKeys = jsonObject.entrySet().stream().map(Entry::getKey).collect(Collectors.toSet());
                    allKeys.forEach(jsonObject::remove);
                }
            }
        };
        prune.accept(obj);        
    }
    
    public static <T> String getAsJsonString(T source, int depth) {
        Gson gson = new Gson();
        JsonObject object = toJsonObject(source, depth);
        return gson.toJson(object);
    }
        
    public static boolean isSingularElement(JsonElement elem) {
        return elem.isJsonPrimitive() || (elem.isJsonArray() && elem.getAsJsonArray().size() <= 1);
    }

    public static Optional<JsonElement> getOptional(JsonObject obj, String prop) {
        return Optional.ofNullable(obj.get(prop));
    }

    public static <T> T getOptionalProperty(JsonObject obj, String prop, Function<JsonElement, T> getter, T defaultValue) {
        return getOptional(obj, prop)
                .filter(JsonObjectUtil::isSingularElement)
                .map(getter)
                .orElse(defaultValue);
    }
    
    public static Optional<JsonObject> getOptionalObject(JsonObject obj, String prop) {
        return Optional.ofNullable(obj.getAsJsonObject(prop));
    }    

    public static String getString(JsonObject obj, String prop, String defaultValue) {
        return getOptionalProperty(obj, prop, JsonElement::getAsString, defaultValue);
    }

    public static String getString(JsonObject obj, String prop) {
        return getOptionalProperty(obj, prop, JsonElement::getAsString, "");
    }

    public static Long getLong(JsonObject obj, String prop, Long defaultValue) {
        return getOptionalProperty(obj, prop, JsonElement::getAsLong, defaultValue);
    }

    public static Long getLong(JsonObject obj, String prop) {
        return getOptionalProperty(obj, prop, JsonElement::getAsLong, null);
    }

    public static Integer getInteger(JsonObject obj, String prop, Integer defaultValue) {
        return getOptionalProperty(obj, prop, JsonElement::getAsInt, defaultValue);
    }

    public static Integer getInteger(JsonObject obj, String prop) {
        return getOptionalProperty(obj, prop, JsonElement::getAsInt, null);
    }

    public static Boolean getBoolean(JsonObject obj, String prop, Boolean defaultValue) {
        return getOptionalProperty(obj, prop, JsonElement::getAsBoolean, defaultValue);
    }

    public static Boolean getBoolean(JsonObject obj, String prop) {
        return getOptionalProperty(obj, prop, JsonElement::getAsBoolean, null);        
    }    
}
