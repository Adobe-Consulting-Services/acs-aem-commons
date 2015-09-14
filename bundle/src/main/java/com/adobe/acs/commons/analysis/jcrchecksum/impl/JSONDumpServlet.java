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
import com.adobe.acs.commons.analysis.jcrchecksum.impl.options.DefaultChecksumGeneratorOptions;
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
import java.io.IOException;

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

        String path = request.getParameter("path");
        Session session = request.getResourceResolver().adaptTo(Session.class);

        String filename;

        if (StringUtils.isBlank(path)) {
            filename = "unknown_path";
        } else {
            filename = StringUtils.replace(path, "/", "_");
        }

        response.setHeader("Content-Disposition", "filename=jcr-checksum" + filename + ".json");
        JSONWriter jsonWriter = new JSONWriter(response.getWriter());

        try {

            ChecksumGeneratorOptions options = new DefaultChecksumGeneratorOptions(request);
            this.generateJSON(session.getRootNode().getNode("." + path), jsonWriter, options);

        } catch (JSONVisitorException e) {
            throw new ServletException("Unable to create value JSON object", e);
        } catch (PathNotFoundException e) {
            throw new ServletException("Unable to find path at: " + path, e);
        } catch (RepositoryException e) {
            throw new ServletException("Error accessing repository", e);
        }
    }

    private void generateJSON(Node node, JSONWriter jsonWriter,
                              ChecksumGeneratorOptions options) throws JSONVisitorException, RepositoryException {

        JSONVisitor v = new JSONVisitor(jsonWriter, options);

        try {
            jsonWriter.object();
            node.accept(v);
            jsonWriter.endObject();
        } catch (JSONException e) {
            throw new JSONVisitorException(e);
        }
    }

    private class JSONVisitor extends Default {
        private final ChecksumGeneratorOptions options;
        private final JSONWriter jsonWriter;

        public JSONVisitor(final JSONWriter jsonWriter, final ChecksumGeneratorOptions options) {
            super(false, -1);
            this.jsonWriter = jsonWriter;
            this.options = options;
        }

        @Override
        protected void entering(Node node, int level) throws RepositoryException {
            try {
                jsonWriter.key(node.getName()).object();
            } catch (JSONException e) {
                throw new JSONVisitorException(e);
            }
        }

        @Override
        protected void entering(Property property, int level) throws RepositoryException {
            try {
                if (!this.options.getExcludedProperties().contains(property.getName())) {
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
            } catch (JSONException e) {
                throw new JSONVisitorException(e);
            }
        }

        @Override
        protected void leaving(Node node, int level) throws RepositoryException {
            try {
                jsonWriter.endObject();
            } catch (JSONException e) {
                throw new JSONVisitorException(e);
            }
        }

        @Override
        protected void leaving(Property property, int level) throws RepositoryException {
            // DO NOTHING
        }
    }

    /**
     * Custom exception to track JSON construction errors while leveraging the
     * TraversingItemVisitor.Default API; Must extend RepositoryException
     */
    private final class JSONVisitorException extends RepositoryException {
        public JSONVisitorException(Exception e) {
            super(e);
        }
    }
}