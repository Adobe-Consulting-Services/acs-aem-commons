package com.adobe.acs.commons.users.impl;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.ConfigurationPolicy;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.PropertyOption;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.User;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.jackrabbit.oak.spi.security.authorization.accesscontrol.AccessControlConstants;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.ValueFormatException;
import javax.jcr.security.AccessControlPolicy;
import javax.jcr.security.Privilege;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component(
        label = "ACS AEM Commons - Ensure Service User",
        configurationFactory = true,
        metatype = true,
        immediate = true,
        policy = ConfigurationPolicy.REQUIRE
)
@Properties({
        @Property(
                name = "webconsole.configurationFactory.nameHint",
                value = "Ensure Service User: {operation} {principalName}"
        )
})
@Service(value = EnsureServiceUser.class)
public final class EnsureServiceUser {
    private static final Logger log = LoggerFactory.getLogger(EnsureServiceUser.class);


    private static final String SERVICE_NAME = "ensure-service-user";
    private static final Map<String, Object> AUTH_INFO;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private ServiceUser serviceUser = null;
    private Operation operation = null;

    public enum Operation {
        ADD,
        REMOVE
    }

    public static boolean DEFAULT_ENSURE_IMMEDIATELY = true;
    @Property(label = "Ensure immediately",
            boolValue = true,
            description = "Ensure on activation. When set to false, this must be ensured via the JMX MBean."
    )
    public static final String PROP_ENSURE_IMMEDIATELY = "ensure-immediately";


    public static final String DEFAULT_OPERATION = "add";
    @Property(label = "Operation",
            description = "Defines if the service user (principal name) should be adjusted to align with this config or removed completely",
            options = {
                    @PropertyOption(name = "add", value = "Ensure existence (add)"),
                    @PropertyOption(name = "remove", value = "Ensure extinction (remove)")
            }
    )
    public static final String PROP_OPERATION = "operation";

    @Property(label = "Principal Name",
            description = "The service user's principal name"
    )
    public static final String PROP_PRINCIPAL_NAME = "principalName";

    @Property(label = "ACEs",
            description = "This field is ignored if the Operation is set to 'Ensure extinction' (remove)",
            cardinality = Integer.MAX_VALUE
    )
    public static final String PROP_ACES = "aces";

    @Reference
    private ResourceResolverFactory resourceResolverFactory;

    @Reference
    private QueryBuilder queryBuilder;

    /**
     * @return the Service User this OSGi Config represents
     */
    public ServiceUser getServiceUser() {
        return serviceUser;
    }

    /**
     * @return the Operation this OSGi Config represents
     */
    public Operation getOperation() {
        return operation;
    }

    /**
     * Entry point for Ensuring a System User.
     *
     * @param operation   the ensure operation to execute (ADD or REMOVE)
     * @param serviceUser the service user configuration to ensure
     * @throws EnsureServiceUserException
     */
    public void ensure(Operation operation, ServiceUser serviceUser) throws EnsureServiceUserException {
        final long start = System.currentTimeMillis();

        ResourceResolver resourceResolver = null;

        try {
            resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);

            if (Operation.ADD.equals(operation)) {
                ensureExistance(resourceResolver, serviceUser);
            } else if (Operation.REMOVE.equals(operation)) {
                ensureRemoval(resourceResolver, serviceUser);
            } else {
                throw new EnsureServiceUserException("Unable to determine Ensure Service User operation Could not create or locate value system user (it is null).");
            }

            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
                log.debug("Persisted change to Service User [ {} ]", serviceUser.getPrincipalName());
            } else {
                log.debug("No changes required for Service User [ {} ]. Skipping...", serviceUser.getPrincipalName());
            }

            log.info("Successfully ensured [ {} ] of Service User [ {} ] in [ {} ms ]", new String[] { operation.toString(), getServiceUser().getPrincipalName(), String.valueOf(System.currentTimeMillis() - start) });
        } catch (Exception e) {
            throw new EnsureServiceUserException(String.format("Failed to ensure [ %s ] of Service User [ %s ]", operation.toString(), serviceUser.getPrincipalName()), e);
        } finally {
            if (resourceResolver != null) {
                resourceResolver.close();
            }
        }
    }

    /**
     * Ensures that the provided ServiceUser and configured ACEs exist. Any extra ACEs will be removed, and any missing ACEs added.
     *
     * @param resourceResolver the resource resolver to perform the user and ACE management
     * @param serviceUser      the service user to ensure
     * @throws RepositoryException
     * @throws EnsureServiceUserException
     */
    @SuppressWarnings("squid:S2583")
    protected void ensureExistance(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException, EnsureServiceUserException {
        final User systemUser = ensureSystemUser(resourceResolver, serviceUser);

        if (systemUser != null) {
            ensureAces(resourceResolver, systemUser, serviceUser);
        } else {
            log.error("Could not create or locate System User with principal name [ {} ]", serviceUser.getPrincipalName());
        }
    }

    /**
     * Ensures that the provided ServiceUser and any of its ACEs are removed.
     *
     * @param resourceResolver the resource resolver to perform the user and ACE management
     * @param serviceUser      the service user to ensure
     * @throws RepositoryException
     * @throws EnsureServiceUserException
     */
    private void ensureRemoval(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException, EnsureServiceUserException {
        final User systemUser = findSystemUser(resourceResolver, serviceUser.getPrincipalName());

        removeAces(resourceResolver, systemUser, serviceUser);

        if (systemUser != null) {
            systemUser.remove();
        }
    }

    /**
     * Ensures a System User exists with the principal name provided by the Service User configuration.
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param serviceUser      the service user to ensure
     * @return the System User; this should never return null
     * @throws RepositoryException
     * @throws EnsureServiceUserException
     */
    private User ensureSystemUser(ResourceResolver resourceResolver, ServiceUser serviceUser) throws RepositoryException, EnsureServiceUserException {
        User user = findSystemUser(resourceResolver, serviceUser.getPrincipalName());

        if (user == null) {
            final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

            // No principal found with this name; create the system user
            log.debug("Requesting creation of system user [ {} ] at [ {} ]", serviceUser.getPrincipalName(), serviceUser.getIntermediatePath());
            user = userManager.createSystemUser(serviceUser.getPrincipalName(), serviceUser.getIntermediatePath());
            log.debug("Created system user at [ {} ]", user.getPath());
        }

        return user;
    }

    /**
     * Ensures the ACEs for the Service User exists. Any extra ACEs for the Service User will be removed.
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param systemUser       the System User the Service User represents
     * @param serviceUser      the Service User
     * return                  # of ace entries that could not be processed
     * @throws RepositoryException
     */
    @SuppressWarnings("squid:S3776")
    private int ensureAces(ResourceResolver resourceResolver, User systemUser, ServiceUser serviceUser) throws RepositoryException {
        int failures = 0;
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager = (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls = findAcls(resourceResolver, serviceUser.getPrincipalName(), accessControlManager);

        // For each rep:policy (ACL) this service user participates in ...
        for (final JackrabbitAccessControlList acl : acls) {
            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();
            final boolean serviceUserCoversThisPath = serviceUser.hasAceAt(acl.getPath());

            for (final JackrabbitAccessControlEntry ace : aces) {

                if (!StringUtils.equals(serviceUser.getPrincipalName(), ace.getPrincipal().getName())) {
                    // Only care about ACEs that this service user participates in
                    continue;
                }

                // Pertains to this service user
                if (StringUtils.startsWith(acl.getPath(), systemUser.getPath())) {
                    // Skip the corner case of ACL's under the system user itself; Do nothing to these.

                } else if (!serviceUserCoversThisPath) {
                    // Remove all ACE's for this user from this ACL since this Service User is not configured to cover this path
                    log.debug("Service user does NOT cover the path yet has an ACE; ensure removal of the ace! {}", ace.toString());
                    acl.removeAccessControlEntry(ace);

                } else {
                    final Ace serviceUserAce = serviceUser.getAce(ace);
                    if (serviceUserAce == null) {
                        acl.removeAccessControlEntry(ace);
                        log.debug("Removed System ACE as it doesn't exist in Service User [ {} ] configuration", serviceUser.getPrincipalName());
                    } else {
                        serviceUserAce.setExists(true);
                        log.debug("No-op on System ACE as it already matches Service User [ {} ] configuration", serviceUser.getPrincipalName());

                    }
                }
            }

            accessControlManager.setPolicy(acl.getPath(), acl);
        }

        // Create an ACEs that do not yet exist
        for (Ace ace : serviceUser.getMissingAces()) {
            if (resourceResolver.getResource(ace.getContentPath()) == null) {
                log.warn("Unable to apply Service User [ {} ] privileges due to missing path at [ {} ]. Please create the path and re-ensure this service user.",
                        serviceUser.getPrincipalName(), ace.getContentPath());
                failures++;
                continue;
            }

            final JackrabbitAccessControlList acl = AccessControlUtils.getAccessControlList(session, ace.getContentPath());
            final Map<String, Value> restrictions = new HashMap<String, Value>();
            final Map<String, Value[]> multiRestrictions = new HashMap<String, Value[]>();

            final ValueFactory valueFactory = session.getValueFactory();

            // Add rep:glob restriction
            if (ace.hasRepGlob()) {
                restrictions.put(AccessControlConstants.REP_GLOB, valueFactory.createValue(ace.getRepGlob(), PropertyType.STRING));
            }

            // Add rep:ntNames restriction
            if (ace.hasRepNtNames()) {
                multiRestrictions.put(AccessControlConstants.REP_NT_NAMES, getMultiValues(valueFactory, ace.getRepNtNames(), PropertyType.NAME));
            }

            // Add rep:itemNames
            if (ace.hasRepItemNames()) {
                multiRestrictions.put(AccessControlConstants.REP_ITEM_NAMES, getMultiValues(valueFactory, ace.getRepItemNames(), PropertyType.NAME));
            }

            // Add rep:prefixes
            if (ace.hasRepPrefixes()) {
                multiRestrictions.put(AccessControlConstants.REP_PREFIXES, getMultiValues(valueFactory, ace.getRepPrefixes(), PropertyType.STRING));
            }

            // Add ACE to the ACL
            acl.addEntry(systemUser.getPrincipal(),
                    ace.getPrivileges(accessControlManager).toArray(new Privilege[]{}),
                    ace.isAllow(),
                    restrictions,
                    multiRestrictions);

            // Update the ACL on the content
            accessControlManager.setPolicy(ace.getContentPath(), acl);

            log.debug("Added Service User ACE for [ {} ] to [ {} ]", serviceUser.getPrincipalName(), ace.getContentPath());
        }

        return failures;
    }


    /**
     * Removes all ACEs for the Service User principal (except those that live beneath the System User's rep:User node)
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param systemUser       the System User the Service User represents
     * @param serviceUser      the Service User
     * @throws RepositoryException
     */
    private void removeAces(ResourceResolver resourceResolver, User systemUser, ServiceUser serviceUser) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager = (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls = findAcls(resourceResolver, serviceUser.getPrincipalName(), accessControlManager);

        for (final JackrabbitAccessControlList acl : acls) {
            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();

            // Check all the existing ACEs in the ACL
            for (JackrabbitAccessControlEntry ace : aces) {
                if (StringUtils.equals(serviceUser.getPrincipalName(), ace.getPrincipal().getName())) {
                    if (systemUser != null && StringUtils.startsWith(acl.getPath(), systemUser.getPath())) {
                        // Skip! Don't ensureRemoval ACE's from the system user itself!
                    } else {
                        acl.removeAccessControlEntry(ace);
                    }
                }
            }

            accessControlManager.setPolicy(acl.getPath(), acl);
            log.debug("Removed ACE from ACL at [ {} ] for [ {} ]", acl.getPath(), serviceUser.getPrincipalName());
        }
    }

    /**
     * Locates a System User by principal name, or null. Note, if a rep:User can be found but it is NOT a system user, this method will throw an exception.
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param principalName    the principal name
     * @return the System User or null
     * @throws RepositoryException
     * @throws EnsureServiceUserException
     */
    private User findSystemUser(ResourceResolver resourceResolver, String principalName) throws RepositoryException, EnsureServiceUserException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        User user = null;

        // Handle the actual user creation

        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable != null) {
            // Am authorizable was found with this name; check if this is a system user
            if (authorizable instanceof User) {
                user = (User) authorizable;
                if (!user.isSystemUser()) {
                    throw new EnsureServiceUserException(String.format("User [ %s ] ensureExistance at [ %s ] but is NOT a system user", principalName, user.getPath()));
                }
            } else {
                throw new EnsureServiceUserException(String.format("Authorizable [ %s ] at [ %s ] is not a user", principalName, authorizable.getPath()));
            }
        }

        return user;
    }

    /**
     * Locates by query all the ACLs that the principal participates in.
     *
     * @param resourceResolver     the resource resolver to perform the user management
     * @param principalName        the principal name
     * @param accessControlManager Jackrabbit access control manager
     * @return a list of ACLs that principal participates in.
     * @throws RepositoryException
     */
    private List<JackrabbitAccessControlList> findAcls(ResourceResolver resourceResolver, String principalName, JackrabbitAccessControlManager accessControlManager) throws RepositoryException {
        final Set<String> paths = new HashSet<String>();
        final List<JackrabbitAccessControlList> acls = new ArrayList<JackrabbitAccessControlList>();

        final Map<String, String> params = new HashMap<String, String>();

        params.put("type", AccessControlConstants.NT_REP_ACE);
        params.put("property", AccessControlConstants.REP_PRINCIPAL_NAME);
        params.put("property.value", principalName);
        params.put("p.limit", "-1");

        final Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
        final Iterator<Resource> resources = query.getResult().getResources();

        while (resources.hasNext()) {
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

    /**
     * Helper function that returns a list of Strings into an Array of Value's
     * @param valueFactory the valueFactory to create the Value
     * @param valueStrs the string values to convert
     * @return the array of Values derives from the valueStrs
     */
    private Value[] getMultiValues(ValueFactory valueFactory, List<String> valueStrs, int propertyType) throws ValueFormatException {
        final List<Value> result = new ArrayList<Value>();

        for (final String valueStr : valueStrs) {
            result.add(valueFactory.createValue(valueStr, propertyType));
        }

        return result.toArray(new Value[result.size()]);
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        boolean ensureImmediately = PropertiesUtil.toBoolean(config.get(PROP_ENSURE_IMMEDIATELY), DEFAULT_ENSURE_IMMEDIATELY);

        String operationStr = StringUtils.upperCase(PropertiesUtil.toString(config.get(PROP_OPERATION), DEFAULT_OPERATION));
        try {
            this.operation = Operation.valueOf(operationStr);
            // Parse OSGi Configuration into Service User object
            this.serviceUser = new ServiceUser(config);

            if (ensureImmediately) {
                // Ensure
                ensure(operation, getServiceUser());
            } else {
                log.info("This Service User is configured to NOT ensure immediately. Please ensure this Service User via the JMX MBean.");
            }


        } catch (EnsureServiceUserException e) {
            log.error("Unable to ensure Service User [ {} ]", PropertiesUtil.toString(config.get(PROP_PRINCIPAL_NAME), "Undefined Service User Principal Name"), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown Ensure Service User operation [ " +  operationStr + " ]", e);
        }
    }




}
