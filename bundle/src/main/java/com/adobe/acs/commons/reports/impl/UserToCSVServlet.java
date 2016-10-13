package com.adobe.acs.commons.reports.impl;

import com.day.text.csv.Csv;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;


@SlingServlet(
        label = "ACS AEM Commons - Users to CSV Export",
        description = "Sample implementation of a Sling All Methods Servlet.",
        methods = { "GET" },
        resourceTypes = { "acs-commons/export/csv-to-user "},
        selectors = { "export" },
        extensions = { "csv" }
)
public class UserToCSVServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(UserToCSVServlet.class);

    private static final String QUERY = "SELECT * FROM [rep:User] WHERE ISDESCENDANTNODE([/home/users])";

    @Reference
    private UserManager userManager;

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException {
        response.setContentType("text/csv");

        final Csv csv = new Csv();
        csv.writeInit(response.getWriter());

        final Iterator<Resource> resources = request.getResourceResolver().findResources(QUERY, Query.JCR_SQL2);

        int maxDeclaredGroups = 0;
        int maxGroups = 0;
        List<CsvUser> csvUsers = new ArrayList<CsvUser>();

        while (resources.hasNext()) {
            CsvUser csvUser = null;
            try {
                csvUser = new CsvUser(userManager.getAuthorizableByPath(resources.next().getPath()));

                if (csvUser.getDeclaredGroups().size() > maxDeclaredGroups) {
                    maxDeclaredGroups = csvUser.getDeclaredGroups().size();
                }

                if (csvUser.getGroups().size() > maxGroups) {
                    maxGroups = csvUser.getGroups().size();
                }

                csvUsers.add(csvUser);
            } catch (RepositoryException e) {
                log.error("", e);
            }
        }

        for (final CsvUser csvUser : csvUsers) {
            List<String> values = new ArrayList<String>();
            try {
                values.add(csvUser.getID());
                values.add(csvUser.getPath());

                values.add("Declared Member of -->");
                values.addAll(csvUser.getDeclaredGroups());
                values.addAll(Collections.nCopies((maxDeclaredGroups - csvUser.getDeclaredGroups().size()), ""));

                values.add("Transitive Member of -->");
                values.addAll(csvUser.getGroups());
                values.addAll(Collections.nCopies((maxGroups - csvUser.getGroups().size()), ""));

                csv.writeRow(values.toArray(new String[]{}));
            } catch (RepositoryException e) {
                e.printStackTrace();
            }
        }

        response.getWriter().flush();
    }


    private class CsvUser {
        final List<String> declaredGroups;
        final List<String> groups;
        final Authorizable authorizable;

        public CsvUser(Authorizable authorizable) throws RepositoryException {
            if (authorizable == null) {
                throw new IllegalArgumentException("Authorizable object cannot be null");
            }
            this.authorizable = authorizable;
            this.declaredGroups = getGroupIds(authorizable.declaredMemberOf());
            this.groups = getGroupIds(authorizable.memberOf());

        }

        public List<String> getDeclaredGroups() {
            return declaredGroups;
        }

        public List<String> getGroups() {
            return groups;
        }

        public String getPath() throws RepositoryException {
            return authorizable.getPath();
        }

        public String getID() throws RepositoryException {
            return authorizable.getID();
        }

        private List<String> getGroupIds(Iterator<Group> groups) throws RepositoryException {
            final List<String> groupIDs = new ArrayList<String>();

            while (groups.hasNext()) {
                groupIDs.add(groups.next().getID());
            }

            return groupIDs;
        }
    }
}
