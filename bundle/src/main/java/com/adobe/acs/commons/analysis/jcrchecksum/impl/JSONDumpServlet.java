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

import com.adobe.acs.commons.analysis.jcrchecksum.ChecksumGeneratorOptions;
import com.adobe.acs.commons.analysis.jcrchecksum.JSONGenerator;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.ChecksumGeneratorOptionsFactory;
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.RequestChecksumGeneratorOptions;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;
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
import java.util.Map;
import java.util.Set;

@SuppressWarnings("serial")
@SlingServlet(label = "ACS AEM Commons - JCR Checksum JSON Dump Servlet",
        paths = { ChecksumGeneratorServlet.SERVLET_PATH  + "."
                    + JSONDumpServlet.SERVLET_SELECTOR + "."
                    + JSONDumpServlet.SERVLET_EXTENSION})
public class JSONDumpServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory
        .getLogger(JSONDumpServlet.class);

    public static final String SERVLET_SELECTOR = "dump";
    
    public static final String SERVLET_EXTENSION = "json";
    
    private static final String DEFAULT_ALLOW_ORIGIN = "*";

    private String allowOrigin = DEFAULT_ALLOW_ORIGIN;

    @Property(label = "Access-Control-Allow-Origin response header value",
            description = "Set to the hostname(s) of the AEM Author environment",
            value = DEFAULT_ALLOW_ORIGIN)
    public static final String PROP_ALLOW_ORIGIN = "access-control-allow-origin";

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

        if (StringUtils.isNotBlank(this.allowOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", this.allowOrigin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST");
        }

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

        Set<String> paths = RequestChecksumGeneratorOptions.getPaths(request);

        if (CollectionUtils.isEmpty(paths)) {
            try {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().print(
                    "ERROR: At least one path must be specified");
            } catch (IOException ioe) {
                throw ioe;
            }
        }

        Session session = request.getResourceResolver().adaptTo(Session.class);

        JSONWriter jsonWriter = new JSONWriter(response.getWriter());

        try {
            JSONGenerator.generateJSON(session, paths, options, jsonWriter);
        } catch (RepositoryException e) {
            throw new ServletException("Error accessing repository", e);
        } catch (JSONException e) {
            throw new ServletException("Unable to generate json", e);
        }
    }

    @Activate
    protected final void activate(Map<String, Object> config) {
        this.allowOrigin =
                PropertiesUtil.toString(config.get(PROP_ALLOW_ORIGIN), DEFAULT_ALLOW_ORIGIN);
    }
}