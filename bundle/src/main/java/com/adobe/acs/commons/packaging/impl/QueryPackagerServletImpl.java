/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 Adobe
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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.jackrabbit.vault.fs.io.AccessControlHandling;
import org.apache.jackrabbit.vault.packaging.JcrPackage;
import org.apache.jackrabbit.vault.packaging.JcrPackageDefinition;
import org.apache.jackrabbit.vault.packaging.Packaging;
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

import com.adobe.acs.commons.packaging.PackageHelper;
import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;

/**
 * ACS AEM Commons - Query Packager Servlet
 * Servlet end-point used to create Query-based CRX packages based on the underlying resource's configuration.
 */
@SuppressWarnings("serial")
@SlingServlet(
        methods = { "POST" },
        resourceTypes = { "acs-commons/components/utilities/packager/query-packager" },
        selectors = { "package" },
        extensions = { "json" }
)
public class QueryPackagerServletImpl extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(QueryPackagerServletImpl.class);

    private static final String PACKAGE_NAME = "packageName";

    private static final String PACKAGE_GROUP_NAME = "packageGroupName";

    private static final String PACKAGE_VERSION = "packageVersion";

    private static final String PACKAGE_DESCRIPTION = "packageDescription";

    private static final String PACKAGE_ACL_HANDLING = "packageACLHandling";

    private static final String CONFLICT_RESOLUTION = "conflictResolution";

    private static final String DEFAULT_PACKAGE_NAME = "query";

    private static final String DEFAULT_PACKAGE_GROUP_NAME = "Query";

    private static final String DEFAULT_PACKAGE_VERSION = "1.0.0";

    private static final String DEFAULT_PACKAGE_DESCRIPTION = "Query Package initially defined by a ACS AEM Commons - "
            + "Query Packager configuration.";

    private static final String QUERY_PACKAGE_THUMBNAIL_RESOURCE_PATH =
            "/apps/acs-commons/components/utilities/packager/query-packager/definition/package-thumbnail.png";

    private static final String QUERY_BUILDER = "queryBuilder";

    private static final String LIST = "list";

    @Reference
    private Packaging packaging;

    @Reference
    private PackageHelper packageHelper;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public final void doPost(final SlingHttpServletRequest request,
                             final SlingHttpServletResponse response) throws IOException {

        final ResourceResolver resourceResolver = request.getResourceResolver();
        final boolean preview = Boolean.parseBoolean(request.getParameter("preview"));

        log.debug("Preview mode: {}", preview);

        final ValueMap properties = this.getProperties(request);

        try {
            final List<Resource> packageResources = this.findResources(resourceResolver,
                    properties.get("queryLanguage", Query.JCR_SQL2),
                    properties.get("query", String.class),
                    properties.get("relPath", String.class));

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
                        request.getResourceResolver().getResource(QUERY_PACKAGE_THUMBNAIL_RESOURCE_PATH));

                log.debug("Successfully created JCR package");
                response.getWriter().print(
                        packageHelper.getSuccessJSON(jcrPackage));
            }
        } catch (RepositoryException ex) {
            log.error("Repository error while creating Query Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (IOException ex) {
            log.error("IO error while creating Query Package", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        } catch (JSONException ex) {
            log.error("JSON error while creating Query Package response", ex);
            response.getWriter().print(packageHelper.getErrorJSON(ex.getMessage()));
        }
    }

    /**
     * Gets the properties saved to the Query Packager Page's jcr:content node.
     *
     * @param request the request obj
     * @return a ValueMap representing the properties
     */
    private ValueMap getProperties(final SlingHttpServletRequest request) {
        if (request.getResource().getChild("configuration") == null) {
            log.warn("Query Packager Configuration node could not be found for: {}", request.getResource());
            return new ValueMapDecorator(new HashMap<String, Object>());
        } else {
            return request.getResource().getChild("configuration").adaptTo(ValueMap.class);
        }
    }

    /**
     * Find all the resources needed for the package definition.
     *
     * @param resourceResolver the resource resolver to find the resources
     * @param language         the Query language
     * @param statement        the Query statement
     * @param relPath          the relative path to resolve against query result nodes for package resources
     * @return a unique set of paths to include in the package
     * @throws RepositoryException
     */
    private List<Resource> findResources(final ResourceResolver resourceResolver,
                                         final String language,
                                         final String statement,
                                         final String relPath) throws RepositoryException {

        final List<Resource> resources = new ArrayList<Resource>();

        if (language.equals(QUERY_BUILDER)) {
            final String[] lines = StringUtils.split(statement, '\n');
            final Map<String, String> params = ParameterUtil.toMap(lines, "=", false, null, true);

            // ensure all results are returned
            params.put("p.limit", "-1");

            final com.day.cq.search.Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
            final List<Hit> hits = query.getResult().getHits();
            for (final Hit hit : hits) {
                resources.add(hit.getResource());
            }
        } else if (language.equals(LIST)) {
            if (StringUtils.isNotBlank(statement)) {
                final String[] lines = statement.split("[,;\\s\\n\\t]+");

                for (String line : lines) {
                    if (StringUtils.isNotBlank(line)) {
                        final Resource resource = resourceResolver.getResource(line);
                        final Resource relativeAwareResource = getRelativeAwareResource(resource, relPath);

                        if (relativeAwareResource != null) {
                            resources.add(relativeAwareResource);
                        }
                    }
                }
            }
        } else {
            Iterator<Resource> resourceIterator = resourceResolver.findResources(statement, language);

            while (resourceIterator.hasNext()) {
                final Resource resource = resourceIterator.next();
                final Resource relativeAwareResource = getRelativeAwareResource(resource, relPath);

                if (relativeAwareResource != null) {
                    resources.add(relativeAwareResource);
                }
            }
        }

        return resources;
    }

    /**
     * Get the relative resource of the given resource if it resolves otherwise
     * the provided resource.
     *
     * @param resource         the resource
     * @param relPath          the relative path to resolve against the resource
     * @return the relative resource if it resolves otherwise the resource
     */
    private Resource getRelativeAwareResource(final Resource resource, final String relPath) {
        if (resource != null && StringUtils.isNotBlank(relPath)) {
            final Resource relResource = resource.getChild(relPath);

            if (relResource != null) {
                return relResource;
            }
        }

        return resource;
    }
}
