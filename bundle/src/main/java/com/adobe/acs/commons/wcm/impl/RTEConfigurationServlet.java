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
import java.util.Iterator;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.osgi.PropertiesUtil;

import com.adobe.acs.commons.util.PathInfoUtil;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.sling.xss.XSSAPI;

import static com.adobe.acs.commons.json.JsonObjectUtil.toJsonObject;

/**
 * Servlets which allows for dynamic selection of RTE configuration. To use in a
 * component, specify the xtype of
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
 * . This will iterate through nodes under /etc/rteconfig to find a matching
 * site (by regex). Then, look for a node named CONFIGNAME and use that
 * configuration.
 */
@SuppressWarnings({"serial", "checkstyle:abbreviationaswordinname"})
@SlingServlet(extensions = "json", selectors = "rte", resourceTypes = "sling/servlet/default")
public final class RTEConfigurationServlet extends AbstractWidgetConfigurationServlet {

    private static final int RTE_HEIGHT = 200;

    private static final int RTE_WIDTH = 430;

    private static final String DEFAULT_CONFIG_NAME = "default";

    @Reference
    private XSSAPI xssApi;

    private static final String DEFAULT_CONFIG
            = "/libs/foundation/components/text/dialog/items/tab1/items/text/rtePlugins";

    private static final String DEFAULT_ROOT_PATH = "/etc/rteconfig";

    @Property(value = DEFAULT_ROOT_PATH)
    private static final String PROP_ROOT_PATH = "root.path";

    private static final String EXTERNAL_STYLESHEETS_PROPERTY = "externalStyleSheets";

    private String rootPath;

    @Override
    protected JsonObject createEmptyWidget(String rteName) {
        JsonObject object = new JsonObject();
        object.addProperty("xtype", "richtext");
        object.addProperty("name", "./" + xssApi.encodeForJSString(rteName));
        object.addProperty("hideLabel", true);
        object.addProperty("jcr:primaryType", "cq:Widget");
        return object;
    }

    private void returnDefault(String rteName, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        Resource root = request.getResourceResolver().getResource(DEFAULT_CONFIG);
        if (root == null) {
            writeEmptyWidget(rteName, response);
            return;
        }

        writeConfigResource(root, rteName, request, response);
    }

    private void writeConfigResource(Resource resource, String rteName,
            SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        JsonObject widget = createEmptyWidget(rteName);

        // these two size properties seem to be necessary to get the size correct
        // in a component dialog
        widget.addProperty("width", RTE_WIDTH);
        widget.addProperty("height", RTE_HEIGHT);

        RequestParameterMap map = request.getRequestParameterMap();
        for (Map.Entry<String, RequestParameter[]> entry : map.entrySet()) {
            String key = entry.getKey();
            RequestParameter[] params = entry.getValue();
            if (params != null && (params.length > 1 || EXTERNAL_STYLESHEETS_PROPERTY.equals(key))) {
                JsonArray arr = new JsonArray();
                for (int i = 0; i < params.length; i++) {
                    arr.add(new JsonPrimitive(params[i].getString()));
                }
                widget.add(key, arr);
            } else if (params != null && params.length == 1) {
                widget.addProperty(key, params[0].getString());
            }
        }

        if (widget.has("fieldLabel")) {
            widget.remove("hideLabel");
        }

        JsonObject config = toJsonObject(resource);

        if (config == null) {
            config = new JsonObject();
        }

        if (config.has("includeDefault") && config.get("includeDefault").getAsBoolean()) {
            config = underlay(config, resource.getResourceResolver().getResource(DEFAULT_CONFIG));
        }

        widget.add("rtePlugins", config);

        JsonObject parent = new JsonObject();
        parent.addProperty("xtype", "dialogfieldset");
        parent.addProperty("border", false);
        parent.addProperty("padding", 0);
        parent.add("items", widget);
        Gson gson = new Gson();
        gson.toJson(parent, response.getWriter());
    }

    @Activate
    protected void activate(Map<String, Object> props) {
        rootPath = PropertiesUtil.toString(props.get(PROP_ROOT_PATH), DEFAULT_ROOT_PATH);
    }

    @Override
    @SuppressWarnings("squid:S3776")
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
                    Resource config = child.getChild(configName);
                    if (config == null) {
                        config = child.getChild(DEFAULT_CONFIG_NAME);
                    }
                    if (config != null) {
                        writeConfigResource(config, rteName, request, response);
                        return;
                    }
                }
            }
        }

        returnDefault(rteName, request, response);

    }

}
