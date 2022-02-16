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
package com.adobe.acs.commons.contentfinder.querybuilder.impl.viewhandler;

import com.adobe.acs.commons.cqsearch.QueryUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.wcm.core.contentfinder.ViewHandler;
import com.day.cq.wcm.core.contentfinder.ViewQuery;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Session;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * ACS AEM Commons - GQL to Querybuilder View Handler
 * Leverage Querybuilder to run ContentFinder queries
 */
@SuppressWarnings("serial")
@Component
@Properties({
        @Property(
                label = "Servlet Paths",
                name = "sling.servlet.paths",
                value = "/bin/wcm/contentfinder/qb/view"
        )
})
@Service
public final class QueryBuilderViewHandler extends ViewHandler {
    private static final Logger log = LoggerFactory.getLogger(QueryBuilderViewHandler.class);

    @Reference
    private transient QueryBuilder queryBuilder;

    @Override
    protected ViewQuery createQuery(SlingHttpServletRequest slingRequest, Session session,
                                    String queryString) throws Exception {
        Map<String, String> map;

        if (GQLToQueryBuilderConverter.convertToQueryBuilder(slingRequest)) {
            map = this.convertToQueryBuilderParams(slingRequest, queryString);
            log.debug("Forced QueryBuilder Parameter Map: {}", map);
        } else {
            map = this.getQueryBuilderParams(slingRequest, queryString);
            log.debug("Converted QueryBuilder Parameter Map: {}", map);
        }

        final Query query = queryBuilder.createQuery(PredicateGroup.create(map), session);
        QueryUtil.setResourceResolverOn(slingRequest.getResourceResolver(), query);
        return new QueryBuilderViewQuery(query);
    }

    /**
     * Assume query should be treated as a QueryBuilder query, rather than a GQL query
     * <p>
     * This intelligently converts default Fulltext and Limit parameters to QueryBuilder equivalents
     *
     * @param request
     * @param queryString
     * @return
     */
    @SuppressWarnings("unchecked")
    private Map<String, String> getQueryBuilderParams(final SlingHttpServletRequest request, final String queryString) {
        Map<String, String> map = new LinkedHashMap<String, String>();

        for (final String key : (Set<String>) request.getParameterMap().keySet()) {
            // Skip known content finder parameters that are unused for QueryBuilder
            if (!ArrayUtils.contains(ContentFinderConstants.QUERYBUILDER_BLACKLIST, key)) {
                final String val = request.getParameter(key);
                if (StringUtils.isNotBlank(val)) {
                    map.put(key, val);
                }
            } else {
                log.debug("Rejecting property [ {} ] due to blacklist match", key);
            }
        }

        map = GQLToQueryBuilderConverter.addFulltext(request, map, queryString);
        map = GQLToQueryBuilderConverter.addLimitAndOffset(request, map);

        return map;
    }


    private Map<String, String> convertToQueryBuilderParams(final SlingHttpServletRequest request,
                                                            final String queryString) {
        Map<String, String> map = new LinkedHashMap<String, String>();

        int userDefinedPropertyCount = 0;

        for (final String key : request.getRequestParameterMap().keySet()) {
            if (StringUtils.equals(key, ContentFinderConstants.CF_PATH)) {
                log.debug("Converting path...");
                map = GQLToQueryBuilderConverter.addPath(request, map);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_TYPE)) {
                log.debug("Converting type...");
                map = GQLToQueryBuilderConverter.addType(request, map);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_NAME)) {
                log.debug("Converting name...");
                map = GQLToQueryBuilderConverter.addName(request, map);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_TAGS)) {
                log.debug("Converting tags...");
                map = GQLToQueryBuilderConverter.addTags(request, map);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_MIMETYPE)) {
                log.debug("Converting mimeType...");
                map = GQLToQueryBuilderConverter.addMimeType(request, map);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_FULLTEXT)) {
                log.debug("Converting fulltext...");
                map = GQLToQueryBuilderConverter.addFulltext(request, map, queryString);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_ORDER)) {
                log.debug("Converting order...");
                map = GQLToQueryBuilderConverter.addOrder(request, map, queryString);
            } else if (StringUtils.equals(key, ContentFinderConstants.CF_LIMIT)) {
                log.debug("Converting limit...");
                map = GQLToQueryBuilderConverter.addLimitAndOffset(request, map);
            } else {
                // User Defined Properties
                log.debug("Converting property [ {} ]...", key);
                map = GQLToQueryBuilderConverter.addProperty(request, map, key, userDefinedPropertyCount);
                userDefinedPropertyCount++;
            }
        }

        /**
         * AND all groups together
         */
        map.put("p.or", "false");

        return map;
    }
}