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
package com.adobe.acs.commons.designer.impl;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletPaths;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.google.gson.stream.JsonWriter;

/**
 * Servlet which produces Options JSON for ClientLibsManager dialog
 *
 */
@SuppressWarnings("serial")
@Component()
@SlingServletPaths("/apps/acs-commons/components/utilities/designer/clientlibsmanager/options.json")
public class OptionsServlet extends SlingSafeMethodsServlet {

    @Reference
    private HtmlLibraryManager libraryManager;

    @Override
    @SuppressWarnings({"squid:S3776", "squid:S1141"})
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("application/json");
        final JsonWriter writer = new JsonWriter(response.getWriter());

        writer.beginArray();
        String type = request.getRequestPathInfo().getSelectorString();
        if (type != null) {
            try {
                Set<String> categories = new TreeSet<String>();
                LibraryType libraryType = LibraryType.valueOf(type.toUpperCase());
                Map<String, ClientLibrary> libraries = libraryManager.getLibraries();
                for (ClientLibrary library : libraries.values()) {
                    if (library.getTypes() != null && library.getTypes().contains(libraryType)) {
                        String[] libraryCats = library.getCategories();
                        if (libraryCats != null) {
                            for (String cat : libraryCats) {
                                categories.add(cat);
                            }
                        }
                    }
                }

                for (String cat : categories) {
                    writer.beginObject();
                    writer.name("value");
                    writer.value(cat);
                    writer.name("text");
                    writer.value(cat);
                    writer.endObject();
                }

            } catch (IllegalArgumentException e) {
                // no matching type. no need to log, just return empty array.
            }
        }
        writer.endArray();

        writer.close();
    }
}
