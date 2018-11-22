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

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestParameter;
import org.apache.sling.api.request.RequestParameterMap;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.xss.XSSAPI;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.acs.commons.util.PathInfoUtil;

/**
 * Servlet which allows for dynamic selection of tag widget configuration.
 * To use in a component, specify the xtype of
 * 
 * <pre>
 * slingscriptinclude
 * </pre>
 * 
 * and set the script to
 * 
 * <pre>
 * tagwidget.CONFIGNAME.FIELDNAME.json.jsp
 * </pre>
 * 
 * This will iterate through nodes under /etc/tagconfig to find a matching site (by regex). Then, look for a node
 * named CONFIGNAME and use that configuration.
 */
@SuppressWarnings("serial")
@Component(service = Servlet.class, property = { SLING_SERVLET_EXTENSIONS + "=json", SLING_SERVLET_SELECTORS + "=tagwidget",
		SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default" })
@Designate(ocd=TagWidgetConfigurationServlet.Config.class)
public class TagWidgetConfigurationServlet extends AbstractWidgetConfigurationServlet {

    private static final String DEFAULT_CONFIG_NAME = "default";

    @Reference
    private XSSAPI xssApi;
    
	@ObjectClassDefinition
	public @interface Config {
		@AttributeDefinition(defaultValue = { DEFAULT_ROOT_PATH })
		String root_path();
	}

    private static final String DEFAULT_CONFIG = "/libs/foundation/components/page/tab_basic/items/basic/items/tags";

    private static final String DEFAULT_ROOT_PATH = "/etc/tagconfig";

    private String rootPath;

    @Activate
    protected void activate(TagWidgetConfigurationServlet.Config config) {
        rootPath = config.root_path();
    }

    @Override
    @SuppressWarnings("squid:S3776")
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        String componentPath = request.getResource().getPath();

        String configName = PathInfoUtil.getSelector(request, 1, DEFAULT_CONFIG_NAME);

        // the actual property name
        String propertyName = PathInfoUtil.getSelector(request, 2, "tags");

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
                        try {
                            writeConfigResource(config, propertyName, request, response);
                        } catch (JSONException e) {
                            throw new ServletException(e);
                        }

                        return;
                    }
                }
            }
        }

        returnDefault(propertyName, request, response);

    }

    @Override
    protected JSONObject createEmptyWidget(String propertyName) throws JSONException {
        JSONObject object = new JSONObject();
        object.put("xtype", "tags");
        object.put("name", "./" + xssApi.encodeForJSString(propertyName));
        object.put("fieldLabel", "Tags/Keywords");
        object.put("jcr:primaryType", "cq:Widget");
        return object;
    }

    private void returnDefault(String propertyName, SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException, ServletException {
        response.setContentType("application/json");
        try {
            Resource root = request.getResourceResolver().getResource(DEFAULT_CONFIG);
            if (root == null) {
                writeEmptyWidget(propertyName, response);
                return;
            }

            writeConfigResource(root, propertyName,  request, response);
        } catch (JSONException e) {
            throw new ServletException(e);
        }
    }

    private void writeConfigResource(Resource resource, String propertyName,
            SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, JSONException,
            ServletException {
        JSONObject widget = createEmptyWidget(propertyName);

        RequestParameterMap map = request.getRequestParameterMap();
        for (Map.Entry<String, RequestParameter[]> entry : map.entrySet()) {
            String key = entry.getKey();
            RequestParameter[] params = entry.getValue();
            if (params != null) {
                if (params.length > 1) {
                    JSONArray arr = new JSONArray();
                    for (int i = 0; i < params.length; i++) {
                        arr.put(params[i].getString());
                    }
                    widget.put(key, arr);
                } else if (params.length == 1) {
                    widget.put(key, params[0].getString());
                }
            }
        }

        widget = underlay(widget, resource);

        JSONObject parent = new JSONObject();
        parent.put("xtype", "dialogfieldset");
        parent.put("border", false);
        parent.put("padding", 0);
        parent.put("style", "padding: 0px");
        parent.accumulate("items", widget);
        parent.write(response.getWriter());
    }

}
