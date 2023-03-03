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
package com.adobe.acs.commons.reports.models;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.inject.Inject;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.apache.sling.models.annotations.injectorspecific.OSGiService;
import org.apache.sling.models.annotations.injectorspecific.Self;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
import com.adobe.acs.commons.util.ParameterUtil;
import com.adobe.acs.commons.util.impl.QueryHelperImpl;
import com.day.cq.search.PredicateGroup;
import com.day.cq.search.QueryBuilder;
import com.day.cq.search.result.SearchResult;
import com.github.jknack.handlebars.Handlebars;
import com.github.jknack.handlebars.Template;

/**
 * Model for executing report requests.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class QueryReportExecutor implements ReportExecutor {

    private static final Logger log = LoggerFactory.getLogger(QueryReportExecutor.class);

    private QueryReportConfig config;

    private int page;

    private final SlingHttpServletRequest request;
    private final QueryBuilder queryBuilder;

    @Inject
    public QueryReportExecutor(@Self SlingHttpServletRequest request, @OSGiService QueryBuilder queryBuilder) {
        this.request = request;
        this.queryBuilder = queryBuilder;
    }

    private ResultsPage fetchResults(int limit, int offset) throws ReportException {

        try {
            final Pair<Stream<Resource>, Long> results;
            if (isQueryBuilder()) {
                results = getResultsFromQueryBuilder(limit, offset);
            } else {
                results = getResultsFromQuery(limit, offset);
            }
            return new ResultsPage(results.getLeft(), config.getPageSize(), page, results.getRight());
        } catch (RepositoryException re) {
            throw new ReportException("Exception executing search results", re);
        }
    }

    private boolean isQueryBuilder() {
        return QueryHelperImpl.QUERY_BUILDER.equals(config.getQueryLanguage());
    }

    private Session getSession() throws ReportException {
        return Optional.ofNullable(request.getResourceResolver().adaptTo(Session.class))
                .orElseThrow(() -> new ReportException("Failed to get JCR Session"));
    }

    private com.day.cq.search.Query prepareQueryBuilderQuery() throws ReportException {

        Session session = getSession();
        final Map<String, String> params = ParameterUtil.toMap(prepareStatement().split("\n\n?"), "=", false, null,
                true);

        return queryBuilder.createQuery(PredicateGroup.create(params),
                session);
    }

    private Pair<Stream<Resource>, Long> getResultsFromQueryBuilder(
            int limit, int offset) throws ReportException {

        com.day.cq.search.Query query = prepareQueryBuilderQuery();
        if (page != -1) {
            log.debug("Fetching results with limit {} and offset {}", limit, offset);
            query.setHitsPerPage(limit);
            query.setStart(offset);
        } else {
            log.debug("Fetching all results");
        }

        SearchResult result = query.getResult();
        long count = -1;
        if (limit <= 100) {
            count = result.getHits().size();
        }
        ResourceResolver resolver = this.request.getResourceResolver();
        return ImmutablePair.of(result.getHits().stream().map(h -> {
            try {
                return resolver.getResource(h.getPath());
            } catch (RepositoryException e) {
                log.warn("Could not get node behind search result hit", e);
                return null;
            }
        }), count);

    }

    private Pair<Stream<Resource>, Long> getResultsFromQuery(int limit, int offset)
            throws RepositoryException, ReportException {
        String statement = prepareStatement();
        QueryManager queryMgr = getSession().getWorkspace().getQueryManager();
        Query query = queryMgr.createQuery(statement, config.getQueryLanguage());

        if (page != -1) {
            log.debug("Fetching results with limit {} and offset {}", limit, offset);
            query.setLimit(limit);
            query.setOffset(offset);
        } else {
            log.debug("Fetching all results");
        }
        QueryResult result = query.execute();

        NodeIterator nodes = result.getNodes();

        Spliterator<Node> spliterator = Spliterators.spliteratorUnknownSize(nodes,
                Spliterator.ORDERED | Spliterator.NONNULL);

        ResourceResolver resolver = request.getResourceResolver();
        return ImmutablePair.of(StreamSupport.stream(spliterator, false).map(n -> getResource(n, resolver)),
                nodes.getSize());

    }

    @Override
    public ResultsPage getAllResults() throws ReportException {
        return fetchResults(Integer.MAX_VALUE, 0);
    }

    private void addQueryDetails(Map<String, String> details) throws ReportException {
        try {
            final QueryManager queryManager = getSession().getWorkspace()
                    .getQueryManager();
            final Query query = queryManager.createQuery("explain " + prepareStatement(), config.getQueryLanguage());
            final QueryResult queryResult = query.execute();

            final RowIterator rows = queryResult.getRows();
            while (rows.hasNext()) {
                final Row row = rows.nextRow();

                String[] cols = queryResult.getColumnNames();
                Value[] values = row.getValues();

                for (int i = 0; i < cols.length; i++) {
                    details.put(cols[i], values[i].getString());
                }
            }
        } catch (RepositoryException re) {
            throw new ReportException("Exception getting details", re);
        }
    }

    private void addQueryBuilderDetails(Map<String, String> details) throws ReportException {
        SearchResult result = prepareQueryBuilderQuery().getResult();
        details.put("XPath Query", result.getQueryStatement());
        details.put("Filtering Predicates", result.getFilteringPredicates());
    }

    @Override
    public String getDetails() throws ReportException {
        String statement = prepareStatement();
        Map<String, String> details = new LinkedHashMap<>();
        details.put("Language", config.getQueryLanguage());
        details.put("Page", Integer.toString(page));
        details.put("Page Size", Integer.toString(config.getPageSize()));
        details.put("Query", statement);

        if (isQueryBuilder()) {
            addQueryBuilderDetails(details);
        } else {
            addQueryDetails(details);
        }
        StringBuilder sb = new StringBuilder();
        for (Entry<String, String> entry : details.entrySet()) {
            sb.append("<dt>" + StringEscapeUtils.escapeHtml(entry.getKey()) + "</dt>");
            sb.append("<dd>" + StringEscapeUtils.escapeHtml(entry.getValue()) + "</dd>");
        }

        return "<dl>" + sb.toString() + "</dl>";
    }

    @Override
    public String getParameters() throws ReportException {
        List<String> params = new ArrayList<>();
        Enumeration<String> keys = request.getParameterNames();
        while (keys.hasMoreElements()) {
            String key = keys.nextElement();
            for (String value : request.getParameterValues(key)) {
                try {
                    params.add(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
                } catch (UnsupportedEncodingException e) {
                    throw new ReportException("UTF-8 encoding available", e);
                }
            }
        }
        return StringUtils.join(params, "&");
    }

    private Resource getResource(Node node, ResourceResolver resolver) {
        try {
            return resolver.getResource(node.getPath());
        } catch (RepositoryException e) {
            log.warn("Failed to get path from node: {}", node, e);
            return null;
        }
    }

    @Override
    public ResultsPage getResults() throws ReportException {
        return fetchResults(config.getPageSize(), config.getPageSize() * page);
    }

    private String prepareStatement() throws ReportException {
        try {
            Map<String, String> parameters = getParamPatternMap(request);
            Template template = new Handlebars().compileInline(config.getQuery());
            String statement = template.apply(parameters);
            log.trace("Loaded statement: {}", statement);
            return statement;
        } catch (IOException ioe) {
            throw new ReportException("Exception templating query", ioe);
        }
    }

    @Override
    public void setConfiguration(Resource config) {
        this.config = config.adaptTo(QueryReportConfig.class);
    }

    @Override
    public void setPage(int page) {
        this.page = page;
    }

}
