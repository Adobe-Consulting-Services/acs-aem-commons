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

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.jcr.JsonItemWriter;

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

    protected JSONObject toJSONObject(Resource resource) throws JSONException, ServletException {
        JSONObject config = null;
        Node node = resource.adaptTo(Node.class);
        if (node != null) {

            JsonItemWriter writer = new JsonItemWriter(null);
            StringWriter string = new StringWriter();
            try {
                writer.dump(node, string, -1);
            } catch (RepositoryException e) {
                throw new ServletException(e);
            }
            config = new JSONObject(string.toString());

        }
        return config;
    }

    protected void writeEmptyWidget(String propertyName, SlingHttpServletResponse response) throws IOException,
            JSONException {
        JSONObject rte = createEmptyWidget(propertyName);
        rte.write(response.getWriter());
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
    protected final JSONObject underlay(JSONObject config, Resource resource)
            throws JSONException, ServletException {
        JSONObject baseStructure = toJSONObject(resource);
        if (baseStructure != null) {
            Iterator<String> keys = config.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                baseStructure.put(key, config.get(key));
            }
            return baseStructure;
        } else {
            return config;
        }
    }

    protected abstract JSONObject createEmptyWidget(String propertyName) throws JSONException;

}
