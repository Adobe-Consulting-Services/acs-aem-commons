/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2015 Adobe
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

package com.adobe.acs.commons.analysis.jcrchecksum.impl;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.commons.collections.CollectionUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.JSONGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.ChecksumGeneratorOptionsFactory;

@SuppressWarnings("serial")
@SlingServlet(label = "ACS AEM Commons - JCR Checksum JSON Dump Servlet", paths = { "/bin/acs-commons/jcr-compare.dump.json" })
public class JSONDumpServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory
        .getLogger(JSONDumpServlet.class);

    @Override
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        try {
            this.handleRequest(request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        try {
            this.handleRequest(request, response);
        } catch (IOException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }
    }
    
    private void handleRequest(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws IOException,
        RepositoryException, ServletException {
        
        response.setContentType("application/json");

        // Generate current date and time for filename
        DateFormat df = new SimpleDateFormat("yyyyddMM_HHmmss");
        Date today = Calendar.getInstance().getTime();
        String filename = df.format(today);

        response.setHeader("Content-Disposition", "filename=jcr-checksum-"
            + filename + ".json");
        
        
        String optionsName = request.getParameter("optionsName");
        ChecksumGeneratorOptions options =
            ChecksumGeneratorOptionsFactory.getOptions(request, optionsName);

        log.debug(options.toString());

        if (CollectionUtils.isEmpty(options.getPaths())) {
            try {
                response.setStatus(400);
                response.getWriter().print(
                    "ERROR: At least one path must be specified");
            } catch (IOException ioe) {
                throw ioe;
            }
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);

        JSONWriter jsonWriter = new JSONWriter(response.getWriter());

        try {
            JSONGenerator.generateJSON(session, options.getPaths(), options,
                jsonWriter);
        } catch (RepositoryException e) {
            throw new ServletException("Error accessing repository", e);
        } catch (JSONException e) {
            throw new ServletException("Unable to generate json", e);
        }
    }
}