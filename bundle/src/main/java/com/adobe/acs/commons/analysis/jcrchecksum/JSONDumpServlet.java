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

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.io.JSONWriter;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.util.TraversingItemVisitor.Default;
import javax.servlet.ServletException;
import java.io.IOException;

@SuppressWarnings("serial")
@Component(metatype = false)
@Service
@Properties({
        @org.apache.felix.scr.annotations.Property(
                name = "sling.servlet.paths",
                value = "/bin/acs-commons/jcr-compare.dump.json",
                propertyPrivate = true)
})
public class JSONDumpServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws ServletException, IOException {
        response.setContentType("application/json");
        Session session = request.getResourceResolver().adaptTo(Session.class);
        String path = request.getParameter("path");

        JSONWriter jsonWriter = new JSONWriter(response.getWriter());
        try {
            collectJSON(session.getRootNode().getNode("." + path), jsonWriter,
                response);
        } catch (RepositoryException e) {
            throw new ServletException("Unable to collect hashes", e);
        }
    }

    private void collectJSON(Node node, JSONWriter jsonWriter,
        SlingHttpServletResponse response) throws RepositoryException {
        generateJSON(node, jsonWriter, response);
    }

    private void generateJSON(Node node, JSONWriter jsonWriter,
        SlingHttpServletResponse response) throws RepositoryException {
        JSONVisitor v = new JSONVisitor(jsonWriter, response);
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

        public JSONVisitor(JSONWriter jsonWriter,
            SlingHttpServletResponse response) {
            super(false, -1);
            this.jsonWriter = jsonWriter;
            this.response = response;

        }

        @Override
        protected void entering(Node node, int level)
            throws RepositoryException {
            try {
                // response.getWriter().println("\nentering node: " +
                // node.getPath() );
                jsonWriter.key(node.getName()).object();
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
                if (!ChecksumGeneratorOptions.DEFAULT_EXCLUDED_PROPERTIES
                    .contains(property.getName())) {
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