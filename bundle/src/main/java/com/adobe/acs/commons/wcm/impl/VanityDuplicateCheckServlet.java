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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.commons.TidyJSONWriter;
import com.day.cq.wcm.api.NameConstants;


/**
 * Class used by the vanity URL check validation javascript. It determines if the vanity URL entered by the user is
 * already in use already and returns the list of pages in which the vanity path is used.
 */

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Commons - Unique Vanity Path Checker",
        description = "Checks if the entered vanity path is already in use",
        metatype = false,
        paths = { "/bin/wcm/duplicateVanityCheck" },
        methods = { "GET" }
)

public class VanityDuplicateCheckServlet extends SlingSafeMethodsServlet{

    private static final Logger log = LoggerFactory.getLogger(VanityDuplicateCheckServlet.class);

    /**
     * Overriden doGet method, runs a query to see if the vanity URL entered by the user in already in use.
     * @param request
     * @param response
     * @throws ServletException
     * @throws IOException
     */
    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException, IOException {

        try {
            final ResourceResolver resolver = request.getResourceResolver();
            final String vanityPath = request.getParameter("vanityPath");
            final String pagePath = request.getParameter("pagePath");
            log.debug("vanity path parameter passed is {}", vanityPath);
            log.debug("page path parameter passed is {}", pagePath);

            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            JSONWriter jsonWriter = new JSONWriter(response.getWriter());
            jsonWriter.array();

            if (StringUtils.isNotBlank(vanityPath)) {
                String xpath = "//element(*)[" + NameConstants.PN_SLING_VANITY_PATH + "='"+ vanityPath + "']";
                @SuppressWarnings("deprecation")
                Iterator<Resource> resources = resolver.findResources(xpath, Query.XPATH);
                while (resources.hasNext()) {
                    Resource resource = resources.next();
                    String path = resource.getPath();
                    if (path.startsWith("/content") && !path.equals(pagePath)) {
                        jsonWriter.value(path);
                    }
                }

            }
            jsonWriter.endArray();
        } catch (JSONException e) {
            throw new ServletException("Unable to generate JSON result", e);
        }
    }


}
