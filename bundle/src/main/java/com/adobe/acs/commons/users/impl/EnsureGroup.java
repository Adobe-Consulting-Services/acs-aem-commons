/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.users.impl;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;

import org.apache.commons.lang.StringUtils;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.oak.spi.security.principal.PrincipalImpl;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.osgi.service.metatype.annotations.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(
        service = { EnsureGroup.class, EnsureAuthorizable.class },
        immediate = true,
        configurationPolicy = ConfigurationPolicy.REQUIRE,
        property = {
                "webconsole.configurationFactory.nameHint=Ensure Group: {operation} {principalName}"
        }
)
@Designate(
        ocd = EnsureGroup.Config.class,
        factory = true
)
public final class EnsureGroup implements EnsureAuthorizable {

    @ObjectClassDefinition(
            name = "ACS AEM Commons - Ensure Group"
    )
    public @interface Config {
        @AttributeDefinition(
                name = "Ensure immediately",
                defaultValue = "true",
                description = "Ensure on activation. When set to false, this must be ensured via the JMX MBean."
        )
        String ensure$_$immediately();

        @AttributeDefinition(
                name = "Operation",
                description = "Defines if the group (principal name) should be adjusted to align with this config or removed completely",
                options = {
                        @Option(value = "add", label = "Ensure existence (add)"),
                        @Option(value = "remove", label = "Ensure extinction (remove)")
                }
        )
        String operation();

        @AttributeDefinition(
                name = "Principal Name",
                description = "The group's principal name"
        )
        String principalName();

        @AttributeDefinition(name = "ACEs",
                description = "This field is ignored if the Operation is set to 'Ensure extinction' (remove)",
                cardinality = Integer.MAX_VALUE)
        String[] aces();

        @AttributeDefinition(
                name = "Member Of",
                description = "Defines groups this group must be a member of.  Group will be removed from any groups not listed.",
                cardinality = Integer.MAX_VALUE)
        String[] member$_$of();

    }

    public static final String PROP_ENSURE_IMMEDIATELY = "ensure-immediately";

    public static final String DEFAULT_OPERATION = "add";
    public static final String PROP_OPERATION = "operation";

    public static final String PROP_PRINCIPAL_NAME = "principalName";

    public static final String PROP_ACES = "aces";

    public static final String PROP_MEMBER_OF = "member-of";

    private static final Logger log = LoggerFactory.getLogger(EnsureGroup.class);
    private static final String SERVICE_NAME = "ensure-service-user";
    private static final Map<String, Object> AUTH_INFO;
    public static boolean DEFAULT_ENSURE_IMMEDIATELY = true;

    static {
        AUTH_INFO = Collections.singletonMap(ResourceResolverFactory.SUBSERVICE, (Object) SERVICE_NAME);
    }

    private Group group = null;
    private Operation operation = null;
    @Reference
    private ResourceResolverFactory resourceResolverFactory;
    @Reference
    private EnsureAce ensureAce;

    /**
     * @return the Operation this OSGi Config represents
     */
    @Override
    public Operation getOperation() {
        return operation;
    }

    /**
     * @return the Service User this OSGi Config represents
     */
    @Override
    public Group getAuthorizable() {
        return group;
    }

    /**
     * Entry point for Ensuring a Group.
     *
     * @param operation
     *            the ensure operation to execute (ADD or REMOVE)
     * @param group
     *            the group configuration to ensure
     * @throws EnsureAuthorizableException
     */
    @Override
    public void ensure(Operation operation, AbstractAuthorizable group) throws EnsureAuthorizableException {
        final long start = System.currentTimeMillis();

        try (ResourceResolver resourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO)){

            if (Operation.ADD.equals(operation)) {
                ensureExistance(resourceResolver, (Group) group);
            } else if (Operation.REMOVE.equals(operation)) {
                ensureRemoval(resourceResolver, (Group) group);
            } else {
                throw new EnsureAuthorizableException(
                        "Unable to determine Ensure Group operation Could not create or locate value group (it is null).");
            }

            if (resourceResolver.hasChanges()) {
                resourceResolver.commit();
                log.debug("Persisted change to Group [ {} ]", group.getPrincipalName());
            } else {
                log.debug("No changes required for Group [ {} ]. Skipping...", group.getPrincipalName());
            }

            log.info("Successfully ensured [ {} ] of Group [ {} ] in [ {} ms ]", operation.toString(),
                    getAuthorizable().getPrincipalName(), String.valueOf(System.currentTimeMillis() - start));
        } catch (Exception e) {
            throw new EnsureAuthorizableException(String.format("Failed to ensure [ %s ] of Group [ %s ]",
                    operation.toString(), group.getPrincipalName()), e);
        }
    }

    /**
     * Ensures that the provided Group and configured ACEs exist. Any extra ACEs will be removed, and any missing ACEs
     * added.
     *
     * @param resourceResolver
     *            the resource resolver to perform the group and ACE management
     * @param group
     *            the group to ensure
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    @SuppressWarnings("squid:S2583")
    protected void ensureExistance(ResourceResolver resourceResolver, Group group) throws RepositoryException,
            EnsureAuthorizableException {
        final org.apache.jackrabbit.api.security.user.Group jcrGroup = ensureGroup(resourceResolver, group);

        if (jcrGroup != null) {
            ensureAce.ensureAces(resourceResolver, jcrGroup, group);
            ensureMembership(resourceResolver, jcrGroup, group);
        } else {
            log.error("Could not create or locate Group with principal name [ {} ]", group.getPrincipalName());
        }
    }

    /**
     * Ensures that the provided Group and any of its ACEs are removed.
     *
     * @param resourceResolver
     *            the resource resolver to perform the group and ACE management
     * @param group
     *            the group to ensure
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    @SuppressWarnings("squid:S2589")
    private void ensureRemoval(ResourceResolver resourceResolver, Group group) throws RepositoryException,
            EnsureAuthorizableException {
        org.apache.jackrabbit.api.security.user.Group jcrGroup = findGroup(resourceResolver, group.getPrincipalName());

        ensureAce.removeAces(resourceResolver, jcrGroup, group);
        ensureRemoveMembership(jcrGroup);

        if (jcrGroup != null) {
            jcrGroup.remove();
        }
    }

    /**
     * Ensures a Group exists with the principal name provided by the Group configuration.
     *
     * @param resourceResolver
     *            the resource resolver to perform the group management
     * @param group
     *            the group to ensure
     * @return the Group; this should never return null
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    private org.apache.jackrabbit.api.security.user.Group ensureGroup(ResourceResolver resourceResolver, Group group)
            throws RepositoryException, EnsureAuthorizableException {
        org.apache.jackrabbit.api.security.user.Group jcrGroup = findGroup(resourceResolver, group.getPrincipalName());

        if (jcrGroup == null) {
            final UserManager userManager = resourceResolver.adaptTo(UserManager.class);

            // No principal found with this name; create the group
            log.debug("Requesting creation of group [ {} ] at [ {} ]", group.getPrincipalName(),
                    group.getIntermediatePath());

            jcrGroup = userManager.createGroup(new PrincipalImpl(group.getPrincipalName()), group.getIntermediatePath());
            log.debug("Created group at [ {} ]", jcrGroup.getPath());
        }

        return jcrGroup;
    }

    /**
     * Locates a Group by principal name, or null.
     *
     * @param resourceResolver
     *            the resource resolver to perform the group management
     * @param principalName
     *            the principal name
     * @return the Group or null
     * @throws RepositoryException
     * @throws EnsureAuthorizableException
     */
    private org.apache.jackrabbit.api.security.user.Group findGroup(ResourceResolver resourceResolver,
            String principalName) throws RepositoryException, EnsureAuthorizableException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        org.apache.jackrabbit.api.security.user.Group jcrGroup = null;

        Authorizable authorizable = userManager.getAuthorizable(principalName);

        if (authorizable != null) {
            // Am authorizable was found with this name; check if this is a group
            if (authorizable instanceof org.apache.jackrabbit.api.security.user.Group) {
                jcrGroup = (org.apache.jackrabbit.api.security.user.Group) authorizable;
            } else {
                throw new EnsureAuthorizableException(String.format("Authorizable [ %s ] at [ %s ] is not a group",
                        principalName, authorizable.getPath()));
            }
        }

        return jcrGroup;
    }

    /**
     * Ensure the group is direct member of all groups listed in the Ensure Group config. Any extra memberships are
     * removed.
     *
     * @param resourceResolver
     *            the resource resolver to perform the group management
     * @param jcrGroup
     *            the Jackrabbit security group object
     * @param group
     *            the Group configuration object
     * @throws EnsureAuthorizableException
     *             if any of the groups in the config don't exist, or exist but are not groups
     * @throws RepositoryException
     *             if an error occurs while performing repository operations
     */
    private void ensureMembership(ResourceResolver resourceResolver,
            org.apache.jackrabbit.api.security.user.Group jcrGroup, Group group) throws EnsureAuthorizableException,
            RepositoryException {
        UserManager userManager = resourceResolver.adaptTo(UserManager.class);

        List<String> memberOf = group.getMemberOf();
        Iterator<org.apache.jackrabbit.api.security.user.Group> groupIterator = jcrGroup.declaredMemberOf();
        // for each group this group is a member of, check if it should be per config, if not remove it. if yes,
        // mark it as already added
        while (groupIterator.hasNext()) {
            org.apache.jackrabbit.api.security.user.Group next = groupIterator.next();
            String groupName = next.getPrincipal().getName();
            if (!memberOf.contains(groupName)) {
                // remove
                next.removeMember(jcrGroup);
            } else {
                // mark as satisfied
                group.addMembership(groupName);
            }
        }

        for (String groupName : group.getMissingMemberOf()) {
            Authorizable authorizable = userManager.getAuthorizable(groupName);
            if (authorizable != null) {
                if (authorizable instanceof org.apache.jackrabbit.api.security.user.Group) {
                    org.apache.jackrabbit.api.security.user.Group groupToAdd =
                            (org.apache.jackrabbit.api.security.user.Group) authorizable;
                    groupToAdd.addMember(jcrGroup);
                } else {
                    throw new EnsureAuthorizableException(String.format(
                            "Authorizable [ %s ] at [ %s ] is not a group", groupName, authorizable.getPath()));
                }
            }
        }

    }

    /**
     * Remove the group from all groups it belongs to.
     * 
     * @param jcrGroup
     *            the Jackrabbit security group object
     * @throws RepositoryException
     *             if an error occurs while performing repository operations
     */
    private void ensureRemoveMembership(org.apache.jackrabbit.api.security.user.Group jcrGroup)
            throws RepositoryException {
        Iterator<org.apache.jackrabbit.api.security.user.Group> groupIterator = jcrGroup.declaredMemberOf();
        while (groupIterator.hasNext()) {
            org.apache.jackrabbit.api.security.user.Group next = groupIterator.next();
            // remove
            next.removeMember(jcrGroup);
        }
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        boolean ensureImmediately =
                PropertiesUtil.toBoolean(config.get(PROP_ENSURE_IMMEDIATELY), DEFAULT_ENSURE_IMMEDIATELY);

        String operationStr =
                StringUtils.upperCase(PropertiesUtil.toString(config.get(PROP_OPERATION), DEFAULT_OPERATION));
        try {
            this.operation = Operation.valueOf(operationStr);
            // Parse OSGi Configuration into Group object
            this.group = new Group(config);

            if (ensureImmediately) {
                // Ensure
                ensure(operation, getAuthorizable());
            } else {
                log.info("This Group is configured to NOT ensure immediately. Please ensure this Group via the JMX MBean.");
            }

        } catch (EnsureAuthorizableException e) {
            log.error("Unable to ensure Group [ {} ]",
                    PropertiesUtil.toString(config.get(PROP_PRINCIPAL_NAME), "Undefined Group Principal Name"), e);
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown Ensure Group operation [ " + operationStr + " ]", e);
        }
    }

}
