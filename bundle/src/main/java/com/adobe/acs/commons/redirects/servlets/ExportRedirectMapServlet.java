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
package com.adobe.acs.commons.redirects.servlets;

import com.adobe.acs.commons.redirects.filter.RedirectFilter;
import com.adobe.acs.commons.redirects.models.RedirectRule;
import com.adobe.acs.commons.redirects.servlets.impl.CsvRedirectExporter;
import com.adobe.acs.commons.redirects.servlets.impl.ExcelRedirectExporter;

import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Servlet;
import java.io.IOException;
import java.util.Collection;

import static com.adobe.acs.commons.redirects.servlets.ImportRedirectMapServlet.CONTENT_TYPE_CSV;
import static com.adobe.acs.commons.redirects.servlets.ImportRedirectMapServlet.CONTENT_TYPE_EXCEL;
import static com.adobe.acs.commons.redirects.filter.RedirectFilter.ACS_REDIRECTS_RESOURCE_TYPE;


/**
 * A servlet to export redirect configurations into an Excel spreadsheet
 *
 */
@Component(service = Servlet.class, immediate = true, name = "ExportRedirectMapServlet", property = {
        "sling.servlet.label=ACS AEM Commons - Export Redirects Servlet",
        "sling.servlet.methods=GET",
        "sling.servlet.selectors=export",
        "sling.servlet.resourceTypes=" + ACS_REDIRECTS_RESOURCE_TYPE
})
public class ExportRedirectMapServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(ExportRedirectMapServlet.class);
    private static final long serialVersionUID = -3564475196678277711L;


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException {

        String path = request.getParameter("path");
        Resource root = request.getResourceResolver().getResource(path);
        log.debug("Requesting redirect maps from {}", path);

        Collection<RedirectRule> rules = RedirectFilter.getRules(root);
        String fileName = root.getParent().getParent().getName() + "-redirects";
        String ext = request.getRequestPathInfo().getExtension();
        if("csv".equals(ext)){
            response.setContentType(CONTENT_TYPE_CSV );
            response.setHeader("Content-Disposition", "attachment;filename=\""+fileName+".csv\" ");
            new CsvRedirectExporter().export(rules, response.getOutputStream());
        } else {
            response.setContentType(CONTENT_TYPE_EXCEL);
            response.setHeader("Content-Disposition", "attachment;filename=\""+fileName+".xlsx\" ");

            new ExcelRedirectExporter().export(rules, response.getOutputStream());
        }
    }
}
