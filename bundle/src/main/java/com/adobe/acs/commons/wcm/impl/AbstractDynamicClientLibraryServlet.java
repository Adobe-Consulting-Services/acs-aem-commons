/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;

import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.jetbrains.annotations.NotNull;

import com.adobe.granite.ui.clientlibs.ClientLibrary;
import com.adobe.granite.ui.clientlibs.HtmlLibraryManager;
import com.adobe.granite.ui.clientlibs.LibraryType;
import com.google.gson.stream.JsonWriter;

abstract class AbstractDynamicClientLibraryServlet extends SlingSafeMethodsServlet {


    private transient HtmlLibraryManager htmlLibraryManager;

    private String[] categories;
    private boolean excludeAll;

    @Override
    protected void doGet(@NotNull SlingHttpServletRequest request, @NotNull SlingHttpServletResponse response) throws ServletException, IOException {
        ResourceResolver resourceResolver = request.getResourceResolver();
        response.setContentType("application/json");
        JsonWriter writer = new JsonWriter(response.getWriter());
        writer.beginObject();

        writer.name("js");
        writer.beginArray();
        if (!excludeAll) {
            Collection<ClientLibrary> libraries = htmlLibraryManager.getLibraries(categories, LibraryType.JS, true, true);
            for (ClientLibrary library : libraries) {
                writer.value(resourceResolver.map(request, library.getIncludePath(LibraryType.JS, htmlLibraryManager.isMinifyEnabled())));
            }
        }
        writer.endArray();

        writer.name("css");
        writer.beginArray();
        if (!excludeAll) {
            Collection<ClientLibrary> libraries = htmlLibraryManager.getLibraries(categories, LibraryType.CSS, true, true);
            for (ClientLibrary library : libraries) {
                writer.value(resourceResolver.map(request, library.getIncludePath(LibraryType.CSS, htmlLibraryManager.isMinifyEnabled())));
            }
        }
        writer.endArray();

        writer.endObject();
    }

    protected void activate(String[] categories, boolean excludeAll, HtmlLibraryManager htmlLibraryManager) {
        this.categories = Optional.ofNullable(categories)
                .map(array -> Arrays.copyOf(array, array.length))
                .orElse(null);
        this.excludeAll = excludeAll;
        this.htmlLibraryManager = htmlLibraryManager;
    }
}
