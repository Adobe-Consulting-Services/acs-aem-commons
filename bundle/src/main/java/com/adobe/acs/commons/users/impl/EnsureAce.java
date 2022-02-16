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

import com.adobe.acs.commons.cqsearch.QueryUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlEntry;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlList;
import org.apache.jackrabbit.api.security.JackrabbitAccessControlManager;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.commons.jackrabbit.authorization.AccessControlUtils;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Component
@Service(EnsureAce.class)
public class EnsureAce {

    private static final Logger log = LoggerFactory.getLogger(EnsureAce.class);

    private static final String PROP_REP_GLOB = "rep:glob";
    private static final String PROP_REP_NT_NAMES = "rep:ntNames";
    private static final String PROP_REP_ITEM_NAMES = "rep:itemNames";
    private static final String PROP_REP_PREFIXES = "rep:prefixes";
    private static final String PROP_NT_REP_ACE = "rep:ACE";
    private static final String PROP_REP_PRINCIPAL_NAME = "rep:principalName";

    @Reference
    private QueryBuilder queryBuilder;

    /**
     * Ensures the ACEs for the Service User exists. Any extra ACEs for the Service User will be removed.
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param jcrAuthorizable  the Jackrabbit Authorizable Object (User or Group) the Authorizable represents
     * @param authorizable     the Authorizable to ensure
     * @return The number of ace entries that could not be processed
     * @throws RepositoryException
     */
    @SuppressWarnings("squid:S3776")
    public int ensureAces(ResourceResolver resourceResolver, Authorizable jcrAuthorizable,
                          AbstractAuthorizable authorizable) throws RepositoryException {
        int failures = 0;
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager =
                (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls =
                findAcls(resourceResolver, authorizable.getPrincipalName(), accessControlManager);

        // For each rep:policy (ACL) this service user participates in ...
        for (final JackrabbitAccessControlList acl : acls) {
            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();
            final boolean serviceUserCoversThisPath = authorizable.hasAceAt(acl.getPath());

            for (final JackrabbitAccessControlEntry ace : aces) {

                if (!StringUtils.equals(authorizable.getPrincipalName(), ace.getPrincipal().getName())) {
                    // Only care about ACEs that this service user participates in
                    continue;
                }

                // Pertains to this service user
                if (StringUtils.startsWith(acl.getPath(), jcrAuthorizable.getPath())) {
                    // Skip the corner case of ACL's under the system user itself; Do nothing to these.

                } else if (!serviceUserCoversThisPath) {
                    // Remove all ACE's for this user from this ACL since this Service User is not configured to cover
                    // this path
                    log.debug("Service user does NOT cover the path yet has an ACE; ensure removal of the ace! {}",
                            ace);
                    acl.removeAccessControlEntry(ace);

                } else {
                    final Ace serviceUserAce = authorizable.getAce(ace, acl.getPath());
                    if (serviceUserAce == null) {
                        acl.removeAccessControlEntry(ace);
                        log.debug("Removed System ACE as it doesn't exist in Service User [ {} ] configuration",
                                authorizable.getPrincipalName());
                    } else {
                        serviceUserAce.setExists(true);
                        log.debug("No-op on System ACE as it already matches Service User [ {} ] configuration",
                                authorizable.getPrincipalName());

                    }
                }
            }

            accessControlManager.setPolicy(acl.getPath(), acl);
        }

        // Create an ACEs that do not yet exist
        for (Ace ace : authorizable.getMissingAces()) {
            if (resourceResolver.getResource(ace.getContentPath()) == null) {
                log.warn(
                        "Unable to apply Service User [ {} ] privileges due to missing path at [ {} ]. Please create the path and re-ensure this service user.",
                        authorizable.getPrincipalName(), ace.getContentPath());
                failures++;
                continue;
            }

            final JackrabbitAccessControlList acl =
                    AccessControlUtils.getAccessControlList(session, ace.getContentPath());
            final Map<String, Value> restrictions = new HashMap<String, Value>();
            final Map<String, Value[]> multiRestrictions = new HashMap<String, Value[]>();

            final ValueFactory valueFactory = session.getValueFactory();

            // Add rep:glob restriction
            if (ace.hasRepGlob()) {
                restrictions.put(PROP_REP_GLOB,
                        valueFactory.createValue(ace.getRepGlob(), PropertyType.STRING));
            }

            // Add rep:ntNames restriction
            if (ace.hasRepNtNames()) {
                multiRestrictions.put(PROP_REP_NT_NAMES,
                        getMultiValues(valueFactory, ace.getRepNtNames(), PropertyType.NAME));
            }

            // Add rep:itemNames
            if (ace.hasRepItemNames()) {
                multiRestrictions.put(PROP_REP_ITEM_NAMES,
                        getMultiValues(valueFactory, ace.getRepItemNames(), PropertyType.NAME));
            }

            // Add rep:prefixes
            if (ace.hasRepPrefixes()) {
                multiRestrictions.put(PROP_REP_PREFIXES,
                        getMultiValues(valueFactory, ace.getRepPrefixes(), PropertyType.STRING));
            }

            // Add ACE to the ACL
            acl.addEntry(jcrAuthorizable.getPrincipal(),
                    ace.getPrivileges(accessControlManager).toArray(new Privilege[]{}), ace.isAllow(), restrictions,
                    multiRestrictions);

            // Update the ACL on the content
            accessControlManager.setPolicy(ace.getContentPath(), acl);

            log.debug("Added Service User ACE for [ {} ] to [ {} ]", authorizable.getPrincipalName(),
                    ace.getContentPath());
        }

        return failures;
    }

    /**
     * Removes all ACEs for the Service User principal (except those that live beneath the System User's rep:User node)
     *
     * @param resourceResolver the resource resolver to perform the user management
     * @param jcrAuthorizable  the Jackrabbit Authorizable Object (User or Group) the Authorizable represents
     * @param authorizable     the Authorizable to remove
     * @throws RepositoryException
     */
    public void removeAces(ResourceResolver resourceResolver, Authorizable jcrAuthorizable,
                           AbstractAuthorizable authorizable) throws RepositoryException {
        final Session session = resourceResolver.adaptTo(Session.class);

        final JackrabbitAccessControlManager accessControlManager =
                (JackrabbitAccessControlManager) session.getAccessControlManager();
        final List<JackrabbitAccessControlList> acls =
                findAcls(resourceResolver, authorizable.getPrincipalName(), accessControlManager);

        for (final JackrabbitAccessControlList acl : acls) {
            final JackrabbitAccessControlEntry[] aces = (JackrabbitAccessControlEntry[]) acl.getAccessControlEntries();

            // Check all the existing ACEs in the ACL
            for (JackrabbitAccessControlEntry ace : aces) {
                if (StringUtils.equals(authorizable.getPrincipalName(), ace.getPrincipal().getName())) {
                    if (jcrAuthorizable != null && StringUtils.startsWith(acl.getPath(), jcrAuthorizable.getPath())) {
                        // Skip! Don't ensureRemoval ACE's from the system user itself!
                    } else {
                        acl.removeAccessControlEntry(ace);
                    }
                }
            }

            accessControlManager.setPolicy(acl.getPath(), acl);
            log.debug("Removed ACE from ACL at [ {} ] for [ {} ]", acl.getPath(), authorizable.getPrincipalName());
        }
    }

    /**
     * Locates by query all the ACLs that the principal participates in.
     *
     * @param resourceResolver     the resource resolver to perform the user management
     * @param principalName        the principal name
     * @param accessControlManager Jackrabbit access control manager
     * @return a list of ACLs that principal participates in.
     */
    private List<JackrabbitAccessControlList> findAcls(ResourceResolver resourceResolver, String principalName,
                                                       JackrabbitAccessControlManager accessControlManager) {
        final Set<String> paths = new HashSet<String>();
        final List<JackrabbitAccessControlList> acls = new ArrayList<JackrabbitAccessControlList>();

        final Map<String, String> params = new HashMap<String, String>();

        params.put("type", PROP_NT_REP_ACE);
        params.put("property", PROP_REP_PRINCIPAL_NAME);
        params.put("property.value", principalName);
        params.put("p.limit", "-1");

        Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
        QueryUtil.setResourceResolverOn(resourceResolver, query);
        for (final Hit hit : query.getResult().getHits()) {
            try {
                final Resource aceResource = resourceResolver.getResource(hit.getPath());

                // first parent is the rep:policy node
                // second parent (grand-parent) is the content node this ACE controls
                // that is the node we need to use the JackrabbitAccessControlManager api
                final Resource contentResource = aceResource.getParent().getParent();

                if (!paths.contains(contentResource.getPath())) {
                    paths.add(contentResource.getPath());
                    for (AccessControlPolicy policy : accessControlManager.getPolicies(contentResource.getPath())) {
                        if (policy instanceof JackrabbitAccessControlList) {
                            acls.add((JackrabbitAccessControlList) policy);
                            break;
                        }
                    }
                }
            } catch (RepositoryException e) {
                log.error("Failed to get resource for query result.", e);
            }
        }

        return acls;
    }

    /**
     * Helper function that returns a list of Strings into an Array of Value's
     *
     * @param valueFactory the valueFactory to create the Value
     * @param valueStrs    the string values to convert
     * @return the array of Values derives from the valueStrs
     */
    private Value[] getMultiValues(ValueFactory valueFactory, List<String> valueStrs, int propertyType)
            throws ValueFormatException {
        final List<Value> result = new ArrayList<Value>();

        for (final String valueStr : valueStrs) {
            result.add(valueFactory.createValue(valueStr, propertyType));
        }

        return result.toArray(new Value[result.size()]);
    }
}
