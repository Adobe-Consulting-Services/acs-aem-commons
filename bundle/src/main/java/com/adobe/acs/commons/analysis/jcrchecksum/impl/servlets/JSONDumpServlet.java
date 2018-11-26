/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl.servlets;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.JSONGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.ChecksumGeneratorOptionsFactory;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.RequestChecksumGeneratorOptions;
import com.google.gson.stream.JsonWriter;

import org.apache.commons.collections.CollectionUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Set;

@SuppressWarnings("serial")
@Component(property= {
        "sling.servlet.paths="+JSONDumpServlet.SERVLET_PATH,
        "sling.auth.requirements=-"+JSONDumpServlet.SERVLET_PATH
        })

public class JSONDumpServlet extends BaseChecksumServlet {
    
    
    
    
    private static final Logger log = LoggerFactory.getLogger(JSONDumpServlet.class);

    public static final String SERVLET_PATH =  ServletConstants.SERVLET_PATH  + "."
            + ServletConstants.JSON_SERVLET_SELECTOR + "."
            + ServletConstants.JSON_SERVLET_EXTENSION;

    @Override
    public final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
    ServletException, IOException {
        try {
            this.handleCORS(request, response);
            this.handleRequest(request, response);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    public final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
    ServletException, IOException {
        try {
            this.handleCORS(request, response);
            this.handleRequest(request, response);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    private void handleRequest(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws IOException,
            RepositoryException, ServletException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        // Generate current date and time for filename
        DateFormat df = new SimpleDateFormat("yyyyddMM_HHmmss");
        Date today = Calendar.getInstance().getTime();
        String filename = df.format(today);

        response.setHeader("Content-Disposition", "filename=jcr-checksum-"
                + filename + ".json");

        String optionsName = request.getParameter(ServletConstants.OPTIONS_NAME);
        ChecksumGeneratorOptions options =
                ChecksumGeneratorOptionsFactory.getOptions(request, optionsName);

        if (log.isDebugEnabled()) {
            log.debug(options.toString());
        }

        Set<String> paths = RequestChecksumGeneratorOptions.getPaths(request);

        if (CollectionUtils.isEmpty(paths)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(
                        "ERROR: At least one path must be specified");
            } catch (IOException ioe) {
                throw ioe;
            }
        } else {
            Session session = request.getResourceResolver().adaptTo(Session.class);

            JsonWriter jsonWriter = new JsonWriter(response.getWriter());

            try {
                JSONGenerator.generateJSON(session, paths, options, jsonWriter);
                jsonWriter.close();
            } catch (RepositoryException e) {
                throw new ServletException("Error accessing repository", e);
            } catch (IOException e) {
                throw new ServletException("Unable to generate json", e);
            }
        }
    }
}