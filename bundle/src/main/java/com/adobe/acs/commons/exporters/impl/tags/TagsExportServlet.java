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
package com.adobe.acs.commons.exporters.impl.tags;

import java.io.IOException;
import java.io.Writer;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.HttpConstants;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
    immediate = true,
    service = Servlet.class,
    property = {
        "sling.servlet.label=ACS AEM Commons - Tags to CSV - Export Servlet",
        "sling.servlet.methods=" + HttpConstants.METHOD_GET,
        "sling.servlet.resourceTypes=acs-commons/components/utilities/exporters/tags-to-csv",
        "sling.servlet.selectors=export",
        "sling.servlet.extensions=csv"
    }
)
public class TagsExportServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(TagsExportServlet.class);

    @Reference
    private transient TagsExportService tagsExportService;

    /**
     * Generates a CSV file representing tag structure under passed in request path.
     *
     * @param request  the Sling HTTP Request object
     * @param response the Sling HTTP Response object
     * @throws IOException
     * @throws ServletException
     */
    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
      ResourceResolver rr = request.getResourceResolver();
      Parameters params = new Parameters(request);

      if (params.containsPath()) {
        Writer writer = response.getWriter();
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");

        if (params.isLocalized()) {
          writer.write(tagsExportService.exportLocalizedTagsForPath(params.getPath(), rr, params.getDefaultLocalization()));
        } else {
          writer.write(tagsExportService.exportNonLocalizedTagsForPath(params.getPath(), rr));
        }
      } else {
        log.warn("Cannot generate tag CSV file, missing 'path' parameter in request.");
      }
    }
}
