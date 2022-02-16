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
package com.adobe.acs.commons.replication.dispatcher.impl;

import com.adobe.acs.commons.replication.dispatcher.DispatcherFlusher;
import com.day.cq.replication.Agent;
import com.day.cq.replication.ReplicationActionType;
import com.day.cq.replication.ReplicationException;
import com.day.cq.replication.ReplicationResult;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.LoginException;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceResolverFactory;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.adobe.acs.commons.replication.dispatcher.impl.DispatcherFlushRulesImpl.AUTH_INFO;
import com.google.gson.Gson;
import java.util.LinkedHashMap;

@SuppressWarnings("serial")
@SlingServlet(resourceTypes = "acs-commons/components/utilities/dispatcher-flush/configuration",
        selectors = "flush", methods = "POST")
public class DispatcherFlusherServlet extends SlingAllMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(DispatcherFlusherServlet.class);

    @Reference
    private transient DispatcherFlusher dispatcherFlusher;

    @Reference
    private transient ResourceResolverFactory resourceResolverFactory;

    private static final boolean DEFAULT_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER = true;

    private boolean flushWithAdminResourceResolver = DEFAULT_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER;

    @Property(label = "Flush with Admin Resource Resolver",
            description = "This allows the user of any Dispatcher Flush UI Web UI to invalidate/delete the cache of "
                    + "any content tree. Note; this is only pertains to the dispatcher cache and does not effect the "
                    + "users JCR permissions. [ Default: true ]",
            boolValue = DEFAULT_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER)
    public static final String PROP_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER = "flush-with-admin-resource-resolver";

    @Override
    @SuppressWarnings("squid:S3776")
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Resource resource = request.getResource();
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page currentPage = pageManager.getContainingPage(resource);

        final ValueMap properties = resource.getValueMap();

        /* Properties */
        final String[] paths = properties.get("paths", new String[0]);
        final ReplicationActionType replicationActionType = ReplicationActionType.valueOf(properties.get(
                "replicationActionType", ReplicationActionType.ACTIVATE.name()));

        final List<FlushResult> overallResults = new ArrayList<FlushResult>();
        boolean caughtException = false;

        ResourceResolver flushingResourceResolver = null;

        try {
            if (paths.length > 0) {

                if (flushWithAdminResourceResolver) {
                    // Use the admin resource resolver for replication to ensure all
                    // replication permission checks are OK
                    // Make sure to close this resource resolver
                    flushingResourceResolver = resourceResolverFactory.getServiceResourceResolver(AUTH_INFO);
                } else {
                    // Use the HTTP Request's resource resolver; don't close this resource resolver
                    flushingResourceResolver = resourceResolver;
                }

                final Map<Agent, ReplicationResult> results = dispatcherFlusher.flush(flushingResourceResolver,
                        replicationActionType, true, paths);

                for (final Map.Entry<Agent, ReplicationResult> entry : results.entrySet()) {
                    final Agent agent = entry.getKey();
                    final ReplicationResult result = entry.getValue();

                    overallResults.add(new FlushResult(agent, result));
                }
            }
        } catch (ReplicationException e) {
            log.error("Replication exception occurred during Dispatcher Flush request.", e);
            caughtException = true;
        } catch (LoginException e) {
            log.error("Could not obtain an Admin Resource Resolver during Dispatcher Flush request.", e);
            caughtException = true;
        } finally {
            if (flushWithAdminResourceResolver && flushingResourceResolver != null) {
                // Close the admin resource resolver if opened by this servlet
                flushingResourceResolver.close();
            }
        }

        if (request.getRequestPathInfo().getExtension().equals("json")) {
            response.setContentType("application/json");
            Gson gson = new Gson();
            Map<String, Object> resultMap = new LinkedHashMap<>();
            for (final FlushResult result : overallResults) {
                resultMap.put(result.agentId, result.success);
            }
            String json = gson.toJson(resultMap); // #2749
            response.getWriter().write(json);
        } else {
            String suffix;
            if (caughtException) {
                suffix = "replication-error";
            } else {
                suffix = StringUtils.join(overallResults, '/');
            }

            response.sendRedirect(request.getContextPath() + currentPage.getPath() + ".html/" + suffix);
        }
    }

    private static final class FlushResult {

        private FlushResult(Agent agent, ReplicationResult result) {
            this.agentId = agent.getId();
            this.success = result.isSuccess() && result.getCode() == HttpServletResponse.SC_OK;
        }

        private final String agentId;

        private final boolean success;

        @Override
        public String toString() {
            return agentId + "/" + success;
        }
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        this.flushWithAdminResourceResolver = PropertiesUtil.toBoolean(
                config.get(PROP_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER),
                DEFAULT_FLUSH_WITH_ADMIN_RESOURCE_RESOLVER);
    }
}
