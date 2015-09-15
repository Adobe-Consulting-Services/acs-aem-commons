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

package com.adobe.acs.commons.analysis.jcrchecksum;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.util.TraversingItemVisitor.Default;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.util.Text;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Commons - JCR Checksum JSON Dump Servlet",
        paths = { "/bin/acs-commons/jcr-compare.dump.json" }
)
public class JSONDumpServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
                         SlingHttpServletResponse response) throws ServletException, IOException {

        response.setContentType("application/json");
        response.setHeader("Content-Disposition", "filename=" + "jcr-compare-dump.json");

        Session session = request.getResourceResolver().adaptTo(Session.class);

        //Generate current date and time for filename
        DateFormat df = new SimpleDateFormat("yyyyddMM_HHmmss");
        Date today = Calendar.getInstance().getTime();        
        String filename = df.format(today);

        response.setHeader("Content-Disposition", "filename=jcr-checksum-" + filename + ".json");


        ServletInputProcessor sip = null;
        try {
            sip = new ServletInputProcessor(request, response);
        } catch (Exception e) {
            return;
        }
        JSONWriter jsonWriter = new JSONWriter(response.getWriter());

        try {
            JSONGenerator.generateJSON(session, sip.getPaths(), sip.getChecksumGeneratorOptions(), jsonWriter);

            //collectJSON(session.getRootNode().getNode("." + path), jsonWriter,
            //    response, sip.getChecksumGeneratorOptions());
        } catch (RepositoryException e) {
            throw new ServletException("Error accessing repository", e);
        } catch (JSONException e) {
            throw new ServletException("Unable to generate json", e);
        }
    }
}