package com.adobe.acs.commons.users.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.*;
import org.apache.felix.scr.annotations.Property;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.resource.*;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.*;
import javax.jcr.security.AccessControlManager;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.util.*;

@Component(
        label = "ACS AEM Commons - Ensure Service User",
        configurationFactory = true,
        metatype = true,
        immediate = true
)
@Service(value = EnsureServiceUser.class)
public class EnsureServiceUser {
    private static final Logger log = LoggerFactory.getLogger(EnsureServiceUser.class);

    public static final String DEFAULT_OPERATION = "add";
    @Property(label = "Operation", options = {
            @PropertyOption(name = "add", value = "Ensure existence"),
            @PropertyOption(name = "remove", value = "Ensure extinction")
    })
    public static final String PROP_OPERATION = "operation";


    @Property(label = "Principal Name")
    public static final String PROP_PRINCIPAL = "principal";

    @Property(label = "ACEs", cardinality = Integer.MAX_VALUE)
    public static final String PROP_ACES = "aces";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    protected void ensure(ServiceUser user) throws RepositoryException {

        ResourceResolver resourceResolver = null;

        try {

            resourceResolver = resourceResolverFactory.getAdministrativeResourceResolver(null);

            if (user.isRemovalOperation()) {
                log.info("Removing service user [ {} ]", user.getPrincipalName());
                remove(resourceResolver, user);
            } else {
                log.info("Ensuring existance of properly configured service user [ {} ]", user.getPrincipalName());
                exists(resourceResolver, user);
            }

            if (resourceResolver.hasChanges()) {
                try {
                    resourceResolver.commit();
                    log.info("Saved all changes for managing service user [ {} ]", user.getPrincipalName());
                } catch (PersistenceException e) {
                    log.error("Could not save changed to ensuring service user [ {} ]", user.getPrincipalName());
                }
            } else {
                log.debug("No changes were made when managing service user [ {} ]", user.getPrincipalName());
            }
        } catch (org.apache.sling.api.resource.LoginException e) {
            log.error("Could not login to repository to ensure service users", e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    private void remove(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException {

        User systemUser = ensureSystemUser(resourceResolver, serviceUser);
        if (systemUser != null) {
            log.info("Removing ACEs for [ {} ]", serviceUser.getPrincipalName());
            removeACEs(resourceResolver, systemUser, serviceUser);
            log.info("Removing system user [ {} ]", systemUser.getPath());
            systemUser.remove();
        } else {
            log.error("Could not create or locate value system user. [ {} ]", serviceUser.getPrincipalName());
        }

    }

    protected void exists(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException {
        // Handle the ACE's
        log.debug("Ensuring system user [ {}  ]", serviceUser.getPrincipalName());
        User systemUser = ensureSystemUser(resourceResolver, serviceUser);
        if (systemUser != null) {
            log.debug("Ensuring ACEs for system user [ {} ]", serviceUser.getPrincipalName());
            ensureACEs(resourceResolver, systemUser, serviceUser);
        } else {
            log.error("Could not create or locate value system user. [ {} ]", serviceUser.getPrincipalName());
        }
    }


    private User ensureSystemUser(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        User user = null;

        // Handle the actual user creation

        Authorizable authorizable = userManager.getAuthorizable(serviceUser.getPrincipalName());

        if (authorizable == null) {
            // No principal found with this name; create the system user
            user = userManager.createSystemUser(serviceUser.getPrincipalName(), serviceUser.getIntermediatePath());
            log.info("Created system user at [ {} ]", user.getPath());
        } else {
            // Am authorizable was found with this name; check if this is a system user
            if (authorizable instanceof User) {
                user = (User) authorizable;
                if (user.isSystemUser()) {
                    log.info("System user [ {} ] exists at [ {} ]", user.getPrincipal().getName(), user.getPath());
                } else {
                    log.warn("User [ {} ] exists at [ {} ] but is NOT a system user", user.getPrincipal().getName(), user.getPath());
                }
            } else {
                log.error("Authorizable [ {} ] at [ {} ] is not a user", authorizable.getPrincipal().getName(), authorizable.getPath());
            }
        }

        return user;
    }

    private void ensureACEs(ResourceResolver resourceResolver, User systemUser, ServiceUser serviceUser) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager = (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls = findACLs(resourceResolver, serviceUser.getPrincipalName(), accessControlManager);

        for (JackrabbitAccessControlList acl : acls) {
            log.debug("ACL Path: {}", acl.getPath());

            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();
            final boolean serviceUserCoversThisPath = serviceUser.hasAceAt(acl.getPath());

            log.debug("Aces: {}", aces.length);

            // Check all the existing ACEs in the ACL
            for (JackrabbitAccessControlEntry ace : aces) {
                log.debug("ace: {}", ace.toString());

                if (StringUtils.equals(serviceUser.getPrincipalName(), ace.getPrincipal().getName())) {
                    // Pertains to this service user
                    if (StringUtils.startsWith(acl.getPath(), systemUser.getPath())) {
                        // Skip the corner case of ACL's under the system user itself; Do nothing to these.
                    } else if (!serviceUserCoversThisPath) {
                        // Remove all ACE's for this user from this ACL since this Service User is not configured to cover this path
                        log.debug("Service user does NOT cover the path yet has an ACE; remove the ace! {}", ace.toString());
                        acl.removeAccessControlEntry(ace);
                    } else {
                        Ace serviceUserAce = serviceUser.getAce(ace);
                        if (serviceUserAce == null) {
                            // Remove this ace if there isn't a configuration for it.
                            acl.removeAccessControlEntry(ace);
                            log.debug("ACE doesnt exist for service user def so remove! {}", ace.toString());

                        } else {
                            serviceUserAce.setExists(true);
                            log.debug("ACE is already defined! {}", ace.toString());

                        }
                    }
                }
            }

            accessControlManager.setPolicy(acl.getPath(), acl);
        }

        // Add any configured ACE's that don't already exist
        for (Ace ace : serviceUser.getMissingAces()) {

            final JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(session, ace.getPath());
            final Map<String, Value> restrictions = new HashMap<String, Value>();
            final ValueFactory valueFactory = session.getValueFactory();

            //restrictions.put(AccessControlConstants.REP_NODE_PATH, valueFactory.createValue(ace.getPath(), PropertyType.PATH));

            if (ace.hasRepGlob()) {
                restrictions.put(AccessControlConstants.REP_GLOB, valueFactory.createValue(ace.getRepGlob()));
            }

            if (ace.hasRepNtNames()) {
                restrictions.put(AccessControlConstants.REP_NT_NAMES, valueFactory.createValue(ace.getRepNtNames()));
            }

            log.info("Adding ACE @ [ {} ] for [ {} ]", ace.getPath(), systemUser.getPrincipal().getName());
            acl.addEntry(systemUser.getPrincipal(),
                    ace.getPrivileges(accessControlManager).toArray(new Privilege[]{}),
                    ace.isAllow(),
                    restrictions);

            log.debug("Added the ACL to [ {} ]", ace.getPath());
            accessControlManager.setPolicy(ace.getPath(), acl);
        }
    }


    private void removeACEs(ResourceResolver resourceResolver, User systemUser, ServiceUser serviceUser) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager = (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls = findACLs(resourceResolver, serviceUser.getPrincipalName(), accessControlManager);

        for (JackrabbitAccessControlList acl : acls) {
            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();

            log.debug("Processing ACE removal from ACL at [ {} ] for [ {} ]", acl.getPath(), serviceUser.getPrincipalName());

            // Check all the existing ACEs in the ACL
            for (JackrabbitAccessControlEntry ace : aces) {
                if (StringUtils.equals(serviceUser.getPrincipalName(), ace.getPrincipal().getName())) {
                    if (StringUtils.startsWith(acl.getPath(), systemUser.getPath())) {
                        // Skip! Don't remove ACE's from the system user itself!
                    } else {
                        acl.removeAccessControlEntry(ace);
                    }
                }
            }

            log.debug("Removing ACE from [ {} ]", acl.getPath());
            accessControlManager.setPolicy(acl.getPath(), acl);
        }
    }


    private List<JackrabbitAccessControlList> findACLs(ResourceResolver resourceResolver, String principalName, JackrabbitAccessControlManager accessControlManager) throws RepositoryException {
        final Set<String> paths = new HashSet<String>();
        final List<JackrabbitAccessControlList> acls = new ArrayList<JackrabbitAccessControlList>();

        final Map<String, String> params = new HashMap<String, String>();

        params.put("type", AccessControlConstants.NT_REP_ACE);
        params.put("property", AccessControlConstants.REP_PRINCIPAL_NAME);
        params.put("property.value", principalName);
        params.put("p.limit", "-1");

        Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));

        Iterator<Resource> resources = query.getResult().getResources();

        while(resources.hasNext()) {
            // Get the content resource as this is what the AccessControlManager API will use to find the ACLs under it
            Resource contentResource = resources.next().getParent().getParent();

            if (!paths.contains(contentResource.getPath())) {
                for (AccessControlPolicy policy : accessControlManager.getPolicies(contentResource.getPath())) {
                    if (policy instanceof JackrabbitAccessControlList) {
                        acls.add((JackrabbitAccessControlList) policy);
                        break;
                    }
                }
            }
        }

        return acls;
    }


    @Activate
    protected void activate(Map<String, Object> config) {
        ServiceUser serviceUser = new ServiceUser(config);

        try {
            ensure(serviceUser);
        } catch (RepositoryException e) {
            log.error("Massive failure when ensuring service user [ {} ]", serviceUser.getPrincipalName(), e);
        }
    }


    protected static class ServiceUser {
        private final String principalName;
        private final String intermediatePath;
        private final String operation;
        private final List<Ace> aces = new ArrayList<Ace>();

        public ServiceUser(Map<String, Object> config) {
            String tmp = PropertiesUtil.toString(config.get(PROP_PRINCIPAL), null);

            if (StringUtils.contains(tmp, "/")) {
                this.principalName = StringUtils.substringAfterLast(tmp, "/");
                this.intermediatePath = StringUtils.substringBeforeLast(tmp, "/");
            } else {
                this.principalName = tmp;
                this.intermediatePath = "/home/users/system";
            }

            this.operation = PropertiesUtil.toString(config.get(PROP_OPERATION), DEFAULT_OPERATION);

            for (String entry : PropertiesUtil.toStringArray(config.get(PROP_ACES), new String[0])) {
                getAces().add(new Ace(entry));
            }
        }

        public boolean isRemovalOperation() {
            return StringUtils.equals(operation, "remove");
        }

        public boolean hasAceAt(String path) {
            for (Ace ace : getAces()) {
                if (StringUtils.equals(path, ace.getPath())) {
                    return true;
                }
            }

            return false;
        }

        public String getIntermediatePath() {
            return intermediatePath;
        }

        public String getPrincipalName() {
            return principalName;
        }

        public List<Ace> getAces() {
            return aces;
        }

        public Ace getAce(JackrabbitAccessControlEntry actual) throws RepositoryException {
            for (Ace ace : getAces()) {
                if (ace.isSameAs(actual)) {
                    return ace;
                }
            }

            return null;
        }

        public List<Ace> getMissingAces() {
            final List<Ace> result = new ArrayList<Ace>();
            for (final Ace ace : getAces()) {
                if (!ace.isExists()) {
                    result.add(ace);
                }
            }

            return result;
        }
    }

    // type=allow;privileges=jcr:read,rep:write;path=/content/foo;rep:glob=/jcr:content/*

    protected static class Ace {

        private static final String PARAM_DELIMITER = ";";
        private static final String KEY_VALUE_SEPARATOR = "=";
        private static final String LIST_SEPARATOR = ",";

        public static final String TYPE = "type";
        public static final String PATH = "path";
        public static final String PRIVILEGES = "privileges";
        public static final String REP_GLOB = AccessControlConstants.REP_GLOB;
        public static final String REP_NT_NAMES = AccessControlConstants.REP_NT_NAMES;


        private String type;
        private String path;
        private String repGlob;
        private String repNtNames;
        private final List<String> permissions = new ArrayList<String>();
        private boolean exists = false;

        public Ace(String raw) {
            String[] segments = StringUtils.split(raw, PARAM_DELIMITER);

            for (String segment : segments) {
                AbstractMap.SimpleEntry<String, String> entry = ParameterUtil.toSimpleEntry(segment, KEY_VALUE_SEPARATOR);

                if (StringUtils.equals(TYPE, entry.getKey())) {
                    this.type = StringUtils.stripToNull(entry.getValue());
                } else if (StringUtils.equals(PATH, entry.getKey())) {
                    this.path = StringUtils.stripToNull(entry.getValue());
                } else if (StringUtils.equals(REP_GLOB, entry.getKey())) {
                    this.repGlob = StringUtils.stripToNull(entry.getValue());
                } else if (StringUtils.equals(REP_NT_NAMES, entry.getKey())) {
                    this.repNtNames = StringUtils.stripToNull(entry.getValue());
                } else if (StringUtils.equals(PRIVILEGES, entry.getKey())) {
                    String[] permissionList = StringUtils.split(entry.getValue(), LIST_SEPARATOR);
                    for (String permission : permissionList) {
                        permission = StringUtils.stripToNull(permission);
                        if (permission != null) {
                            getPrivilegeNames().add(permission);
                        }
                    }

                }
            }
        }

        public boolean isAllow() {
            return StringUtils.equals("allow", this.type);
        }

        public String getPath() {
            return path;
        }

        public String getRepGlob() {
            return repGlob;
        }

        public List<String> getPrivilegeNames() {
            return permissions;
        }

        public List<Privilege> getPrivileges(AccessControlManager accessControlManager) {
            final List<Privilege> privileges = new ArrayList<Privilege>();

            for (String privilegeName : getPrivilegeNames()) {
                try {
                    privileges.add(accessControlManager.privilegeFromName(privilegeName));
                } catch (RepositoryException e) {
                    log.error("Unable to convert provided privilege name [ {} ] to a JCR Privilege. Skipping...", privilegeName);
                }
            }

            return privileges;
        }


        public void setExists(boolean exists) {
            this.exists = exists;
        }

        public boolean isExists() {
            return exists;
        }

        public boolean hasRepGlob() {
            return StringUtils.isNotBlank(getRepGlob());
        }

        public boolean hasRepNtNames() {
            return StringUtils.isNotBlank(getRepNtNames());
        }

        public String getRepNtNames() {
            return repNtNames;
        }

        public boolean isSameAs(JackrabbitAccessControlEntry actual) throws RepositoryException {
            // Allow vs Deny
            if (actual.isAllow() != this.isAllow()) {
                return false;
            }

            // Privileges
            final List<String> actualPrivileges = Arrays.asList(AccessControlUtils.namesFromPrivileges(actual.getPrivileges()));
            if (!CollectionUtils.isEqualCollection(actualPrivileges, getPrivilegeNames())) {
                return false;
            }

            // rep:glob
            if (this.hasRepGlob() != ArrayUtils.contains(actual.getRestrictionNames(), AccessControlConstants.REP_GLOB)) {
                // configuration has rep:glob, but the actual does not
                return false;
            } else {
                Value actualRepGlob = actual.getRestriction(AccessControlConstants.REP_GLOB);
                if (!StringUtils.equals(actualRepGlob.toString(), this.getRepGlob())) {
                    return false;
                }
            }

            // rep:ntNames
            if (this.hasRepNtNames() != ArrayUtils.contains(actual.getRestrictionNames(), AccessControlConstants.REP_NT_NAMES)) {
                // configuration has rep:ntNames, but the actual does not
                return false;
            } else {
                Value actualRepNtNames = actual.getRestriction(AccessControlConstants.REP_NT_NAMES);
                if (!StringUtils.equals(actualRepNtNames.toString(), this.getRepNtNames())) {
                    return false;
                }
            }

            return true;
        }
    }

}
