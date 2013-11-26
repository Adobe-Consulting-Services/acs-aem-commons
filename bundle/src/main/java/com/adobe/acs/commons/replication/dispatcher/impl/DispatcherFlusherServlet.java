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
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ResourceResolver;
import org.apache.sling.api.resource.ResourceUtil;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.io.JSONWriter;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@SuppressWarnings("serial")
@SlingServlet(resourceTypes = "acs-commons/components/utilities/dispatcher-flush/configuration",
        selectors = "flush", methods = "POST")
public class DispatcherFlusherServlet extends SlingAllMethodsServlet {

    @Reference
    private DispatcherFlusher dispatcherFlusher;

    @Override
    protected final void doPost(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {
        final Resource resource = request.getResource();
        final ResourceResolver resourceResolver = request.getResourceResolver();
        final PageManager pageManager = resourceResolver.adaptTo(PageManager.class);
        final Page currentPage = pageManager.getContainingPage(resource);

        final ValueMap properties = ResourceUtil.getValueMap(resource);

        /* Properties */
        final String[] paths = properties.get("paths", new String[0]);
        final ReplicationActionType replicationActionType = ReplicationActionType.valueOf(properties.get(
                "replicationActionType", ReplicationActionType.ACTIVATE.name()));

        final List<FlushResult> overallResults = new ArrayList<FlushResult>();
        boolean caughtException = false;

        try {
            if (paths.length > 0) {
                final Map<Agent, ReplicationResult> results = dispatcherFlusher.flush(resourceResolver,
                        replicationActionType, true, paths);

                for (final Map.Entry<Agent, ReplicationResult> entry : results.entrySet()) {
                    final Agent agent = entry.getKey();
                    final ReplicationResult result = entry.getValue();

                    overallResults.add(new FlushResult(agent, result));
                }
            }
        } catch (ReplicationException ex) {
            caughtException = true;
        }

        if (request.getRequestPathInfo().getExtension().equals("json")) {
            response.setContentType("application/json");
            JSONWriter writer = new JSONWriter(response.getWriter());
            try {
                writer.object();
                for (final FlushResult result : overallResults) {
                    writer.key(result.agentId);
                    writer.value(result.success);
                }
                writer.endObject();
            } catch (JSONException e) {
                throw new ServletException("Unable to output JSON data", e);
            }
        } else {
            String suffix;
            if (caughtException) {
                suffix = "replication-error";
            } else {
                suffix = StringUtils.join(overallResults, '/');
            }

            response.sendRedirect(currentPage.getPath() + ".html/" + suffix);
        }
    }

    private static final class FlushResult {

        private FlushResult(Agent agent, ReplicationResult result) {
            this.agentId = agent.getId();
            this.success = result.isSuccess() && result.getCode() == SlingHttpServletResponse.SC_OK;
        }

        private final String agentId;
        private final boolean success;

        @Override
        public String toString() {
            return agentId + "/" + success;
        }
    }

}
