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

package com.adobe.acs.commons.analysis.jcrchecksum;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.TreeSet;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;

@SuppressWarnings("serial")
@Component(metatype = false)
@Service
@Properties({
    @Property(name = "sling.servlet.paths", value = "/bin/hashes", propertyPrivate = true),
    @Property(name = "sling.servlet.extensions", value = "txt", propertyPrivate = true),
    @Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true) })
public class ChecksumGeneratorServlet extends SlingAllMethodsServlet {

    @Override
    public void doGet(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/plain");

        ServletInputProcessor sip =
            new ServletInputProcessor(request, response);
        handleRequest(request, response, sip);
    }

    protected void doPost(SlingHttpServletRequest request,
        SlingHttpServletResponse response) {
        response.setContentType("text/plain");

        ServletInputProcessor sip;
        try {
            sip = new ServletInputProcessor(request, response);
            handleRequest(request, response, sip);
        } catch (Exception e) {
            return;
        }

    }

    private void handleRequest(SlingHttpServletRequest request,
        SlingHttpServletResponse response, ServletInputProcessor sip)
        throws IOException {

        PrintWriter out = response.getWriter();
        Session session = request.getResourceResolver().adaptTo(Session.class);

        TreeSet<String> paths = sip.getPaths();
        if (paths != null) {
            for (String path : paths) {
                if (path == null)
                    continue;
                try {
                    ChecksumGenerator.generateChecksums(session, path,
                        sip.getChecksumGeneratorOptions(), out);
                } catch (RepositoryException e) {
                    // do nothing as errors should have been printed to output
                }
            }
        }
    }
}