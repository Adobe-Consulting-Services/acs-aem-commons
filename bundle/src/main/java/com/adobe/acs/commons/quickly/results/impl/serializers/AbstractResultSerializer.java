/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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

package com.adobe.acs.commons.quickly.results.impl.serializers;

import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.Action;
import com.google.gson.Gson;
import com.google.gson.JsonObject;

public abstract class AbstractResultSerializer {

    public JsonObject toJSON(final Result result) {
        final JsonObject json = new JsonObject();

        json.addProperty("title", result.getTitle());
        json.addProperty("type", result.getResultType());
        json.addProperty("description", result.getDescription());
        json.addProperty("path", result.getPath());

        json.add("action", this.toJSON(result.getAction()));
        json.add("secondaryAction", this.toJSON(result.getSecondaryAction()));

        return json;
    }

    public JsonObject toJSON(final Action action) {

        final JsonObject json = new JsonObject();

        if (action != null) {
            Gson gson = new Gson();
            json.addProperty("uri", action.getUri());
            json.addProperty("method", action.getMethod().name());
            json.addProperty("target", action.getTarget().name());
            json.addProperty("xhr", false);
            json.addProperty("script", action.getScript());
            json.add("params", gson.toJsonTree(action.getParams()));
        }

        return json;
    }
}
