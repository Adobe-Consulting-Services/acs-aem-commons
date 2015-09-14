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
import java.util.HashSet;

import javax.jcr.Node;
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
@Component(metatype = false)
@Service
@Properties({
    @org.apache.felix.scr.annotations.Property(name = "sling.servlet.paths", value = "/bin/jsondump", propertyPrivate = true),
    @org.apache.felix.scr.annotations.Property(name = "sling.servlet.extensions", value = "json", propertyPrivate = true),
    @org.apache.felix.scr.annotations.Property(name = "sling.servlet.methods", value = "GET", propertyPrivate = true) })
public class JSONDumpServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        Session session = request.getResourceResolver().adaptTo(Session.class);
        //String path = request.getParameter("path");

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
            throw new ServletException("Unable to generate json", e);
        } catch (JSONException e) {
            throw new ServletException("Unable to generate json", e);
        }
    }

    private void collectJSON(Node node, JSONWriter jsonWriter,
        SlingHttpServletResponse response, ChecksumGeneratorOptions opts) throws RepositoryException {
        generateJSON(node, jsonWriter, response, opts);
    }

    private void generateJSON(Node node, JSONWriter jsonWriter,
        SlingHttpServletResponse response, ChecksumGeneratorOptions opts) throws RepositoryException {
        JSONVisitor v = new JSONVisitor(jsonWriter, response, opts);
        try {
            jsonWriter.object();
            node.accept(v);
            jsonWriter.endObject();
        } catch (Exception e) {
        }
    }

    private class JSONVisitor extends Default {
        private JSONWriter jsonWriter;
        private SlingHttpServletResponse response;
        private ChecksumGeneratorOptions opts;
        private HashSet<String> excludedPaths = new HashSet<String>();
        
        public JSONVisitor(JSONWriter jsonWriter,
            SlingHttpServletResponse response,
            ChecksumGeneratorOptions opts) {
            super(false, -1);
            this.jsonWriter = jsonWriter;
            this.response = response;
            this.opts = opts;
            
        }

        @Override
        protected void entering(Node node, int level)
            throws RepositoryException {
            try {
                String path = node.getPath();
                
                for(int i = 1;  !Text.getRelativeParent(path, i).equals(""); i++) {
                    if(excludedPaths.contains(Text.getRelativeParent(path, i))) {
                        return;
                    }
                }
                
                if(opts.getNodeTypeExcludes().contains(node.getPrimaryNodeType().getName())) {
                    excludedPaths.add(node.getPath());
                } else {
                    // response.getWriter().println("\nentering node: " +node.getPath() );
                    jsonWriter.key(node.getName()).object();
                }
            } catch (Exception e) {
                try {
                    response.getWriter().println(
                        "\nERROR: entering node=" + node.getPath() + " "
                            + e.getMessage());
                } catch (IOException io) {
                }
            }
        }

        @Override
        protected void entering(Property property, int level)
            throws RepositoryException {
            try {
                // response.getWriter().println("\nentering prop: " +
                // property.getPath() );
                if (!opts.getPropertyExcludes().contains(property.getName())) {
                    jsonWriter.key(property.getName());
                    if (property.isMultiple()) {
                        jsonWriter.array();
                        for (Value value : property.getValues()) {
                            jsonWriter.value(value.getString());
                        }
                        jsonWriter.endArray();
                    } else {
                        jsonWriter.value(property.getString());
                    }
                }
            } catch (Exception e) {
                try {
                    response.getWriter().println(
                        "\nERROR: entering property=" + property.getPath()
                            + " " + e.getMessage());
                } catch (IOException io) {
                }

            }
        }

        @Override
        protected void leaving(Node node, int level) throws RepositoryException {
            try {
                // response.getWriter().println("<br>leaving node: " +
                // node.getPath() );
                jsonWriter.endObject();
            } catch (Exception e) {
                try {
                    response.getWriter().println(
                        "\nERROR: leaving node=" + node.getPath() + " "
                            + e.getMessage());
                } catch (IOException io) {
                }
            }

        }

        @Override
        protected void leaving(Property property, int level)
            throws RepositoryException {

        }
    }
}