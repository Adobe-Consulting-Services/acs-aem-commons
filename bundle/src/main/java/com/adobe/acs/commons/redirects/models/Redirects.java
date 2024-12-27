/*
 * ACS AEM Commons
 *
 * Copyright (C) 2013 - 2023 Adobe
 *
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
 */
package com.adobe.acs.commons.redirects.models;

import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.google.common.collect.Lists;

import java.util.*;
import javax.annotation.PostConstruct;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.SlingObject;
import org.jetbrains.annotations.NotNull;

import static com.adobe.acs.commons.redirects.filter.RedirectFilter.REDIRECT_RULE_RESOURCE_TYPE;
import static com.adobe.acs.commons.redirects.models.RedirectRule.*;

/**
 * Model for paginated output on http://localhost:4502/apps/acs-commons/content/redirect-manager.html
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class Redirects {

    public static final String CFG_PROP_CONTEXT_PREFIX = "contextPrefix";
    public static final String CFG_PROP_IGNORE_SELECTORS = "ignoreSelectors";

    @SlingObject
    private SlingHttpServletRequest request;

    @OSGiService
    private QueryBuilder queryBuilder;


    int pageNumber = 1;
    int pageSize = 100;
    List<List<Resource>> pages;

    String contextPrefix;
    boolean ignoreSelectors;

    @PostConstruct
    protected void init() {

        Resource configResource = request.getRequestPathInfo().getSuffixResource();
        ValueMap properties = configResource.getValueMap();
        contextPrefix = properties.get(Redirects.CFG_PROP_CONTEXT_PREFIX, "");
        ignoreSelectors = properties.get(Redirects.CFG_PROP_IGNORE_SELECTORS, false);

        List<Resource> all = new ArrayList<>();

        if (ArrayUtils.contains(request.getRequestPathInfo().getSelectors(), "search")) {
            // Search
            pages = Collections.singletonList(search(request.getParameter("term"), configResource.getPath()));
            pageNumber = 1;
        } else {
            // Browse
            String pg = request.getParameter("page");
            if (pg != null) {
                pageNumber = Integer.parseInt(pg);
            }
            new AbstractResourceVisitor() {
                @Override
                public void visit(Resource res) {
                    if(res.isResourceType(REDIRECT_RULE_RESOURCE_TYPE)){
                        all.add(res);
                    }
                }
            }.accept(configResource);
            pages = Lists.partition(all, pageSize);
        }
    }

    public List<Resource> getItems() {
        return pages.isEmpty() ? Collections.emptyList() : pages.get(pageNumber - 1);
    }

    public boolean isPaginated() {
        return pages.size() > 1;
    }

    public int getPages() {
        return pages.size();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public boolean hasNext() {
        return pageNumber < pages.size();
    }

    public int getNextPage() {
        return pageNumber + 1;
    }

    public boolean hasPrevious() {
        return pageNumber > 1;
    }

    public int getPreviousPage() {
        return pageNumber - 1;
    }

    public String getContextPrefix() {
        return contextPrefix;
    }

    public boolean getIgnoreSelectors() {
        return ignoreSelectors;
    }


    private List<Resource> search(String term, @NotNull String path) {
        final List<Resource> resources = new ArrayList<>();

        final Map<String, String> map = new HashMap<>();

        term = "*" + StringUtils.lowerCase(term) + "*";

        map.put("type", "nt:unstructured");
        map.put("path", path);

        map.put("1_group.p.or", "true");
        map.put("1_group.1_fulltext.relPath", "@source");
        map.put("1_group.1_fulltext", term);
        map.put("1_group.2_fulltext.relPath", "@target");
        map.put("1_group.2_fulltext", term);
        map.put("1_group.3_fulltext.relPath", "@note");
        map.put("1_group.3_fulltext", term);
        map.put("1_group.4_fulltext.relPath", "@" + CACHE_CONTROL_HEADER_NAME);
        map.put("1_group.4_fulltext", term);
        map.put("1_group.f5_ulltext.relPath", "@cq:tags");
        map.put("1_group.5_fulltext", term);
        map.put("1_group.6_fulltext.relPath", "@statusCode");
        map.put("1_group.6_fulltext", term);
        map.put("1_group.7_fulltext.relPath", "@" + CREATED_BY_PROPERTY_NAME);
        map.put("1_group.7_fulltext", term);
        map.put("1_group.8_fulltext.relPath", "@" + MODIFIED_BY_PROPERTY_NAME);
        map.put("1_group.8_fulltext", term);

        map.put("property", "sling:resourceType");
        map.put("property.value", "acs-commons/components/utilities/manage-redirects/redirect-row");

        map.put("orderby", "@jcr:score");
        map.put("orderby.sort", "desc");

        map.put("p.offset", "0");
        map.put("p.limit", "100");

        map.put("p.guessTotal", "true");

        Query query = queryBuilder.createQuery(PredicateGroup.create(map), request.getResourceResolver().adaptTo(Session.class));

        SearchResult result = query.getResult();

        // QueryBuilder has a leaking ResourceResolver, so the following work around is required.
        ResourceResolver leakingResourceResolver = null;

        try {
            // A common use case it to collect all the resources that represent hits and put them in a list for work outside of the search service

            for (final Hit hit : result.getHits()) {
                if (leakingResourceResolver == null) {
                    // Get a reference to QB's leaking ResourceResolver
                    leakingResourceResolver = hit.getResource().getResourceResolver();
                }

                Resource resource = request.getResourceResolver().getResource(hit.getPath());
                resources.add(resource);
            }

        } catch (RepositoryException e) {
        } finally {
            if (leakingResourceResolver != null) {
                // Always Close the leaking QueryBuilder resourceResolver.
                leakingResourceResolver.close();
            }
        }

        return resources;
    }
}
