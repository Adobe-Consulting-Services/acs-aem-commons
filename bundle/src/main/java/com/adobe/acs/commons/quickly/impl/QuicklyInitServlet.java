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
package com.adobe.acs.commons.quickly.impl;

import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_PATHS;

import java.io.IOException;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;

import com.google.gson.Gson;
import com.google.gson.JsonObject;

/**
 * ACS AEM Commons - Quickly - Init Servlet
 *
 */
@SuppressWarnings("serial")
@Component(service=Servlet.class,
property= {
SLING_SERVLET_PATHS+"=/bin/quickly.init.json"})
public class QuicklyInitServlet extends SlingSafeMethodsServlet {
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Content-Type", " application/json; charset=UTF-8");

        final JsonObject json = new JsonObject();

        json.addProperty("user", request.getResourceResolver().getUserID());
        json.addProperty("throttle", 200);
        
        Gson gson = new Gson();
        gson.toJson(json, response.getWriter());
    }
}
