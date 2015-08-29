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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
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
        SlingHttpServletResponse response) {
        response.setContentType("text/plain");

        String[] paths = request.getParameterValues("path");
        String query = request.getParameter("query");
        String queryType = request.getParameter("queryType");
        String[] nodeTypes = request.getParameterValues("nodeTypes");
        String[] nodeTypeExcludes =
            request.getParameterValues("nodeTypeExcludes");
        String[] propertyExcludes =
            request.getParameterValues("propertyExcludes");
        String[] ignoreOrder = request.getParameterValues("sortValuesFor");

        ArrayList<String> pathArr = new ArrayList<String>();
        // add all paths from paths param first
        if (paths != null) {
            for (String path : paths) {
                pathArr.add(path);
            }
        }
        // add all query result params
        if (query != null) {
            if (queryType == null) {
                queryType = "xpath";
            }
            Iterator<Resource> resIter =
                request.getResourceResolver().findResources(query, queryType);
            while (resIter.hasNext()) {
                Resource res = resIter.next();
                pathArr.add(res.getPath());
            }
        }

        try {
            handleRequest(request, response, pathArr.toArray(new String[] {}),
                nodeTypes, nodeTypeExcludes, propertyExcludes, ignoreOrder,
                null);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    protected void doPost(SlingHttpServletRequest request,
        SlingHttpServletResponse response) {
        response.setContentType("text/plain");

        String[] paths = request.getParameterValues("path");
        String[] nodeTypes = request.getParameterValues("nodeTypes");
        String[] nodeTypeExcludes =
            request.getParameterValues("nodeTypeExcludes");
        String[] propertyExcludes =
            request.getParameterValues("propertyExcludes");
        String[] sortValuesFor = request.getParameterValues("sortValuesFor");
        InputStream is = null;
        try {
            is = request.getRequestParameter("data").getInputStream();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        try {
            handleRequest(request, response, paths, nodeTypes,
                nodeTypeExcludes, propertyExcludes, sortValuesFor, is);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    private void handleRequest(SlingHttpServletRequest request,
        SlingHttpServletResponse response, String[] paths, String[] nodeTypes,
        String[] nodeTypeExcludes, String[] excludes, String[] sortValuesFor,
        InputStream pathInputStream) throws IOException {
        if (excludes == null) {
            excludes = new String[] {};
        }
        if (nodeTypes == null) {
            nodeTypes = new String[] {};
        }
        if (sortValuesFor == null) {
            sortValuesFor = new String[] {};
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
                        ChecksumGenerator.generateChecksums(session, path,
                            opts, out);
                    }
                }
                if (pathInputStream != null) {
                    BufferedReader br =
                        new BufferedReader(new InputStreamReader(
                            pathInputStream));
                    String path = null;
                    try {
                        while ((path = br.readLine()) != null) {
                            ChecksumGenerator.generateChecksums(session, path,
                                opts, out);
                            // out.println(path);
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
}