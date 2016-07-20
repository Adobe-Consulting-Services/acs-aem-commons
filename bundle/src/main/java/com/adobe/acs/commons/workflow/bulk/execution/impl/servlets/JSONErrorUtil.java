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

package com.adobe.acs.commons.workflow.bulk.execution.impl.servlets;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import java.io.IOException;

public final class JSONErrorUtil {

    private JSONErrorUtil() {
        // Util class
    }

    public static void sendJSONError(SlingHttpServletResponse response,
                                     int statusCode,
                                     String title,
                                     String message) throws IOException {

        response.setStatus(statusCode);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        JSONObject json = new JSONObject();
        try {
            json.put("title", title);
            json.put("message", message);
            response.getWriter().write(json.toString());
        } catch (JSONException e) {
            String jsonString = "{title: \"Error constructing error message\"}";
            response.getWriter().write(jsonString);
        }
    }
}
