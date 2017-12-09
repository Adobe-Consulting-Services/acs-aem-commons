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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import org.apache.commons.lang3.text.StrSubstitutor;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.models.annotations.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.reports.api.ReportExecutor;
import com.adobe.acs.commons.reports.api.ResultsPage;

/**
 * Model for executing report requests.
 */
@Model(adaptables = SlingHttpServletRequest.class)
public class QueryReportExecutor implements ReportExecutor {

	private static final Logger log = LoggerFactory.getLogger(QueryReportExecutor.class);

	private int page;

	private SlingHttpServletRequest request;

	private QueryReportConfig config;

	private String statement;

	public QueryReportExecutor(SlingHttpServletRequest request) {
		this.request = request;
	}

	public String getParameters() throws UnsupportedEncodingException {
		List<String> params = new ArrayList<String>();
		Enumeration<String> keys = request.getParameterNames();
		while (keys.hasMoreElements()) {
			String key = keys.nextElement();
			for (String value : request.getParameterValues(key)) {
				params.add(URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8"));
			}
		}
		return StringUtils.join(params, "&");
	}

	private void prepareStatement() {
		Map<String, String> parameters = new HashMap<String, String>();
		Enumeration<String> paramNames = request.getParameterNames();
		while (paramNames.hasMoreElements()) {
			String key = paramNames.nextElement();
			parameters.put(key, StringEscapeUtils.escapeSql(request.getParameter(key)));
		}
		log.trace("Loading parameters from request: {}", parameters);
		StrSubstitutor sub = new StrSubstitutor(parameters);
		statement = sub.replace(config.getQuery());
		log.trace("Loaded statement: {}", statement);
	}

	@Override
	public ResultsPage<Resource> getResults() {
		prepareStatement();
		ResourceResolver resolver = request.getResourceResolver();
		Session session = resolver.adaptTo(Session.class);
		List<Resource> results = new ArrayList<Resource>();
		try {
			QueryManager queryMgr = session.getWorkspace().getQueryManager();

			Query query = queryMgr.createQuery(statement, config.getQueryLanguage());

			log.debug("Fetching page {} with limit {} and offset {}",
					new Object[] { page, config.getPageSize(), (config.getPageSize() * page) });
			query.setLimit(config.getPageSize());
			query.setOffset(config.getPageSize() * page);
			QueryResult result = query.execute();
			NodeIterator nodes = result.getNodes();

			while (nodes.hasNext()) {
				results.add(resolver.getResource(nodes.nextNode().getPath()));
			}
		} catch (RepositoryException re) {
			log.error("Exception executing search results", re);
			throw new RuntimeException("Exception executing search results", re);
		}
		return new ResultsPage<Resource>(results, config.getPageSize(), page);
	}

	@Override
	public void setConfiguration(Resource config) {
		this.config = config.adaptTo(QueryReportConfig.class);
	}

	@Override
	public void setPage(int page) {
		this.page = page;
	}

	@Override
	public String getDetails() {
		Map<String, String> details = new LinkedHashMap<String, String>();
		details.put("Language", config.getQueryLanguage());
		details.put("Page", Integer.toString(page));
		details.put("Page Size", Integer.toString(config.getPageSize()));
		details.put("Query", statement);

		try {
			final QueryManager queryManager = request.getResourceResolver().adaptTo(Session.class).getWorkspace()
					.getQueryManager();
			final Query query = queryManager.createQuery("explain " + statement, config.getQueryLanguage());
			final QueryResult queryResult = query.execute();

			final RowIterator rows = queryResult.getRows();
			while(rows.hasNext()){
				final Row row = rows.nextRow();

				String[] cols = queryResult.getColumnNames();
				Value[] values = row.getValues();

				for (int i = 0; i < cols.length; i++) {
					details.put(cols[i], values[i].getString());
				}
			}
			
		} catch (RepositoryException re) {
			log.error("Exception getting details", re);
			throw new RuntimeException("Exception getting details", re);
		}
		StringBuilder sb = new StringBuilder();
		for (Entry<String, String> entry : details.entrySet()) {
			sb.append("<dt>" + StringEscapeUtils.escapeHtml(entry.getKey()) + "</dt>");
			sb.append("<dd>" + StringEscapeUtils.escapeHtml(entry.getValue()) + "</dd>");
		}

		return "<dl>" + sb.toString() + "</dl>";
	}

}
