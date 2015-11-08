/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2013 - 2014 Adobe
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
package com.adobe.acs.commons.replication.impl;

import com.adobe.acs.commons.replication.ReplicateVersion;
import com.adobe.acs.commons.replication.ReplicationResult;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * ACS AEM Commons - Replicate Version Servlet
 * Servlet end-point used to initiate replication of resource versions.
 */
@SuppressWarnings("serial")
@SlingServlet(
        resourceTypes = "acs-commons/components/utilities/version-replicator",
        selectors = "replicateversion",
        extensions = "json",
        methods = "POST",
        generateComponent = true)
public class ReplicateVersionServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory
            .getLogger(ReplicateVersionServlet.class);

    @Reference
    private ReplicateVersion replicateVersion;

    @Override
    public final void doPost(SlingHttpServletRequest req,
            SlingHttpServletResponse res) throws ServletException, IOException {

        log.debug("Entering ReplicatePageVersionServlet.doPost(..)");

        JSONObject obj = null;

        try {
            String[] rootPaths = req.getParameterValues("rootPaths");
            Date date = getDate(req.getParameter("datetimecal"));
            String[] agents = req.getParameterValues("cmbAgent");

            obj = validate(rootPaths, agents, date);

            if (!obj.has("error")) {
                log.debug("Initiating version replication");

                List<ReplicationResult> response = replicateVersion.replicate(
                        req.getResourceResolver(), rootPaths, agents, date);

                if (log.isDebugEnabled()) {
                    for (final ReplicationResult replicationResult : response) {
                        log.debug("Replication result: {} -- {}",
                                replicationResult.getPath(),
                                replicationResult.getStatus());
                    }
                }

                JSONArray arr = convertResponseToJson(response);
                obj = new JSONObject();
                obj.put("result", arr);

            } else {
                log.debug("Did not attempt to replicate version due to issue with input params");

                try {
                    res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                    obj.put("status", "error");
                } catch (JSONException e) {
                    log.error("exception occurred", e);
                }
            }
        } catch (JSONException ex) {
            try {
                if (obj == null) {
                    obj = new JSONObject();
                }
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                obj.put("error", "System Error.");
                obj.put("status", "error");
            } catch (JSONException e) {
                log.error("exception occurred", e);
            }
        }

        try {
            res.setContentType("application/json");
            obj.write(res.getWriter());
        } catch (JSONException e) {
            log.error("exception occurred", e);
        }
    }

    private JSONArray convertResponseToJson(List<ReplicationResult> list) throws JSONException {
        JSONArray arr = new JSONArray();

        for (ReplicationResult result : list) {
            JSONObject resultObject = new JSONObject();

            resultObject.put("path", result.getPath());
            resultObject.put("status", result.getStatus().name());
            resultObject.put("version", result.getVersion());

            arr.put(resultObject);
        }
        return arr;
    }


    private JSONObject validate(String[] rootPaths, String[] agents, Date date)
            throws JSONException {

        final JSONObject obj = new JSONObject();

        if (ArrayUtils.isEmpty(rootPaths)) {
            obj.put("error", "Select at least 1 root path.");
            log.debug("Error validating root paths (they're empty)");
            return obj;
        }

        for (final String rootPath : rootPaths) {
            if (StringUtils.isBlank(rootPath)) {
                obj.put("error", "Root paths cannot be empty.");
                log.debug("Error validating a root path");
                return obj;
            }
        }

        if (date == null) {
            obj.put("error", "Specify the date and time to select the appropriate resource versions for replication.");
            log.debug("Error validating date");
            return obj;
        }

        if (ArrayUtils.isEmpty(agents)) {
            obj.put("error", "Select at least 1 replication agent.");
            log.debug("Error validating agents");
            return obj;
        }

        log.debug("Validated all version replication inputs successfully");

        return obj;
    }

    private Date getDate(String datetime) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss aa");

            date = sdf.parse(datetime);
        } catch (Exception e) {
            log.error("exception occurred", e);
        }
        return date;
    }
}
