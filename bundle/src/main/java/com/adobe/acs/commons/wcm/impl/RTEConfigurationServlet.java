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
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.json.jcr.JsonItemWriter;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.adobe.acs.commons.util.PathInfoUtil;
import com.adobe.granite.xss.XSSAPI;

/**
 * Servlets which allows for dynamic selection of RTE configuration. To use in a component, specify the xtype of
 * 
 * <pre>
 * slingscriptinclude
 * </pre>
 * 
 * and set the script to
 * 
 * <pre>
 * rte.CONFIGNAME.FIELDNAME.json.jsp
 * </pre>
 * 
 * . This will iterate through nodes under /etc/rteconfig to find a matching site (by regex). Then, look for a node
 * named CONFIGNAME and use that configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(extensions = "json", selectors = "rte", resourceTypes = "sling/servlet/default")
public final class RTEConfigurationServlet extends SlingSafeMethodsServlet {

    private static final int RTE_HEIGHT = 200;

    private static final int RTE_WIDTH = 430;

    private static final String DEFAULT_CONFIG_NAME = "default";

    @Reference
    private XSSAPI xssApi;

    private static final String DEFAULT_CONFIG =
            "/libs/foundation/components/text/dialog/items/tab1/items/text/rtePlugins";

    private static final String DEFAULT_ROOT_PATH = "/etc/rteconfig";

    @Property(value = DEFAULT_ROOT_PATH)
    private static final String PROP_ROOT_PATH = "root.path";

    private String rootPath;

    private JSONObject createEmptyRTE(String rteName) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("xtype", "richtext");
        object.put("name", "./" + xssApi.encodeForJSString(rteName));
        object.put("hideLabel", true);
        object.put("jcr:primaryType", "cq:Widget");
        return object;
    }

    private boolean matches(String componentPath, Resource resource) {
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

    private void returnDefault(String rteName, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        try {
            Resource root = request.getResourceResolver().getResource(DEFAULT_CONFIG);
            if (root == null) {
                writeEmptyRTE(rteName, response);
                return;
            }

            writeConfigResource(root, rteName, true, response);
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    private JSONObject toJSONObject(Resource resource) throws JSONException, ServletException {
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

    /**
     * Load the default configuration and "underlay" it under the provided
     * configuration so that the provided configuration overwrites the default
     * configuration.
     * 
     * @param resolver the resource resolver
     * @param config the config to underlay
     * @return the underlayed configuration
     * @throws JSONException
     * @throws ServletException
     */
    private JSONObject underlayDefault(ResourceResolver resolver, JSONObject config) throws JSONException,
            ServletException {
        JSONObject defaultStructure = toJSONObject(resolver.getResource(DEFAULT_CONFIG));
        if (defaultStructure != null) {
            Iterator<String> keys = config.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                defaultStructure.put(key, config.get(key));
            }
            return defaultStructure;
        } else {
            return config;
        }
    }

    private void writeConfigResource(Resource resource, String rteName, boolean isDefault,
            SlingHttpServletResponse response) throws IOException, JSONException, ServletException {
        JSONObject widget = createEmptyRTE(rteName);

        // these two size properties seem to be necessary to get the size correct
        // in a component dialog
        widget.put("width", RTE_WIDTH);
        widget.put("height", RTE_HEIGHT);

        JSONObject config = toJSONObject(resource);

        if (config == null) {
            config = new JSONObject();
        }

        if (config.optBoolean("includeDefault")) {
            config = underlayDefault(resource.getResourceResolver(), config);
        }

        widget.put("rtePlugins", config);

        JSONObject parent = new JSONObject();
        parent.put("xtype", "dialogfieldset");
        parent.put("border", false);
        parent.put("padding", 0);
        parent.accumulate("items", widget);
        parent.write(response.getWriter());
    }

    private void writeEmptyRTE(String rteName, SlingHttpServletResponse response) throws IOException,
            JSONException {
        JSONObject rte = createEmptyRTE(rteName);
        rte.write(response.getWriter());
    }

    @Activate
    protected void activate(Map<String, Object> props) {
        rootPath = PropertiesUtil.toString(props.get(PROP_ROOT_PATH), DEFAULT_ROOT_PATH);
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        String componentPath = request.getResource().getPath();

        String configName = PathInfoUtil.getSelector(request, 1, DEFAULT_CONFIG_NAME);

        // the actual property name
        String rteName = PathInfoUtil.getSelector(request, 2, "text");

        Resource root = request.getResourceResolver().getResource(rootPath);
        if (root != null) {
            Iterator<Resource> children = root.listChildren();
            while (children.hasNext()) {
                Resource child = children.next();
                if (matches(componentPath, child)) {
                    boolean isDefault = false;
                    Resource config = child.getChild(configName);
                    if (config == null) {
                        config = child.getChild(DEFAULT_CONFIG_NAME);
                        isDefault = true;
                    }
                    if (config != null) {
                        try {
                            writeConfigResource(config, rteName, isDefault, response);
                        } catch (JSONException e) {
                            throw new ServletException(e);
                        }

                        return;
                    }
                }
            }
        }

        returnDefault(rteName, request, response);

    }

}
