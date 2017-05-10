/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.http;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.IOException;

import static com.day.cq.wcm.foundation.List.log;

/**
 * Converts response to a JSON Object.
 */
public class JsonObjectResponseHandler implements ResponseHandler<JSONObject> {

    private BasicResponseHandler handler = new BasicResponseHandler();

    @Override
    public JSONObject handleResponse(HttpResponse httpResponse) throws
            ClientProtocolException, IOException {
        String json = handler.handleResponse(httpResponse);
        if (json == null) {
            return null;
        }
        try {
            return new JSONObject(json);
        } catch (JSONException e) {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("error", e.getMessage());
            } catch (JSONException e1) {
                log.error("Could not form a JSON error response", e);
            }
            return jsonObject;
        }
    }
}
