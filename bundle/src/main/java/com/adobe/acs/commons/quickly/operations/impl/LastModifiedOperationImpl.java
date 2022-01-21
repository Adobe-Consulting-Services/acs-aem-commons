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

package com.adobe.acs.commons.quickly.operations.impl;

import com.adobe.acs.commons.cqsearch.QueryUtil;
import com.adobe.acs.commons.quickly.Command;
import com.adobe.acs.commons.quickly.operations.AbstractOperation;
import com.adobe.acs.commons.quickly.operations.Operation;
import com.adobe.acs.commons.quickly.results.Result;
import com.adobe.acs.commons.quickly.results.impl.serializers.OpenResultSerializerImpl;
import com.adobe.acs.commons.util.TextUtil;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.Query;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.Hit;
import com.day.cq.search.result.SearchResult;
import com.day.cq.wcm.api.NameConstants;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.time.FastDateFormat;
import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.jcr.Session;

import java.text.Format;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.adobe.acs.commons.json.JsonObjectUtil.*;

/**
 * ACS AEM Commons - Quickly - Last Modified Operation
 */
@Component
@Properties({
        @Property(
                name = Operation.PROP_CMD,
                value = LastModifiedOperationImpl.CMD
        )
})
@Service
public class LastModifiedOperationImpl extends AbstractOperation {
    private static final Logger log = LoggerFactory.getLogger(LastModifiedOperationImpl.class);

    public static final String CMD = "lastmod";
    private static final Format DATE_FORMAT = FastDateFormat.getInstance("EEE, d MMM yyyy @ hh:mm aaa");
    private static final int MAX_QUERY_RESULTS = 25;

    @Reference
    private QueryBuilder queryBuilder;

    @Override
    public boolean accepts(final SlingHttpServletRequest request,
                           final Command cmd) {

        return StringUtils.equalsIgnoreCase(CMD, cmd.getOp());
    }

    @Override
    public String getCmd() {
        return CMD;
    }

    @Override
    protected List<Result> withoutParams(final SlingHttpServletRequest request,
                                         final SlingHttpServletResponse response,
                                         final Command cmd) {

        final List<Result> results = new ArrayList<Result>();

        final Result infoResult = new Result.Builder("lastmod [ userId ] [ 1s | 2m | 3h | 4d | 5w | 6M | 7y ]")
                .description("Returns the last modified Pages. Defaults to: me 1d")
                .build();

        results.add(infoResult);

        // Default to "lastmod me 1d"
        results.addAll(this.withParams(request, response, cmd));

        return results;
    }

    @Override
    protected List<Result> withParams(final SlingHttpServletRequest request,
                                      final SlingHttpServletResponse response,
                                      final Command cmd) {

        final long start = System.currentTimeMillis();

        final List<Result> results = new ArrayList<Result>();
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);

        final List<Resource> pages = this.getLastModifiedPages(resourceResolver, cmd);

        log.debug("LastModified pages -- [ {} ] results", pages.size());

        for (final Resource resource : pages) {
            final Page page = pageManager.getContainingPage(resource);

            if (page == null) {
                continue;
            }

            final String title = TextUtil.getFirstNonEmpty(page.getTitle(), page.getPageTitle(),
                    page.getNavigationTitle(), page.getName());

            final String description = page.getPath()
                    + " by "
                    + page.getLastModifiedBy()
                    + " at "
                    + DATE_FORMAT.format(page.getLastModified().getTime());

            results.add(new Result.Builder(title)
                    .path(page.getPath())
                    .description(description)
                    .resultType(OpenResultSerializerImpl.TYPE)
                    .build());
        }

        log.debug("Lastmod - Execution time: {} ms",
                System.currentTimeMillis() - start);

        return results;
    }

    private List<Resource> getLastModifiedPages(final ResourceResolver resourceResolver, final Command cmd) {

        final String relativeDateRange = this.getRelativeDateRangeLowerBoundParam(cmd);
        final String userId = this.getUserIdParam(cmd, resourceResolver.getUserID());

        return this.getLastModifiedQuery(resourceResolver,
                userId,
                relativeDateRange,
                "cq:PageContent",
                "@" + NameConstants.PN_PAGE_LAST_MOD, MAX_QUERY_RESULTS);
    }

    private List<Resource> getLastModifiedQuery(final ResourceResolver resourceResolver,
                                                final String userId,
                                                final String relativeDateRange,
                                                final String nodeType,
                                                final String dateProperty,
                                                final int limit) {

        final List<Resource> resources = new ArrayList<Resource>();
        final Map<String, String> map = new HashMap<String, String>();

        map.put("path", "/content");
        map.put("type", nodeType);

        map.put("1_property", NameConstants.PN_PAGE_LAST_MOD_BY);
        map.put("1_property.value", userId);

        map.put("relativedaterange.property", dateProperty);
        map.put("relativedaterange.lowerBound", relativeDateRange);

        map.put("orderby", dateProperty);
        map.put("orderby.sort", "desc");

        map.put("p.limit", String.valueOf(limit));
        map.put("p.guessTotal", "true");

        log.debug("Lastmod QueryBuilder Map: {}", toJsonObject(map, 2));

        Query query = queryBuilder.createQuery(PredicateGroup.create(map), resourceResolver.adaptTo(Session.class));
        QueryUtil.setResourceResolverOn(resourceResolver, query);
        final SearchResult result = query.getResult();

        for (final Hit hit : result.getHits()) {
            try {
                resources.add(resourceResolver.getResource(hit.getPath()));
            } catch (RepositoryException e) {
                log.error("Error resolving Hit to Resource [ {} ]. "
                        + "Likely issue with lucene index being out of sync.", hit.toString());
            }
        }

        return resources;
    }


    private String getRelativeDateRangeLowerBoundParam(final Command cmd) {
        final String defaultParam = "-1d";
        final String[] params = cmd.getParams();

        if (params.length > 0) {
            String param = params[0];

            if (params.length > 1) {
                param = params[1];
            }

            if (StringUtils.isNotBlank(param)
                    && param.matches("\\d+[s|m|h|d|w|M|y]{1}")) {
                return "-" + param;
            }
        }

        return defaultParam;
    }


    private String getUserIdParam(final Command cmd, String currentUserId) {
        final String[] params = cmd.getParams();

        if (params.length > 0) {
            String param = params[0];

            if (StringUtils.isNotBlank(param)
                    && !StringUtils.equalsIgnoreCase(param, "me")
                    && !param.matches("\\d+[s|m|h|d|w|M|y]{1}")) {
                return param;
            }
        }

        return currentUserId;
    }

}