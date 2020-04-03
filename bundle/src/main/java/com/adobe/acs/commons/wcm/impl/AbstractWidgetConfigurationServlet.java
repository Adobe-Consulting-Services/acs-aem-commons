/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.json.JsonObjectUtil;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

@SuppressWarnings("serial")
public abstract class AbstractWidgetConfigurationServlet extends SlingSafeMethodsServlet {

    protected boolean matches(String componentPath, Resource resource) {
        ValueMap map = resource.adaptTo(ValueMap.class);
        if (map == null) {
            return false;
        }
        String pattern = map.get("pattern", String.class);
        if (pattern == null) {
            return false;
        }
        return componentPath.matches(pattern);
    }

    protected void writeEmptyWidget(String propertyName, SlingHttpServletResponse response) throws IOException {
        Gson gson = new Gson();
        JsonObject rte = createEmptyWidget(propertyName);
        gson.toJson(rte, response.getWriter());
    }

    /**
     * Load the base configuration and "underlay" it under the provided
     * configuration so that the provided configuration overwrites the default
     * configuration.
     *
     * @param config the configuration to underlay
     * @param resource the resource to underlay
     * @return the underlayed configuration
     * @throws JSONException
     * @throws ServletException
     */
    protected final JsonObject underlay(JsonObject config, Resource resource)
            throws ServletException {
        JsonObject baseStructure = JsonObjectUtil.toJsonObject(resource);
        if (baseStructure != null) {
            config.entrySet().forEach(e -> baseStructure.add(e.getKey(), e.getValue()));
            return baseStructure;
        } else {
            return config;
        }
    }

    protected abstract JsonObject createEmptyWidget(String propertyName);

}
