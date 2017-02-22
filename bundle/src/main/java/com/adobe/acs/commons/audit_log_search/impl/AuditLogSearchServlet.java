/*
 * #%L
 * ACS AEM Tools Bundle
 * %%
 * Copyright (C) 2017 Dan Klco
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
package com.adobe.acs.commons.audit_log_search.impl;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.audit_log_search.AuditLogSearchRequest;

@SlingServlet(label = "ACS AEM Commons - Audit Log Search Servlet", methods = { "GET" }, resourceTypes = {
		"acs-commons/components/utilities/audit-log-search" }, selectors = { "auditlogsearch" }, extensions = { "json" })
public class AuditLogSearchServlet extends SlingSafeMethodsServlet {

	private static final long serialVersionUID = 7661105540626580845L;

	private static final Logger log = LoggerFactory.getLogger(AuditLogSearchServlet.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.apache.sling.api.servlets.SlingSafeMethodsServlet#doGet(org.apache.
	 * sling.api.SlingHttpServletRequest,
	 * org.apache.sling.api.SlingHttpServletResponse)
	 */
	@Override
	protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
			throws ServletException, IOException {

		log.trace("doGet");

		AuditLogSearchRequest req = null;
		try {
			JSONObject result = new JSONObject();
			boolean succeeded = true;
			try {
				req = new AuditLogSearchRequest(request);
				log.debug("Loaded search request: {}", req);

				int limit = -1;
				if (StringUtils.isNotEmpty(request.getParameter("limit"))) {
					limit = Integer.parseInt(request.getParameter("limit"), 10);
					log.debug("Limiting to {} results", limit);
				}

				JSONArray results = new JSONArray();
				long count = 0;
				String queryStr = "SELECT * FROM [cq:AuditEvent] AS s WHERE " + req.getQueryParameters();
				log.debug("Finding audit events with: {}", queryStr);
				ResourceResolver resolver = request.getResourceResolver();
				QueryManager queryManager = resolver.adaptTo(Session.class).getWorkspace().getQueryManager();
				Query query = queryManager.createQuery(queryStr, Query.JCR_SQL2);
				query.setLimit(limit);
				NodeIterator nodes = query.execute().getNodes();
				log.debug("Query execution complete!");
				while (nodes.hasNext()) {
					results.put(serializeAuditEvent(resolver.getResource(nodes.nextNode().getPath()), req));
					count++;
				}
				result.put("count", count);
				result.put("events", results);
				log.debug("Found {} audit events", count);
			} catch (ParseException e) {
				log.warn("Encountered exception parsing start / end date", e);
				succeeded = false;
			} catch (RepositoryException e) {
				log.warn("Encountered respository exception attempting to retrieve audit events", e);
				succeeded = false;
			} catch (ClassNotFoundException e) {
				log.warn("Encountered exception deserializing attributes", e);
				succeeded = false;
			}

			result.put("succeeded", succeeded);

			response.setContentType("application/json");
			response.getWriter().write(result.toString());
		} catch (JSONException e) {
			throw new ServletException("Failed to serialize JSON", e);
		}
	}

	private JSONObject serializeAuditEvent(Resource auditEventResource, AuditLogSearchRequest request)
			throws JSONException, RepositoryException, IOException, ClassNotFoundException {
		JSONObject auditEvent = new JSONObject();
		ValueMap properties = auditEventResource.getValueMap();
		auditEvent.put("category", properties.get("cq:category", String.class));
		auditEvent.put("eventPath", auditEventResource.getPath());
		auditEvent.put("path", properties.get("cq:path", String.class));
		auditEvent.put("type", properties.get("cq:type", String.class));
		String userId = properties.get("cq:userid", String.class);
		auditEvent.put("userId", userId);
		auditEvent.put("userName", request.getUserName(auditEventResource.getResourceResolver(), userId));
		auditEvent.put("userPath", request.getUserPath(auditEventResource.getResourceResolver(), userId));
		auditEvent.put("time", properties.get("cq:time", new Date()).getTime());
		
		JSONArray modified = getModifiedProperties(properties);
		if(properties.get("above", String.class) != null){
			modified.put("above="+properties.get("above", String.class));
		}
		if(properties.get("destination", String.class) != null){
			modified.put("destination="+properties.get("destination", String.class));
		}
		if (modified.length() != 0) {
			auditEvent.put("modified", modified);
		}
		if(properties.get("versionId", String.class) != null){
			modified.put("versionId="+properties.get("versionId", String.class));
		}

		return auditEvent;
	}

	@SuppressWarnings("unchecked")
	private JSONArray getModifiedProperties(ValueMap properties) throws IOException {
		JSONArray modifiedProperties = new JSONArray();
		InputStream is = properties.get("cq:properties", InputStream.class);
		if (is != null) {
			ObjectInputStream ois = new ObjectInputStream(is);
			ois.readInt();

			while (ois.available() != -1) {
				try {
					Object obj = ois.readObject();
					if (obj instanceof HashSet) {
						Set<String> propertiesSet = (Set<String>) obj;
						for (String property : propertiesSet) {
							modifiedProperties.put(property);
						}
						break;
					}
				} catch (Exception e) {
					break;
				}
			}
		}
		return modifiedProperties;
	}

}
