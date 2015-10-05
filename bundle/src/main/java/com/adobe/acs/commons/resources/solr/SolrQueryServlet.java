package com.adobe.acs.commons.resources.solr;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONObject;

import javax.servlet.ServletException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@SlingServlet(paths="/services/solr/query")
public class SolrQueryServlet extends SlingSafeMethodsServlet {

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        ResourceResolver resourceResolver = request.getResourceResolver();
        String query = request.getQueryString();

        Iterator<Resource> resourceIterator = resourceResolver.findResources(query, SolrResourceProvider.QUERY_LANGUAGE_SOLR);

        List<JSONObject> jsonObjectList = new ArrayList<JSONObject>();
        while (resourceIterator != null && resourceIterator.hasNext()) {
            Resource next = resourceIterator.next();

            JSONObject jsonObject = new JSONObject(next.getValueMap());
            jsonObjectList.add(jsonObject);
        }

        response.getWriter().append(new JSONArray(jsonObjectList).toString());
    }

}
