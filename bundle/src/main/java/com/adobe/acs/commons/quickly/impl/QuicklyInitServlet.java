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

import com.adobe.acs.commons.quickly.QuicklyEngine;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.ServletException;

import java.io.IOException;

/**
 * ACS AEM Commons - Quickly - Init Servlet
 *
 */
@SuppressWarnings("serial")
@SlingServlet(paths = "/bin/quickly.init.json")
public class QuicklyInitServlet extends SlingSafeMethodsServlet {
    @Reference
    private QuicklyEngine quicklyEngine;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setHeader("Content-Type", " application/json; charset=UTF-8");

        final JSONObject json = new JSONObject();

        try {
            json.put("user", request.getResourceResolver().getUserID());
            json.put("throttle", 200);
            response.getWriter().write(json.toString());
        } catch (JSONException e) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"status:\": \"error\"}");
        }
    }
}
