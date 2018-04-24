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

import com.day.cq.commons.jcr.JcrConstants;
import com.day.text.csv.Csv;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.Group;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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

    private static final String QUERY = "SELECT * FROM [rep:User] WHERE ISDESCENDANTNODE([/home/users]) ORDER BY [rep:principalName]";
    private static final String GROUP_DELIMITER = "|";

    /**
     * Generates a CSV file representing the User Data.
     *
     * @param request  the Sling HTTP Request object
     * @param response the Sling HTTP Response object
     * @throws IOException
     * @throws ServletException
     */
    public void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws IOException, ServletException {
        response.setContentType("text/csv");
        response.setCharacterEncoding("UTF-8");

        final Parameters parameters = new Parameters(request);

        log.debug("Users to CSV Export Parameters: {}", parameters.toString());

        final Csv csv = new Csv();
        final Writer writer = response.getWriter();
        csv.writeInit(writer);

        final Iterator<Resource> resources = request.getResourceResolver().findResources(QUERY, Query.JCR_SQL2);

        // Using a HashMap to satisfy issue with duplicate results in AEM 6.1 GA
        HashMap<String, CsvUser> csvUsers = new LinkedHashMap<String, CsvUser>();

        while (resources.hasNext()) {
            try {
                Resource resource = resources.next();
                CsvUser csvUser = new CsvUser(resource);

                if (!csvUsers.containsKey(csvUser.getPath())
                        && checkGroups(parameters.getGroups(), parameters.getGroupFilter(), csvUser)) {
                    csvUsers.put(csvUser.getPath(), csvUser);
                }

            } catch (RepositoryException e) {
                log.error("Unable to extract a user from resource.", e);
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

        for (final CsvUser csvUser : csvUsers.values()) {
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

                values.add(StringUtils.join(csvUser.getAllGroups(), GROUP_DELIMITER));
                values.add(StringUtils.join(csvUser.getDeclaredGroups(), GROUP_DELIMITER));
                values.add(StringUtils.join(csvUser.getTransitiveGroups(), GROUP_DELIMITER));

                csv.writeRow(values.toArray(new String[values.size()]));
            } catch (RepositoryException e) {
                log.error("Unable to export user to CSV report", e);
            }
        }

        csv.close();
    }

    /**
     * Determines if the user should be included based on the specified group filter type, and requested groups.
     *
     * @param groups      the groups
     * @param groupFilter the groupFilter
     * @param csvUser     the user
     * @return true if the user should be included.
     */
    protected boolean checkGroups(String[] groups, String groupFilter, CsvUser csvUser) throws RepositoryException {
        log.debug("Group Filter: {}", groupFilter);
        if (!ArrayUtils.isEmpty(groups)) {
            if (GROUP_FILTER_DIRECT.equals(groupFilter) && csvUser.isInDirectGroup(groups)) {
                log.debug("Adding [ {} ] via [ Direct ] membership", csvUser.getID());
                return true;
            } else if (GROUP_FILTER_INDIRECT.equals(groupFilter) && csvUser.isInIndirectGroup(groups)) {
                log.debug("Adding [ {} ] via [ Indirect ] membership", csvUser.getID());
                return true;
            } else if (GROUP_FILTER_BOTH.equals(groupFilter)
                    && (csvUser.isInDirectGroup(groups) || csvUser.isInIndirectGroup(groups))) {
                log.debug("Adding [ {} ] via [ Direct OR Indirect ] membership", csvUser.getID());
                return true;
            }

            return false;
        }

        log.debug("Adding [ {} ] as no groups were specified to specify membership filtering.", csvUser.getID());
        return true;
    }

    /**
     * Internal class representing a user that will be exported in CSV format.
     */
    protected static class CsvUser {
        private final ValueMap properties;
        private Set<String> declaredGroups = new LinkedHashSet<String>();
        private Set<String> transitiveGroups = new LinkedHashSet<String>();
        private Set<String> allGroups = new LinkedHashSet<String>();
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
            this.allGroups.addAll(this.declaredGroups);

            this.transitiveGroups.removeAll(this.declaredGroups);

            this.firstName = properties.get("profile/givenName", "");
            this.lastName = properties.get("profile/familyName", "");
            this.email = properties.get("profile/email", "");
            this.createdDate = properties.get(JcrConstants.JCR_CREATED, Calendar.class);
            this.lastModifiedDate = properties.get("cq:lastModified", Calendar.class);
        }

        public List<String> getDeclaredGroups() {
            return new ArrayList<String>(declaredGroups);
        }

        public List<String> getTransitiveGroups() {
            return new ArrayList<String>(transitiveGroups);
        }

        public List<String> getAllGroups() {
            return new ArrayList<String>(allGroups);
        }

        public String getPath() throws RepositoryException {
            return authorizable.getPath();
        }

        @SuppressWarnings("checkstyle:abbreviationaswordinname")
        public String getID() throws RepositoryException {
            return authorizable.getID();
        }

        private Set<String> getGroupIds(Iterator<Group> groups) throws RepositoryException {
            final List<String> groupIDs = new ArrayList<String>();

            while (groups.hasNext()) {
                groupIDs.add(groups.next().getID());
            }

            Collections.sort(groupIDs);

            return new LinkedHashSet<String>(groupIDs);
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
