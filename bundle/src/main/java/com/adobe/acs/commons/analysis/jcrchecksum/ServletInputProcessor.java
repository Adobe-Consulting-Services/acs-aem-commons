package com.adobe.acs.commons.analysis.jcrchecksum;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;

import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.servlet.ServletException;

import org.apache.sling.api.SlingException;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.QuerySyntaxException;
import org.apache.sling.api.resource.Resource;

public class ServletInputProcessor {
    private ChecksumGeneratorOptions opts = new ChecksumGeneratorOptions();
    private TreeSet<String> pathsList = new TreeSet<String>();
    
    public ServletInputProcessor(SlingHttpServletRequest request,
        SlingHttpServletResponse response) throws IOException, ServletException {
        String[] paths = request.getParameterValues("path");
        String[] nodeTypes = request.getParameterValues("nodeTypes");
        String[] nodeTypeExcludes =
            request.getParameterValues("nodeTypeExcludes");
        String[] propertyExcludes =
            request.getParameterValues("propertyExcludes");
        String[] sortValuesFor = request.getParameterValues("sortValuesFor");
        String query = request.getParameter("query");
        String queryType = request.getParameter("queryType");
        
        processInput(request, response, paths, nodeTypes, nodeTypeExcludes,
            propertyExcludes, sortValuesFor, query, queryType);
    }

    private void processInput(SlingHttpServletRequest request,
        SlingHttpServletResponse response, String[] paths, String[] nodeTypes,
        String[] nodeTypeExcludes, String[] propertyExcludes,
        String[] sortValuesFor, String query, String queryType)
        throws IOException, ServletException {
        if (propertyExcludes == null) {
            propertyExcludes = new String[] {};
        }
        if (nodeTypes == null) {
            nodeTypes = new String[] {};
        }
        if (sortValuesFor == null) {
            sortValuesFor = new String[] {};
        }
        PrintWriter out = response.getWriter();
        InputStream pathInputStream = null;
        
        // add all paths from paths param first
        if (paths != null) {
            for (String path : paths) {
                pathsList.add(path);
            }
        }
        
        if (request.getRequestParameter("data") != null) {
            try {
                pathInputStream =
                    request.getRequestParameter("data").getInputStream();
                BufferedReader br =
                        new BufferedReader(new InputStreamReader(
                            pathInputStream));
                String path = null;
                while ((path = br.readLine()) != null) {
                    pathsList.add(path);
                }
            } catch (IOException e) {
                out.println("ERROR: Unable to read 'data' inputStream of paths");
                throw e;
            }
        }
        
        try {
            // add all query result params
            if (query != null) {
                String qType = queryType;
                if (queryType == null) {
                    if(query.startsWith("/")) {
                        qType = Query.XPATH;
                    } else if(query.toLowerCase().startsWith("select")) {
                        if(query.contains("[")) {
                            qType = Query.JCR_SQL2;
                        } else {
                            qType = Query.SQL;
                        }
                    }
                }
                Iterator<Resource> resIter =
                    request.getResourceResolver().findResources(query, queryType);
                while (resIter.hasNext()) {
                    Resource res = resIter.next();
                    pathsList.add(res.getPath());
                }
            }
        } catch (QuerySyntaxException qse) {
            out.println("ERROR: Invalid query: "  + qse.getMessage());
            throw qse;
        } catch (SlingException se) {
            out.println("ERROR: Unable to execute query: "  + se.getMessage());
            throw se;
        } catch (IllegalStateException ise) {
            out.println("ERROR: Unable to execute query: "  + ise.getMessage());
            throw ise;
        }

        if (pathsList.isEmpty()) {
            out.println("ERROR: You must specify the path.");
            throw new ServletException("ERROR: You must specify the path");
        } else {
            opts.setNodeTypeIncludes(nodeTypes);
            opts.setNodeTypeExcludes(nodeTypeExcludes);
            opts.setPropertyExcludes(propertyExcludes);
            opts.setSortedMultiValueProperties(sortValuesFor);
        }
    }

    public ChecksumGeneratorOptions getChecksumGeneratorOptions() {
        return this.opts;
    }
    
    public TreeSet<String> getPaths() {
        return pathsList;
    }
}
