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
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public abstract class AbstractResultSerializer {
    private static final Logger log = LoggerFactory.getLogger(AbstractResultSerializer.class);

    public JSONObject toJSON(final Result result, final ValueMap config) throws JSONException {

        final JSONObject json = new JSONObject();
        final JSONObject action = new JSONObject();
        final JSONObject actionParams = new JSONObject();

        json.put("title", result.getTitle());
        json.put("type", result.getResultType());
        json.put("description", result.getDescription());
        json.put("path", result.getPath());
        json.put("autoComplete", result.getPath());

        // Action
        action.put("uri", result.getActionURI());
        action.put("method", result.getActionMethod().toString());
        action.put("target", result.getActionTarget().toString());
        action.put("xhr", false);
        action.put("script", result.getActionScript());

        for (final Map.Entry<String, String> param : result.getActionParams().entrySet()) {
            actionParams.put(param.getKey(), param.getValue());
        }

        action.put("params", actionParams);

        json.put("action", action);

        return json;
    }
}
