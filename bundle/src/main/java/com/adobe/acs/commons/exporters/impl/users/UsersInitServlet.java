package com.adobe.acs.commons.exporters.impl.users;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;
import java.io.IOException;
import java.util.Arrays;

import static com.adobe.acs.commons.exporters.impl.users.Constants.*;

@SlingServlet(
        label = "ACS AEM Commons - Users to CSV - Init Servlet",
        methods = {"GET"},
        resourceTypes = {"acs-commons/components/utilities/exporters/users-to-csv"},
        selectors = {"init"},
        extensions = {"json"}
)
public class UsersInitServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(UsersInitServlet.class);

    private static final String QUERY = "SELECT * FROM [rep:Group] WHERE ISDESCENDANTNODE([/home/groups]) ORDER BY [rep:principalName]";

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");

        JSONObject json = new JSONObject();
        JSONObject existing = new JSONObject();
        JSONObject options = new JSONObject();

        try {
            options.put(GROUPS, getGroupOptions(request.getResourceResolver()));
            options.put(GROUP_FILTERS, getGroupFilterOptions());

            final Parameters parameters = new Parameters(request.getResource());
            existing.put(GROUP_FILTER, parameters.getGroupFilter());
            existing.put(GROUPS, Arrays.asList(parameters.getGroups()));
            existing.put(CUSTOM_PROPERTIES, parameters.getCustomPropertiesAsJSON());

            json.put("options", options);
            json.put("form", existing);

        } catch (JSONException e) {
            throw new ServletException(e);
        } catch (RepositoryException e) {
            throw new ServletException(e);
        }

        response.getWriter().write(json.toString());
        response.getWriter().flush();
    }

    private JSONArray getGroupOptions(ResourceResolver resourceResolver) throws RepositoryException {
        final JSONArray jsonArray = new JSONArray();
        final QueryManager queryManager = resourceResolver.adaptTo(Session.class).getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(QUERY, Query.JCR_SQL2);
        final NodeIterator nodeIter = query.execute().getNodes();

        while (nodeIter.hasNext()) {
            Resource resource = resourceResolver.getResource(nodeIter.nextNode().getPath());
            jsonArray.put(resource.getValueMap().get("rep:principalName", "Unknown"));
        }

        return jsonArray;
    }

    private JSONArray getGroupFilterOptions() throws JSONException {
        JSONArray jsonArray = new JSONArray();

        JSONObject both = new JSONObject();
        both.put("text", "Direct or Indirect Membership");
        both.put("value", GROUP_FILTER_BOTH);

        JSONObject direct = new JSONObject();
        direct.put("text", "Direct Membership");
        direct.put("value", GROUP_FILTER_DIRECT);


        JSONObject indirect = new JSONObject();
        indirect.put("text", "Indirect Membership");
        indirect.put("value", GROUP_FILTER_INDIRECT);

        jsonArray.put(direct);
        jsonArray.put(indirect);
        jsonArray.put(both);

        return jsonArray;
    }
}
