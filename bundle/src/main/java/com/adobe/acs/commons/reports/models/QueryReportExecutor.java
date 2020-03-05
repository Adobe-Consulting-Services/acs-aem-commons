/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2017 Adobe
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
package com.adobe.acs.commons.reports.models;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

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
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportException;
import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;
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

  private SlingHttpServletRequest request;

  private String statement;

  public QueryReportExecutor(SlingHttpServletRequest request) {
    this.request = request;
  }

  private ResultsPage fetchResults(int limit, int offset) throws ReportException {
    prepareStatement();
    ResourceResolver resolver = request.getResourceResolver();
    Session session = resolver.adaptTo(Session.class);
    List<Object> results = new ArrayList<>();
    try {
      QueryManager queryMgr = Optional.ofNullable(session)
          .orElseThrow(() -> new ReportException("Failed to get JCR Session")).getWorkspace().getQueryManager();

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

      while (nodes.hasNext()) {
        results.add(resolver.getResource(nodes.nextNode().getPath()));
      }
    } catch (RepositoryException re) {
      throw new ReportException("Exception executing search results", re);
    }
    return new ResultsPage(results, config.getPageSize(), page);
  }

  @Override
  public ResultsPage getAllResults() throws ReportException {
    return fetchResults(Integer.MAX_VALUE, 0);
  }

  @Override
  public String getDetails() throws ReportException {
    Map<String, String> details = new LinkedHashMap<>();
    details.put("Language", config.getQueryLanguage());
    details.put("Page", Integer.toString(page));
    details.put("Page Size", Integer.toString(config.getPageSize()));
    details.put("Query", statement);

    try {
      final QueryManager queryManager = Optional.ofNullable(request.getResourceResolver().adaptTo(Session.class))
          .orElseThrow(() -> new ReportException("Failed to get JCR Session")).getWorkspace().getQueryManager();
      final Query query = queryManager.createQuery("explain " + statement, config.getQueryLanguage());
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

  @Override
  public ResultsPage getResults() throws ReportException {
    return fetchResults(config.getPageSize(), config.getPageSize() * page);
  }

  private void prepareStatement() throws ReportException {
    try {
      Map<String, String> parameters = getParamPatternMap(request);
      Template template =  new Handlebars().compileInline(config.getQuery());
      statement = template.apply(parameters);
      log.trace("Loaded statement: {}", statement);
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
