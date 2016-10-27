package com.adobe.acs.commons.exporters.impl.users;

import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.ModifiableValueMap;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;

import static com.adobe.acs.commons.exporters.impl.users.Constants.*;


@SlingServlet(
        label = "ACS AEM Commons - Users to CSV - Save Servlet",
        methods = {"POST"},
        resourceTypes = {"acs-commons/components/utilities/exporters/users-to-csv"},
        selectors = {"save"},
        extensions = {"json"}
)
public class UsersSaveServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(UsersSaveServlet.class);

    public void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("application/json");

        final ValueMap properties = request.getResource().adaptTo(ModifiableValueMap.class);
        try {
            final Parameters parameters = new Parameters(request);
            properties.put(GROUP_FILTER, parameters.getGroupFilter());
            properties.put(GROUPS, parameters.getGroups());
            properties.put(CUSTOM_PROPERTIES, parameters.getCustomProperties());
            request.getResourceResolver().commit();
        } catch (JSONException e) {
            log.error("Could not save Users to CSV configuration", e);
            throw new ServletException(e);
        }
    }
}