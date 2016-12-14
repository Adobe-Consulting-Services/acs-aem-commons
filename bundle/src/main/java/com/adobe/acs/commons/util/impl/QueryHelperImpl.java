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

package com.adobe.acs.commons.util.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.QueryHelper;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Service
public class QueryHelperImpl implements QueryHelper {

    @Reference
    private QueryBuilder queryBuilder;

    private static final String QUERY_BUILDER = "queryBuilder";

    private static final String LIST = "list";

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
    public List<Resource> findResources(final ResourceResolver resourceResolver,
                                        final String language,
                                        final String statement,
                                        final String relPath) throws RepositoryException {

        final List<Resource> resources = new ArrayList<Resource>();

        if (QUERY_BUILDER.equalsIgnoreCase(language)) {
            final String[] lines = statement.split("[,;\\s\\n\\t]+");
            final Map<String, String> params = ParameterUtil.toMap(lines, "=", false, null, true);

            // ensure all results are returned
            if (!params.containsKey("p.limit")) {
                params.put("p.limit", "-1");
            }

            final com.day.cq.search.Query query = queryBuilder.createQuery(PredicateGroup.create(params), resourceResolver.adaptTo(Session.class));
            final List<Hit> hits = query.getResult().getHits();
            for (final Hit hit : hits) {
                resources.add(hit.getResource());
            }
        } else if (LIST.equalsIgnoreCase(language)) {
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
            QueryManager queryManager = resourceResolver.adaptTo(Session.class).getWorkspace().getQueryManager();
            NodeIterator nodeIter = queryManager.createQuery(statement, language).execute().getNodes();

            while (nodeIter.hasNext()) {
                Resource resource = resourceResolver.getResource(nodeIter.nextNode().getPath());
                if (resource != null) {
                    final Resource relativeAwareResource = getRelativeAwareResource(resource, relPath);

                    if (relativeAwareResource != null) {
                        resources.add(relativeAwareResource);
                    }
                }
            }
        }

        return resources;
    }

    /**
     * Get the relative resource of the given resource if it resolves otherwise
     * the provided resource.
     *
     * @param resource the resource
     * @param relPath  the relative path to resolve against the resource
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
