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
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.jcr.vault.fs.io.AccessControlHandling;
import com.day.jcr.vault.packaging.JcrPackage;
import com.day.jcr.vault.packaging.JcrPackageDefinition;
import com.day.jcr.vault.packaging.Packaging;
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

@SlingServlet(
        label = "ACS AEM Commons - ACL Packager Servlet",
        description = "Servlet end-point used to create ACL CRX packages based on the underlying resource's "
                + "configuration.",
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/packager/acl-packager" },
        selectors =  {"package" },
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

    private static final String QUERY = "SELECT * FROM [rep:ACL]";

    private static final String QUERY_LANG = Query.JCR_SQL2;

    private static final String ACL_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/acl-packager/definition/package-thumbnail.png";

    @Reference
    private Packaging packaging;

    @Reference
    private PackageHelper packageHelper;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.debug("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);

        final String[] principalNames = properties.get(PRINCIPAL_NAMES, new String[]{});

        final Set<Resource> packageResources = this.findResources(resourceResolver,
                Arrays.asList(principalNames),
                this.toPatterns(Arrays.asList(properties.get(INCLUDE_PATTERNS, new String[]{}))));

        try {
            // Add Principals
            if (properties.get(INCLUDE_PRINCIPALS, DEFAULT_INCLUDE_PRINCIPALS)) {
                packageResources.addAll(this.getPrincipalResources(resourceResolver, principalNames));
            }

            // Add the ACL Packager Configuration page
            if (properties.get(INCLUDE_CONFIGURATION, DEFAULT_INCLUDE_CONFIGURATION)) {
                final Resource tmp = this.getACLPackagerPageResource(request);
                if(tmp != null) {
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
                response.getWriter().print(packageHelper.getPreviewJSON(packageResources));
            } else if (packageResources == null || packageResources.isEmpty()) {
                // Do not create empty packages; This will only clutter up CRX Package Manager
                response.getWriter().print(packageHelper.getErrorJSON("Refusing to create a package with no filter "
                        + "set rules."));
            } else {
                // Create JCR Package; Defaults should always be passed in via Request Parameters, but just in case
                final JcrPackage jcrPackage = packageHelper.createPackage(packageResources,
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
     * @return Set (ordered by path) of rep:ACE nodes who hold permissions for at least one Principal
     * enumerated in principleNames
     */
    private Set<Resource> findResources(final ResourceResolver resourceResolver,
                                        final List<String> principalNames,
                                        final List<Pattern> includePatterns) {
        final Set<Resource> resources = new TreeSet<Resource>(resourceComparator);

        final Iterator<Resource> repPolicies = resourceResolver.findResources(QUERY, QUERY_LANG);

        while (repPolicies.hasNext()) {
            final Resource repPolicy = repPolicies.next();

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

        log.debug("Found {} matching rep:policy resources.", resources.size());
        return resources;
    }

    /**
     * Gets the resources for the param principals.
     *
     * @param resourceResolver the resourceresolver to get the principal resources; Must have read access to these resources.
     * @param principalNames   the principals to get
     * @return a list of resources that represent the principals that match to the principalNames
     * @throws RepositoryException
     */
    private List<Resource> getPrincipalResources(final ResourceResolver resourceResolver,
                                                 final String[] principalNames) throws RepositoryException {
        final UserManager userManager = resourceResolver.adaptTo(UserManager.class);
        final List<Resource> resources = new ArrayList<Resource>();

        for (final String principalName : principalNames) {
            final Authorizable authorizable = userManager.getAuthorizable(principalName);
            if (authorizable != null) {
                final Resource resource = resourceResolver.getResource(authorizable.getPath());
                if (resource != null) {
                    resources.add(resource);
                }
            }
        }

        return resources;
    }

    /**
     * Gets the ACL Packager page resource.
     *
     * @param request the Sling HTTP Servlet Request object
     * @return a List of 1 resource representing the cq:Page; or if cannot be found an empty list
     */
    private Resource getACLPackagerPageResource(final SlingHttpServletRequest request) {
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(request.getResource());

        if (page != null) {
            return resourceResolver.getResource(page.getPath());
        }

        return null;
    }

    /**
     * Determines if the resource's path matches any of the include patterns
     * <p/>
     * If includePatterns is null or empty all resources are expected to be included.
     *
     * @param resource        the resource whose path to evaluate
     * @param includePatterns a list of include patterns (regex)
     * @return true if the resource's path matches any of the include patterns
     */
    private boolean isIncluded(final Resource resource, final List<Pattern> includePatterns) {
        if (includePatterns == null || includePatterns.isEmpty()) {
            return true;
        }

        for (final Pattern pattern : includePatterns) {
            final Matcher matcher = pattern.matcher(resource.getPath());
            if (matcher.matches()) {
                return true;
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
