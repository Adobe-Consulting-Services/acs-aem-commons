package com.adobe.acs.commons.exporters.impl.users;

import com.day.cq.commons.jcr.JcrConstants;
import com.day.text.csv.Csv;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.*;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.*;

import static com.adobe.acs.commons.exporters.impl.users.Constants.*;

@SlingServlet(
        label = "ACS AEM Commons - Users to CSV - Export Servlet",
        methods = {"GET"},
        resourceTypes = {"acs-commons/components/utilities/exporters/users-to-csv"},
        selectors = {"export"},
        extensions = {"csv"}
)
public class UsersExportServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(UsersExportServlet.class);

    private static final String QUERY = "SELECT * FROM [rep:User] WHERE ISDESCENDANTNODE([/home/users])";

    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/csv");

        final Parameters parameters;
        try {
            parameters = new Parameters(request);
        } catch (JSONException e) {
            throw new ServletException(e);
        }

        log.debug("Users to CSV Export Parameters: {}", parameters.toString());

        final Csv csv = new Csv();
        final Writer writer = response.getWriter();
        csv.writeInit(writer);

        final Iterator<Resource> resources = request.getResourceResolver().findResources(QUERY, Query.JCR_SQL2);

        List<CsvUser> csvUsers = new ArrayList<CsvUser>();

        while (resources.hasNext()) {
            try {
                Resource resource = resources.next();
                CsvUser csvUser = new CsvUser(resource);

                if (checkGroups(parameters.getGroups(), parameters.getGroupFilter(), csvUser)) {
                    csvUsers.add(csvUser);
                }

            } catch (RepositoryException e) {
                log.error("Unable to extract a user from [ {} ]", e);
            }
        }

        List<String> columns = new ArrayList<String>();
        columns.add("Path");
        columns.add("User ID");
        columns.add("First Name");
        columns.add("Last Name");
        columns.add("E-mail Address");
        columns.add("Created Date");
        columns.add("Last Modified Date");

        for (String customProperty : parameters.getCustomProperties()) {
            columns.add(customProperty);
        }

        columns.add("All Groups");
        columns.add("Direct Groups");
        columns.add("Indirect Groups");

        csv.writeRow(columns.toArray(new String[columns.size()]));

        for (final CsvUser csvUser : csvUsers) {
            List<String> values = new ArrayList<String>();
            try {
                values.add(csvUser.getPath());
                values.add(csvUser.getID());
                values.add(csvUser.getFirstName());
                values.add(csvUser.getLastName());
                values.add(csvUser.getEmail());
                values.add(csvUser.getCreatedDate());
                values.add(csvUser.getLastModifiedDate());

                for (String customProperty : parameters.getCustomProperties()) {
                    values.add(csvUser.getCustomProperty(customProperty));
                }

                values.add(StringUtils.join(csvUser.getAllGroups(), "|"));
                values.add(StringUtils.join(csvUser.getDeclaredGroups(), "|"));
                values.add(StringUtils.join(csvUser.getTransitiveGroups(), "|"));

                csv.writeRow(values.toArray(new String[values.size()]));
                log.info("{}", values);
            } catch (RepositoryException e) {
                log.error("Unable to export user to CSV report", e);
            }
        }

        csv.close();
    }

    private boolean checkGroups(String[] groups, String groupFilter, CsvUser csvUser) {
        if (groups != null && groups.length > 0) {
            if (GROUP_FILTER_DIRECT.equals(groupFilter) && csvUser.isInDirectGroup(groups)) {
                return true;
            } else if (GROUP_FILTER_INDIRECT.equals(groupFilter) && csvUser.isInIndirectGroup(groups)) {
                return true;
            } else if (csvUser.isInDirectGroup(groups) || csvUser.isInIndirectGroup(groups)) {
                return true;
            }

            return false;
        }
        return true;
    }

    private class CsvUser {
        private final ValueMap properties;
        private List<String> declaredGroups = new ArrayList<String>();
        private List<String> transitiveGroups = new ArrayList<String>();
        private List<String> allGroups = new ArrayList<String>();
        private Authorizable authorizable;
        private String email;
        private String firstName;
        private String lastName;
        private Calendar createdDate;
        private Calendar lastModifiedDate;
        private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");

        public CsvUser(Resource resource) throws RepositoryException {
            if (resource == null) {
                throw new IllegalArgumentException("Authorizable object cannot be null");
            }

            final UserManager userManager = resource.adaptTo(UserManager.class);
            this.properties = resource.getValueMap();

            this.authorizable = userManager.getAuthorizableByPath(resource.getPath());
            this.declaredGroups = getGroupIds(authorizable.declaredMemberOf());
            this.transitiveGroups = getGroupIds(authorizable.memberOf());
            this.allGroups.addAll(this.transitiveGroups);
            this.transitiveGroups.removeAll(this.declaredGroups);

            this.firstName = properties.get("profile/givenName", "");
            this.lastName = properties.get("profile/familyName", "");
            this.email = properties.get("profile/email", "");
            this.createdDate = properties.get(JcrConstants.JCR_CREATED, Calendar.class);
            this.lastModifiedDate = properties.get("cq:lastModified", Calendar.class);
        }

        public List<String> getDeclaredGroups() {
            return declaredGroups;
        }

        public List<String> getTransitiveGroups() {
            return transitiveGroups;
        }

        public List<String> getAllGroups() {
            return allGroups;
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

            Collections.sort(groupIDs);

            return groupIDs;
        }

        public String getFirstName() {
            return firstName;
        }

        public String getLastName() {
            return lastName;
        }

        public String getCreatedDate() {
            if (createdDate != null) {
                return sdf.format(createdDate.getTime());
            } else {
                return "";
            }
        }

        public String getLastModifiedDate() {
            if (lastModifiedDate != null) {
                return sdf.format(lastModifiedDate.getTime());
            } else {
                return "";
            }
        }

        public String getEmail() {
            return email;
        }

        public boolean isInDirectGroup(String... groups) {
            return CollectionUtils.containsAny(this.getDeclaredGroups(), Arrays.asList(groups));
        }

        public boolean isInIndirectGroup(String... groups) {
            return CollectionUtils.containsAny(this.getTransitiveGroups(), Arrays.asList(groups));
        }

        public String getCustomProperty(String customProperty) {
            return properties.get(customProperty, "");
        }
    }
}
