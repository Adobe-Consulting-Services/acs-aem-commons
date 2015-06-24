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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

public abstract class AbstractResultSerializer {

    public JSONObject toJSON(final Result result, final ValueMap config) throws JSONException {
        final JSONObject json = new JSONObject();

        json.put("title", result.getTitle());
        json.put("type", result.getResultType());
        json.put("description", result.getDescription());
        json.put("path", result.getPath());

        json.put("action", this.toJSON(result.getAction()));
        json.put("secondaryAction", this.toJSON(result.getSecondaryAction()));

        return json;
    }

    public JSONObject toJSON(final Action action) throws JSONException {

        final JSONObject json = new JSONObject();

        if (action != null) {
            json.put("uri", action.getUri());
            json.put("method", action.getMethod());
            json.put("target", action.getTarget());
            json.put("xhr", false);
            json.put("script", action.getScript());
            json.put("params", new JSONObject(action.getParams()));
        }

        return json;
    }
}
