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
package com.adobe.acs.commons.dam.impl;


import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;


import java.io.IOException;
import java.util.Map;

import javax.annotation.Nonnull;

import javax.servlet.Servlet;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

import com.adobe.acs.commons.util.ParameterUtil;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;


@SuppressWarnings("serial")
@SlingServletPaths("/bin/acs-commons/dam/custom-components.json")
@Component()
@Designate(ocd=CustomComponentActivatorListServlet.Config.class)

public class CustomComponentActivatorListServlet extends SlingSafeMethodsServlet {

static final String HISTORY = "xmpMM:History=/apps/acs-commons/dam/content/admin/history";
static final String FONTS = "xmpTPg:Fonts=/apps/acs-commons/dam/content/admin/fonts";
static final String COLORANTS = "xmpTPg:Colorants=/apps/acs-commons/dam/content/admin/color-swatches";
static final String LOCATION = "location=/apps/acs-commons/dam/content/admin/asset-location-map";
static final String[] DEFAULT_COMPONENTS = { HISTORY, FONTS, COLORANTS, LOCATION };

@ObjectClassDefinition(description = "ACS AEM Commons - Custom DAM Component List Servlet")
public @interface Config {

@AttributeDefinition(description = "Map in the form <propertyName>=<replacement path>", defaultValue = {
HISTORY, FONTS, COLORANTS, LOCATION })
String[] components();
}

private JsonObject json;


@Activate
protected void activate(Config config) {
Map<String, String> components = null;
if (config.components().length == 0) {
components = ParameterUtil.toMap(DEFAULT_COMPONENTS, "=");
} else {
components = ParameterUtil.toMap(config.components(), "=");
}
JsonArray array = new JsonArray();
for (Map.Entry<String, String> entry : components.entrySet()) {
JsonObject obj = new JsonObject();
obj.addProperty("propertyName", entry.getKey());
obj.addProperty("componentPath", entry.getValue());
array.add(obj);
}
this.json = new JsonObject();
json.add("components", array);
}

@Override
protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response)
throws ServletException, IOException {
response.setContentType("application/json");
response.setCharacterEncoding("UTF-8");
response.getWriter().print(json.toString());
}
}
