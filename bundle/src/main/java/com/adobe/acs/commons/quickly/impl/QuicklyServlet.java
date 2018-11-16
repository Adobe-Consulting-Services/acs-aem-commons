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

import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.QuicklyEngine;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import javax.servlet.ServletException;

import java.io.IOException;

/**
 * ACS AEM Commons - Quickly - Servlet End-point
 *
 */
@SuppressWarnings("serial")
@SlingServlet(paths = "/bin/quickly.json")
public class QuicklyServlet extends SlingSafeMethodsServlet {

    @Reference
    private QuicklyEngine quicklyEngine;

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Command cmd = new Command(request);

        response.setHeader("Content-Type", " application/json; charset=UTF-8");

        try {
            response.getWriter().append(quicklyEngine.execute(request, response, cmd).toString());
        } catch (Exception e) {
            response.sendError(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().print("{\"status:\": \"error\"}");
        }
    }
}
