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
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

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
    private static final String KEY_ERROR = "error";
    private static final String KEY_RESULT = "result";
    private static final String KEY_STATUS = "status";
    private static final String KEY_PATH = "path";
    private static final String KEY_VERSION = "version";

    @Reference
    private ReplicateVersion replicateVersion;

    @Override
    public final void doPost(SlingHttpServletRequest req,
            SlingHttpServletResponse res) throws ServletException, IOException {

        log.debug("Entering ReplicatePageVersionServlet.doPost(..)");

        String[] rootPaths = req.getParameterValues("rootPaths");
        Date date = getDate(req.getParameter("datetimecal"));
        String[] agents = req.getParameterValues("cmbAgent");

        JsonObject obj = validate(rootPaths, agents, date);

        if (!obj.has(KEY_ERROR)) {
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

            JsonArray arr = convertResponseToJson(response);
            obj = new JsonObject();
            obj.add(KEY_RESULT, arr);

        } else {
            log.debug("Did not attempt to replicate version due to issue with input params");

            res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            obj.addProperty(KEY_STATUS, KEY_ERROR);
        }

        res.setContentType("application/json");
        res.getWriter().print(obj.toString());
    }

    private JsonArray convertResponseToJson(List<ReplicationResult> list) {
        JsonArray arr = new JsonArray();

        for (ReplicationResult result : list) {
            JsonObject resultObject = new JsonObject();

            resultObject.addProperty(KEY_PATH, result.getPath());
            resultObject.addProperty(KEY_STATUS, result.getStatus().name());
            resultObject.addProperty(KEY_VERSION, result.getVersion());

            arr.add(resultObject);
        }
        return arr;
    }


    private JsonObject validate(String[] rootPaths, String[] agents, Date date) {

        final JsonObject obj = new JsonObject();

        if (ArrayUtils.isEmpty(rootPaths)) {
            obj.addProperty(KEY_ERROR, "Select at least 1 root path.");
            log.debug("Error validating root paths (they're empty)");
            return obj;
        }

        for (final String rootPath : rootPaths) {
            if (StringUtils.isBlank(rootPath)) {
                obj.addProperty(KEY_ERROR, "Root paths cannot be empty.");
                log.debug("Error validating a root path");
                return obj;
            }
        }

        if (date == null) {
            obj.addProperty(KEY_ERROR, "Specify the date and time to select the appropriate resource versions for replication.");
            log.debug("Error validating date");
            return obj;
        }

        if (ArrayUtils.isEmpty(agents)) {
            obj.addProperty(KEY_ERROR, "Select at least 1 replication agent.");
            log.debug("Error validating agents");
            return obj;
        }

        log.debug("Validated all version replication inputs successfully");

        return obj;
    }

    private Date getDate(String datetime) {
        Date date = null;
        try {
            String modifiedDate = datetime.substring(0, datetime.indexOf('+'));
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm");
            sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
            date = sdf.parse(modifiedDate);
        } catch (Exception e) {
            log.error("exception occurred", e);
        }
        return date;
    }
}
