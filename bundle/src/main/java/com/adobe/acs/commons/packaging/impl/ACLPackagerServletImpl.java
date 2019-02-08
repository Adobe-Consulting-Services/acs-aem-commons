/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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

package com.adobe.acs.commons.packaging.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.jcr.RepositoryException;
import javax.jcr.query.Query;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.jackrabbit.vault.fs.api.PathFilterSet;
import org.apache.jackrabbit.vault.fs.filter.DefaultPathFilter;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.wrappers.ValueMapDecorator;

import com.adobe.acs.commons.packaging.PackageHelper;

/**
 * ACS AEM Commons - ACL Packager Servlet
 * Servlet end-point used to create ACL CRX packages based on the underlying resource's configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/packager/acl-packager" },
        selectors = { "package" },
        extensions = { "json" }
)
public class ACLPackagerServletImpl extends AbstractPackagerServlet {

    private static final String INCLUDE_PATTERNS = "includePatterns";

    private static final String PRINCIPAL_NAMES = "principalNames";

    private static final String INCLUDE_PRINCIPALS = "includePrincipals";

    private static final String DEFAULT_PACKAGE_NAME = "acls";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "ACLs";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "ACL Package initially defined by a ACS AEM Commons - "
            + "ACL Packager configuration.";

    private static final boolean DEFAULT_INCLUDE_PRINCIPALS = false;

    private static final String QUERY_LANG = Query.JCR_SQL2;

    // rep:ACE covers rep:GrantACE and rep:DenyACE
    private static final String AEM6_QUERY_ACE = "SELECT * FROM [rep:ACE] where [rep:principalName] is not null";

    private static final String[] AEM6_QUERIES = new String[] {AEM6_QUERY_ACE};

    private static final String ACL_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/acl-packager/definition/package-thumbnail.png";

    @Reference
    private PackageHelper packageHelper;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.trace("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);

        final String[] principalNames = properties.get(PRINCIPAL_NAMES, new String[]{});

        final List<PathFilterSet> packageResources = this.findResources(resourceResolver,
                Arrays.asList(principalNames),
                toPatterns(Arrays.asList(properties.get(INCLUDE_PATTERNS, new String[]{}))));

        try {
            // Add Principals
            if (properties.get(INCLUDE_PRINCIPALS, DEFAULT_INCLUDE_PRINCIPALS)) {
                packageResources.addAll(this.getPrincipalResources(resourceResolver, principalNames));
            }
            doPackaging(request, response, preview, properties, packageResources);


        } catch (RepositoryException | IOException ex) {
            log.error(ex.getMessage());
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        }
    }

    private ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("ACL Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<String, Object>());
        } else {
            return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
        }
    }

    /**
     * Search the JCR for all rep:ACE nodes to be further filtered by Grant/Deny ACE rep:principalNames.
     *
     * @param resourceResolver ResourceResolver of initiating user
     * @param principalNames   Principal Names to filter rep:ACE nodes with; Only rep:ACE nodes with children
     *                         with rep:principalNames in this list will be returned
     * @return Set (ordered by path) of rep:ACE coverage who hold permissions for at least one Principal
     * enumerated in principleNames
     */
    @SuppressWarnings("squid:S3776")
    private List<PathFilterSet> findResources(final ResourceResolver resourceResolver,
                                              final List<String> principalNames,
                                              final List<Pattern> includePatterns) {

        final Set<Resource> resources = new TreeSet<Resource>(resourceComparator);
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final String query : AEM6_QUERIES) {
            final Iterator<Resource> hits = resourceResolver.findResources(query, QUERY_LANG);

            while (hits.hasNext()) {
                final Resource hit = hits.next();
                Resource repPolicy = null;


                // get the parent node since the query is for the Grant/Deny nodes
                if (hit.getParent() != null) {
                    repPolicy = hit.getParent();
                }


                if (this.isIncluded(repPolicy, includePatterns)) {
                    log.debug("Included by pattern [ {} ]", repPolicy.getPath());
                } else {
                    continue;
                }

                final Iterator<Resource> aces = repPolicy.listChildren();

                while (aces.hasNext()) {
                    final Resource ace = aces.next();
                    final ValueMap props = ace.adaptTo(ValueMap.class);
                    final String repPrincipalName = props.get("rep:principalName", String.class);

                    if (principalNames == null
                            || principalNames.isEmpty()
                            || principalNames.contains(repPrincipalName)) {

                        resources.add(repPolicy);

                        log.debug("Included by principal [ {} ]", repPolicy.getPath());
                        break;
                    }
                }
            }
        }

        for (final Resource resource : resources) {
            pathFilterSets.add(new PathFilterSet(resource.getPath()));
        }

        log.debug("Found {} matching rep:policy resources.", pathFilterSets.size());
        return pathFilterSets;
    }

    /**
     * Gets the resources for the param principals.
     *
     * @param resourceResolver the ResourceResolver obj to get the principal resources;
     *                         Must have read access to the principal resources.
     * @param principalNames   the principals to get
     * @return a list of PathFilterSets covering the selectes principal names (if they exist)
     * @throws RepositoryException
     */
    private List<PathFilterSet> getPrincipalResources(final ResourceResolver resourceResolver,
                                                      final String[] principalNames) throws RepositoryException {
        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        for (final String principalName : principalNames) {
            final Authorizable authorizable = userManager.getAuthorizable(principalName);
            if (authorizable != null) {
                final Resource resource = resourceResolver.getResource(authorizable.getPath());
                if (resource != null) {
                    final PathFilterSet principal = new PathFilterSet(resource.getPath());
                    // Exclude tokens as they are not vlt installable in AEM6/Oak
                    principal.addExclude(new DefaultPathFilter(resource.getPath() + "/\\.tokens"));
                    pathFilterSets.add(principal);
                }
            }
        }

        return pathFilterSets;
    }

    @Override
    protected String getPackageThumbnailPath() {
        return ACL_PACKAGE_THUMBNAIL_RESOURCE_PATH;
    }

    @Override
    protected String getDefaultPackageDescription() {
        return DEFAULT_PACKAGE_DESCRIPTION;
    }

    @Override
    protected String getDefaultPackageGroupName() {
        return DEFAULT_PACKAGE_GROUP_NAME;
    }

    @Override
    protected String getDefaultPackageName() {
        return DEFAULT_PACKAGE_NAME;
    }

    @Override
    protected PackageHelper getPackageHelper() {
        return packageHelper;
    }

    /**
     * Determines if the resource's path matches any of the include patterns
     * <p>
     * If includePatterns is null or empty all resources are expected to be included.
     *
     * @param resource        the resource whose path to evaluate
     * @param includePatterns a list of include patterns (regex)
     * @return true if the resource's path matches any of the include patterns
     */
    private boolean isIncluded(final Resource resource, final List<Pattern> includePatterns) {
        if (resource == null // Resource is null; so dont accept this
                || (!resource.isResourceType("rep:ACL"))) { // ONLY accept the resource is a rep:ACL node
            return false;
        }

        if (includePatterns == null || includePatterns.isEmpty()) {
            // If patterns are empty then accept everything!
            return true;
        } else {
            // Else check the patterns
            for (final Pattern pattern : includePatterns) {
                final Matcher matcher = pattern.matcher(resource.getPath());
                if (matcher.matches()) {
                    // Accept the resource on the first match
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Compiles a list of string patterns into a list of Pattern objects.
     *
     * @param data List of strings to compile into patterns
     * @return a list of patterns
     */
    private List<Pattern> toPatterns(final List<String> data) {
        final List<Pattern> patterns = new ArrayList<Pattern>();

        for (final String item : data) {
            patterns.add(Pattern.compile(item));
        }

        if (log.isDebugEnabled()) {
            for (final Pattern pattern : patterns) {
                log.debug("Compiled pattern: {}", pattern.toString());
            }
        }

        return patterns;
    }

    /**
     * Compares and sorts resources alphabetically (descending) by path.
     */
    private static Comparator<Resource> resourceComparator = Comparator.comparing(Resource::getPath);
}
