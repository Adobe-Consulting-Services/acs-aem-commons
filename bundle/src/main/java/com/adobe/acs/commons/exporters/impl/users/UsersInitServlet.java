/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2016 Adobe
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
package com.adobe.acs.commons.exporters.impl.users;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

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
    private static final String QUERY = "SELECT * FROM [rep:Group] WHERE ISDESCENDANTNODE([/home/groups]) ORDER BY [rep:principalName]";
    private static final String KEY_TEXT = "text";
    private static final String KEY_VALUE = "value";

    /**
     * Returns a JSON containing the options available to the form, and any prior saved data that should pre-populate the form.
     * @param request the Sling HTTP Request object
     * @param response the Sling HTTP Response object
     * @throws IOException
     * @throws ServletException
     */
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        final JsonObject json = new JsonObject();
        final JsonObject existing = new JsonObject();
        final JsonObject options = new JsonObject();

        try {
            options.add(GROUPS, getGroupOptions(request.getResourceResolver()));
            options.add(GROUP_FILTERS, getGroupFilterOptions());

            final Parameters parameters = new Parameters(request.getResource());
            existing.addProperty(GROUP_FILTER, parameters.getGroupFilter());
            existing.add(GROUPS, new Gson().toJsonTree(Arrays.asList(parameters.getGroups())));
            existing.add(CUSTOM_PROPERTIES, parameters.getCustomPropertiesAsJSON());

            json.add("options", options);
            json.add("form", existing);

        } catch (RepositoryException e) {
            throw new ServletException(e);
        }

        response.getWriter().write(json.toString());
        response.getWriter().flush();
    }

    /**
     * Creates a JSON array of the User Groups principals in the system.
     * @param resourceResolver the security context used to collect the groups.
     * @return a JSON Array of all the user group principals name that the resourceResolver can read.
     * @throws RepositoryException
     */
    private JsonArray getGroupOptions(ResourceResolver resourceResolver) throws RepositoryException {
        final JsonArray jsonArray = new JsonArray();
        final QueryManager queryManager = resourceResolver.adaptTo(Session.class).getWorkspace().getQueryManager();
        final Query query = queryManager.createQuery(QUERY, Query.JCR_SQL2);
        final NodeIterator nodeIter = query.execute().getNodes();

        while (nodeIter.hasNext()) {
            Resource resource = resourceResolver.getResource(nodeIter.nextNode().getPath());
            jsonArray.add(new JsonPrimitive(resource.getValueMap().get("rep:principalName", "Unknown")));
        }

        return jsonArray;
    }

    /**
     * Creates a list of options for the Group Filter list.
     * @return a JSON Array of the available group filter options.
     * @throws JSONException
     */
    private JsonArray getGroupFilterOptions() {
        JsonObject both = new JsonObject();
        both.addProperty(KEY_TEXT, "Direct or Indirect Membership");
        both.addProperty(KEY_VALUE, GROUP_FILTER_BOTH);

        JsonObject direct = new JsonObject();
        direct.addProperty(KEY_TEXT, "Direct Membership");
        direct.addProperty(KEY_VALUE, GROUP_FILTER_DIRECT);


        JsonObject indirect = new JsonObject();
        indirect.addProperty(KEY_TEXT, "Indirect Membership");
        indirect.addProperty(KEY_VALUE, GROUP_FILTER_INDIRECT);

        JsonArray jsonArray = new JsonArray();
        jsonArray.add(direct);
        jsonArray.add(indirect);
        jsonArray.add(both);

        return jsonArray;
    }
}
