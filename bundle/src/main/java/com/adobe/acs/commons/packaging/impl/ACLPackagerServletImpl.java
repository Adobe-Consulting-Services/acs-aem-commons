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

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.util.AemCapabilityHelper;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.jcr.vault.fs.api.PathFilterSet;
import com.day.jcr.vault.fs.filter.DefaultPathFilter;
import com.day.jcr.vault.fs.io.AccessControlHandling;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.api.security.user.Authorizable;
import org.apache.jackrabbit.api.security.user.UserManager;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.api.wrappers.ValueMapDecorator;
import org.apache.sling.commons.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Commons - ACL Packager Servlet",
        description = "Servlet end-point used to create ACL CRX packages based on the underlying resource's "
                + "configuration.",
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/packager/acl-packager" },
        selectors = { "package" },
        extensions = { "json" }
)
public class ACLPackagerServletImpl extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(ACLPackagerServletImpl.class);

    private static final String INCLUDE_PATTERNS = "includePatterns";

    private static final String PRINCIPAL_NAMES = "principalNames";

    private static final String PACKAGE_NAME = "packageName";

    private static final String PACKAGE_GROUP_NAME = "packageGroupName";

    private static final String PACKAGE_VERSION = "packageVersion";

    private static final String PACKAGE_DESCRIPTION = "packageDescription";

    private static final String PACKAGE_ACL_HANDLING = "packageACLHandling";

    private static final String CONFLICT_RESOLUTION = "conflictResolution";

    private static final String INCLUDE_PRINCIPALS = "includePrincipals";

    private static final String INCLUDE_CONFIGURATION = "includeConfiguration";

    private static final String DEFAULT_PACKAGE_NAME = "acls";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "ACLs";

    private static final String DEFAULT_PACKAGE_VERSION = "1.0.0";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "ACL Package initially defined by a ACS AEM Commons - "
            + "ACL Packager configuration.";

    private static final boolean DEFAULT_INCLUDE_PRINCIPALS = false;

    private static final boolean DEFAULT_INCLUDE_CONFIGURATION = false;

    private static final String QUERY_LANG = Query.JCR_SQL2;

    private static final String CQ5_QUERY = "SELECT * FROM [rep:ACL]";

    private static final String[] CQ5_QUERIES = new String[] {CQ5_QUERY};

    // rep:ACE covers rep:GrantACE and rep:DenyACE
    private static final String AEM6_QUERY_ACE = "SELECT * FROM [rep:ACE] where [rep:principalName] is not null";

    private static final String[] AEM6_QUERIES = new String[] {AEM6_QUERY_ACE};

    private static final String ACL_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/acl-packager/definition/package-thumbnail.png";

    @Reference
    private PackageHelper packageHelper;

    @Reference
    private AemCapabilityHelper aemCapabilityHelper;

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

            // Add the ACL Packager Configuration page
            if (properties.get(INCLUDE_CONFIGURATION, DEFAULT_INCLUDE_CONFIGURATION)) {
                final PathFilterSet tmp = this.getACLPackagerPageResource(request);
                if (tmp != null) {
                    packageResources.add(tmp);
                }
            }

            final Map<String, String> packageDefinitionProperties = new HashMap<String, String>();

            // ACL Handling
            packageDefinitionProperties.put(JcrPackageDefinition.PN_AC_HANDLING,
                    properties.get(PACKAGE_ACL_HANDLING, AccessControlHandling.OVERWRITE.toString()));

            // Package Description
            packageDefinitionProperties.put(
                    JcrPackageDefinition.PN_DESCRIPTION,
                    properties.get(PACKAGE_DESCRIPTION, DEFAULT_PACKAGE_DESCRIPTION));

            if (preview) {
                // Handle preview mode
                response.getWriter().print(packageHelper.getPathFilterSetPreviewJSON(packageResources));
            } else if (packageResources == null || packageResources.isEmpty()) {
                // Do not create empty packages; This will only clutter up CRX Package Manager
                response.getWriter().print(packageHelper.getErrorJSON("Refusing to create a package with no filter "
                        + "set rules."));
            } else {
                // Create JCR Package; Defaults should always be passed in via Request Parameters, but just in case
                final JcrPackage jcrPackage = packageHelper.createPackageFromPathFilterSets(packageResources,
                        request.getResourceResolver().adaptTo(Session.class),
                        properties.get(PACKAGE_GROUP_NAME, DEFAULT_PACKAGE_GROUP_NAME),
                        properties.get(PACKAGE_NAME, DEFAULT_PACKAGE_NAME),
                        properties.get(PACKAGE_VERSION, DEFAULT_PACKAGE_VERSION),
                        PackageHelper.ConflictResolution.valueOf(properties.get(CONFLICT_RESOLUTION,
                                PackageHelper.ConflictResolution.IncrementVersion.toString())),
                        packageDefinitionProperties
                );

                // Add thumbnail to the package definition
                packageHelper.addThumbnail(jcrPackage,
                        request.getResourceResolver().getResource(ACL_PACKAGE_THUMBNAIL_RESOURCE_PATH));

                log.debug("Successfully created JCR package");
                response.getWriter().print(
                        packageHelper.getSuccessJSON(jcrPackage));
            }
        } catch (RepositoryException ex) {
            log.error(ex.getMessage());
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error(ex.getMessage());
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (JSONException ex) {
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
    private List<PathFilterSet> findResources(final ResourceResolver resourceResolver,
                                              final List<String> principalNames,
                                              final List<Pattern> includePatterns) {
        boolean isOak = true;
        try {
            isOak = aemCapabilityHelper.isOak();
        } catch (RepositoryException e) {
            isOak = true;
        }

        final Set<Resource> resources = new TreeSet<Resource>(resourceComparator);
        final List<PathFilterSet> pathFilterSets = new ArrayList<PathFilterSet>();

        String[] queries = CQ5_QUERIES;
        if (isOak) {
            queries = AEM6_QUERIES;
        }

        for (final String query : queries) {
            final Iterator<Resource> hits = resourceResolver.findResources(query, QUERY_LANG);

            while (hits.hasNext()) {
                final Resource hit = hits.next();
                Resource repPolicy = null;

                if (isOak) {
                    // If Oak, get the parent node since the query is for the Grant/Deny nodes
                    if (hit.getParent() != null) {
                        repPolicy = hit.getParent();
                    }
                } else {
                    // If not Oak, then the rep:ACL is the hit
                    repPolicy = hit;
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

    /**
     * Gets the ACL Packager Page resource.
     *
     * @param request the Sling HTTP Servlet Request object
     * @return a the PathFilterSet wrapping the cq:Page or null
     */
    private PathFilterSet getACLPackagerPageResource(final SlingHttpServletRequest request) {
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(request.getResource());

        if (page != null) {
            return new PathFilterSet(page.getPath());
        }

        return null;
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
        if (resource == null) {
            // Resource is null; so dont accept this
            return false;
        } else if (!resource.isResourceType("rep:ACL")) {
            // ONLY accept the resource is a rep:ACL node
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
    private static List<Pattern> toPatterns(final List<String> data) {
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
    private static Comparator<Resource> resourceComparator = new Comparator<Resource>() {
        public int compare(final Resource r1, final Resource r2) {
            return r1.getPath().compareTo(r2.getPath());
        }
    };
}
