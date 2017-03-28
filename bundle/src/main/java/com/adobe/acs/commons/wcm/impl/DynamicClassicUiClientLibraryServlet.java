/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.google.gson.stream.JsonWriter;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;

import javax.annotation.Nonnull;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.Map;

@SlingServlet(paths = "/bin/acs-commons/dynamic-classicui-clientlibs.json",
    label = "ACS AEM Commons - Dynamic Classic UI Client Library Loader",
    description = "Allows for dynamic loading of optional Classic UI Client Libraries",
    metatype = true)
public class DynamicClassicUiClientLibraryServlet extends SlingSafeMethodsServlet {

    private static final String CATEGORY_LIMIT = "acs-commons.cq-widgets.add-ons.classicui-limit-parsys";
    private static final String CATEGORY_PLACEHOLDER = "acs-commons.cq-widgets.add-ons.classicui-parsys-placeholder";

    private static final String[] DEFAULT_CATEGORIES = new String[] {
            CATEGORY_LIMIT,
            CATEGORY_PLACEHOLDER
    };

    private static final boolean DEFAULT_EXCLUDE_ALL = false;

    @Property(label = "Client Library Categories", description = "Client Library Categories", value = {
            CATEGORY_LIMIT,
            CATEGORY_PLACEHOLDER
    })
    private static final String PROP_CATEGORIES = "categories";

    @Property(label = "Exclude All", description = "Exclude all client library categories", boolValue = DEFAULT_EXCLUDE_ALL)
    private static final String PROP_EXCLUDE_ALL = "exclude.all";

    @Reference
    private HtmlLibraryManager htmlLibraryManager;

    private String[] categories;
    private boolean excludeAll;

    @Override
    protected void doGet(@Nonnull SlingHttpServletRequest request, @Nonnull SlingHttpServletResponse response) throws ServletException, IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        response.setContentType("application/json");
        JsonWriter writer = new JsonWriter(response.getWriter());
        writer.beginObject();

        writer.name("js");
        writer.beginArray();
        if (!excludeAll) {
            Collection<ClientLibrary> libraries = htmlLibraryManager.getLibraries(categories, LibraryType.JS, true, true);
            for (ClientLibrary library : libraries) {
                writer.value(resourceResolver.map(library.getIncludePath(LibraryType.JS, htmlLibraryManager.isMinifyEnabled())));
            }
        }
        writer.endArray();

        writer.name("css");
        writer.beginArray();
        if (!excludeAll) {
            Collection<ClientLibrary> libraries = htmlLibraryManager.getLibraries(categories, LibraryType.CSS, true, true);
            for (ClientLibrary library : libraries) {
                writer.value(resourceResolver.map(library.getIncludePath(LibraryType.CSS, htmlLibraryManager.isMinifyEnabled())));
            }
        }
        writer.endArray();

        writer.endObject();
    }

    @Activate
    private void activate(Map<String, Object> config) {
        this.excludeAll = PropertiesUtil.toBoolean(config.get(PROP_EXCLUDE_ALL), DEFAULT_EXCLUDE_ALL);
        this.categories = PropertiesUtil.toStringArray(config.get(PROP_CATEGORIES), DEFAULT_CATEGORIES);
    }
}
