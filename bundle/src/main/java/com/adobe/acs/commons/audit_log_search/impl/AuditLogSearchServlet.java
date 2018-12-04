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
package com.adobe.acs.commons.audit_log_search.impl;

import com.adobe.acs.commons.audit_log_search.AuditLogSearchRequest;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.servlets.annotations.SlingServletResourceTypes;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.NodeIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.ParseException;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;


@Component(service=Servlet.class)
@SlingServletResourceTypes(
        resourceTypes="acs-commons/components/utilities/audit-log-search", 
        methods= "GET",
        extensions="json",
        selectors="auditlogsearch"
        )

@SuppressWarnings("serial")
public class AuditLogSearchServlet extends SlingSafeMethodsServlet {

    private static final Logger log = LoggerFactory.getLogger(AuditLogSearchServlet.class);

    @Override
    @SuppressWarnings("squid:S1141")
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        log.trace("doGet");

        AuditLogSearchRequest req = null;

        JsonObject result = new JsonObject();
        boolean succeeded = true;
        try {
            req = new AuditLogSearchRequest(request);
            log.debug("Loaded search request: {}", req);

            JsonArray results = new JsonArray();
            long count = 0;
            String whereClause = req.getQueryParameters();
            StringBuilder queryBuilder = new StringBuilder("SELECT * FROM [cq:AuditEvent] AS s");
            if (StringUtils.isNotEmpty(whereClause)) {
                queryBuilder.append(" WHERE ").append(whereClause);
            }
            String queryStr = queryBuilder.toString();
            log.debug("Finding audit events with: {}", queryStr);
            ResourceResolver resolver = request.getResourceResolver();
            QueryManager queryManager = resolver.adaptTo(Session.class).getWorkspace().getQueryManager();
            Query query = queryManager.createQuery(queryStr, Query.JCR_SQL2);

            int limit = -1;
            if (StringUtils.isNotEmpty(request.getParameter("limit"))) {
                limit = Integer.parseInt(request.getParameter("limit"), 10);
                if (limit > 0) {
                    log.debug("Limiting to {} results", limit);
                    query.setLimit(limit);
                }
            }

            NodeIterator nodes = query.execute().getNodes();
            log.debug("Query execution complete!");
            while (nodes.hasNext()) {
                results.add(serializeAuditEvent(resolver.getResource(nodes.nextNode().getPath()), req));
                count++;
            }
            result.addProperty("count", count);
            result.add("events", results);
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

        result.addProperty("succeeded", succeeded);

        response.setContentType("application/json");
        response.getWriter().write(result.toString());
    }

    private JsonObject serializeAuditEvent(Resource auditEventResource, AuditLogSearchRequest request)
            throws RepositoryException, IOException, ClassNotFoundException {
        JsonObject auditEvent = new JsonObject();
        ValueMap properties = auditEventResource.getValueMap();
        auditEvent.addProperty("category", properties.get("cq:category", String.class));
        auditEvent.addProperty("eventPath", auditEventResource.getPath());
        auditEvent.addProperty("path", properties.get("cq:path", String.class));
        auditEvent.addProperty("type", properties.get("cq:type", String.class));
        String userId = properties.get("cq:userid", String.class);
        auditEvent.addProperty("userId", userId);
        auditEvent.addProperty("userName", request.getUserName(auditEventResource.getResourceResolver(), userId));
        auditEvent.addProperty("userPath", request.getUserPath(auditEventResource.getResourceResolver(), userId));
        auditEvent.addProperty("time", properties.get("cq:time", new Date()).getTime());

        JsonArray modified = getModifiedProperties(properties);
        if (properties.get("above", String.class) != null) {
            modified.add(new JsonPrimitive("above=" + properties.get("above", String.class)));
        }
        if (properties.get("destination", String.class) != null) {
            modified.add(new JsonPrimitive("destination=" + properties.get("destination", String.class)));
        }
        if (properties.get("versionId", String.class) != null) {
            modified.add(new JsonPrimitive("versionId=" + properties.get("versionId", String.class)));
        }
        if (modified.size() != 0) {
            auditEvent.add("modified", modified);
        }

        return auditEvent;
    }

    @SuppressWarnings("unchecked")
    private JsonArray getModifiedProperties(ValueMap properties) throws IOException {
        JsonArray modifiedProperties = new JsonArray();
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
                            modifiedProperties.add(new JsonPrimitive(property));
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
