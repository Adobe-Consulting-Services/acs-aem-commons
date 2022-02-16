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

import javax.jcr.query.Query;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.day.cq.wcm.api.NameConstants;
import com.google.gson.Gson;
import java.util.ArrayList;
import java.util.List;

/**
 * Class used by the vanity URL check validation javascript. It determines if
 * the vanity URL entered by the user is already in use already and returns the
 * list of pages in which the vanity path is used.
 */
@SuppressWarnings("serial")
@SlingServlet(
        metatype = false,
        paths = {"/bin/wcm/duplicateVanityCheck"},
        methods = {"GET"}
)
public final class VanityDuplicateCheckServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(VanityDuplicateCheckServlet.class);

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        final ResourceResolver resolver = request.getResourceResolver();
        final String vanityPath = request.getParameter("vanityPath");
        final String pagePath = request.getParameter("pagePath");
        log.debug("vanity path parameter passed is {}; page path parameter passed is {}", vanityPath, pagePath);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        List<String> paths = new ArrayList<>();

        if (StringUtils.isNotBlank(vanityPath)) {
            String xpath = "//element(*)[" + NameConstants.PN_SLING_VANITY_PATH + "='" + vanityPath + "']";
            @SuppressWarnings("deprecation")
            Iterator<Resource> resources = resolver.findResources(xpath, Query.XPATH);
            while (resources.hasNext()) {
                Resource resource = resources.next();
                String path = resource.getPath();
                if (path.startsWith("/content") && !path.equals(pagePath)) {
                    paths.add(path);
                }
            }
        }

        Gson gson = new Gson();
        String json = gson.toJson(paths); // #2749
        response.getWriter().write(json);
    }

}
