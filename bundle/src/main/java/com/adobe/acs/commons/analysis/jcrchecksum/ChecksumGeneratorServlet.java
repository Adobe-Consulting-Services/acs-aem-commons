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

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.oak.commons.PropertiesUtil;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.servlet.ServletException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

@SuppressWarnings("serial")
@Component(
        label = "ACS AEM Commons - JCR Checksum Servlet",
        metatype = false
)
@Properties({
        @Property(
                name = "sling.servlet.paths",
                value = "/bin/acs-commons/jcr-compare.hashes.txt",
                propertyPrivate = true)
})
@Service
public class ChecksumGeneratorServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(ChecksumGeneratorServlet.class);

    private static final String DEFAULT_ALLOW_ORIGIN = "*";

    private String allowOrigin = DEFAULT_ALLOW_ORIGIN;

    @Property(label = "Access-Control-Allow-Origin response header value",
            description = "Set to the hostname(s) of the AEM Author environment",
            value = DEFAULT_ALLOW_ORIGIN)
    public static final String PROP_ALLOW_ORIGIN = "access-control-allow-origin";


    private static final String PATH = "path";

    private static final String QUERY = "query";

    private static final String QUERY_TYPE = "queryType";

    private static final String NODES_TYPES = "nodeTypes";

    private static final String NODE_TYPE_EXCLUDES = "nodeTypeExcludes";

    private static final String PROPERTY_EXCLUDES = "propertyExcludes";

    private static final String SORT_VALUES_FOR = "sortValuesFor";


    @Override
    public void doGet(SlingHttpServletRequest request,
                      SlingHttpServletResponse response) {

        response.setContentType("text/plain");

        if (StringUtils.isNotBlank(this.allowOrigin)) {
            response.setHeader("Access-Control-Allow-Origin", this.allowOrigin);
        }

        String[] paths = request.getParameterValues(PATH);
        String query = request.getParameter(QUERY);
        String queryType = request.getParameter(QUERY_TYPE);
        String[] nodeTypes = request.getParameterValues(NODES_TYPES);
        String[] nodeTypeExcludes = request.getParameterValues(NODE_TYPE_EXCLUDES);
        String[] propertyExcludes = request.getParameterValues(PROPERTY_EXCLUDES);
        String[] ignoreOrder = request.getParameterValues(SORT_VALUES_FOR);

        ArrayList<String> pathArr = new ArrayList<String>();
        // add all paths from paths param first
        if (paths != null) {
            for (String path : paths) {
                pathArr.add(path);
            }
        }
        // add all query result params
        if (query != null) {

            queryType = StringUtils.defaultIfEmpty(queryType, "xpath");
            Iterator<Resource> resources = request.getResourceResolver().findResources(query, queryType);

            while (resources.hasNext()) {
                Resource res = resources.next();
                pathArr.add(res.getPath());
            }
        }

        try {
            handleRequest(request, response, pathArr.toArray(new String[]{}),
                    nodeTypes, nodeTypeExcludes, propertyExcludes, ignoreOrder,
                    null);
        } catch (IOException e) {
            log.error("Unable to handle Checksum request", e);
        }
    }

    protected void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException {
        response.setContentType("text/plain");

        String[] paths = request.getParameterValues("path");
        String[] nodeTypes = request.getParameterValues("nodeTypes");
        String[] nodeTypeExcludes = request.getParameterValues("nodeTypeExcludes");
        String[] propertyExcludes = request.getParameterValues("propertyExcludes");
        String[] sortValuesFor = request.getParameterValues("sortValuesFor");
        InputStream is = null;

        try {

            is = request.getRequestParameter("data").getInputStream();
            handleRequest(request, response, paths, nodeTypes,
                    nodeTypeExcludes, propertyExcludes, sortValuesFor, is);

        } catch (IOException e) {
            throw new ServletException(e);
        }
    }

    private void handleRequest(SlingHttpServletRequest request,
                               SlingHttpServletResponse response, String[] paths, String[] nodeTypes,
                               String[] nodeTypeExcludes, String[] excludes, String[] sortValuesFor,
                               InputStream pathInputStream) throws IOException {
        if (excludes == null) {
            excludes = new String[]{};
        }

        if (nodeTypes == null) {
            nodeTypes = new String[]{};
        }

        if (sortValuesFor == null) {
            sortValuesFor = new String[]{};
        }

        PrintWriter out = response.getWriter();
        Session session;

        session = request.getResourceResolver().adaptTo(Session.class);

        if ((paths == null || paths.length == 0) && pathInputStream == null) {
            out.println("ERROR: You must specify the path.");
        } else {
            try {
                ChecksumGeneratorOptions opts = new ChecksumGeneratorOptions();
                opts.setNodeTypeIncludes(nodeTypes);
                opts.setNodeTypeExcludes(nodeTypeExcludes);
                opts.setPropertyExcludes(excludes);
                opts.setSortedMultiValueProperties(sortValuesFor);

                if (paths != null) {
                    for (String path : paths) {
                        ChecksumGenerator.generateChecksums(session, path, opts, out);
                    }
                }

                if (pathInputStream != null) {
                    BufferedReader br = new BufferedReader(new InputStreamReader(pathInputStream));
                    String path = null;
                    try {
                        while ((path = br.readLine()) != null) {
                            ChecksumGenerator.generateChecksums(session, path, opts, out);
                        }
                    } catch (Exception e) {
                        out.println(e);
                    } finally {
                        br.close();
                    }
                }
            } catch (RepositoryException e) {
                out.println(e.getMessage());
            }
        }
    }

    @Activate
    protected final void activate(Map<String, Object> config) {
        this.allowOrigin =
                PropertiesUtil.toString(config.get(PROP_ALLOW_ORIGIN), DEFAULT_ALLOW_ORIGIN);
    }
}