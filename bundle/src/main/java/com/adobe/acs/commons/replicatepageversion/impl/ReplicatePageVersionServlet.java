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
package com.adobe.acs.commons.replicatepageversion.impl;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingAllMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.replicatepageversion.ReplicatePageVersionService;

@SuppressWarnings("serial")
@SlingServlet(paths = "/bin/replicatepageversion", methods = "POST",
        generateComponent = true)
public class ReplicatePageVersionServlet extends SlingAllMethodsServlet {

    private static final Logger log = LoggerFactory
            .getLogger(ReplicatePageVersionServlet.class);

    @Reference
    private ReplicatePageVersionService rps;

    @Override
    public final void doPost(SlingHttpServletRequest req,
            SlingHttpServletResponse res) throws ServletException, IOException {
        log.info("in do post - ReplicatePageVersionServlet ");
        String pageRoot = getNormalizedPath(req.getParameter("pathPage"));
        String assetRoot = getNormalizedPath(req.getParameter("pathAsset"));
        Date date = getDate(req.getParameter("datetimecal"));
        String agent = req.getParameter("cmbAgent");
        JSONObject obj = null;
        try {
            obj = validate(pageRoot, assetRoot, agent, date);
            boolean error = false;

            if (!obj.has("error")) {
                obj = rps.locateVersionAndReplicateResource(req.getResourceResolver(),
                        pageRoot, assetRoot, agent, date);
            } else {
                error = true;
            }

            if (error) {
                if (obj == null) {
                    obj = new JSONObject();
                }

                try {
                    obj.put("error", "System Error.");
                    obj.put("status", "error");
                } catch (JSONException e) {
                   log.error("exception occured", e);
                }
            }
        } catch (JSONException ex) {
            try {
                if (obj == null) {
                    obj = new JSONObject();
                }
                obj.put("error", "System Error.");
                obj.put("status", "error");
            } catch (JSONException e) {
                log.error("exception occured", e);
            }
        }

        try {
            res.setContentType("text/json");
            obj.write(res.getWriter());
        } catch (JSONException e) {
            log.error("exception occured", e);
        }
    }

    /**
     * 
     * @param svm
     * @return
     * @throws JSONException
     */
    private JSONObject validate(String pageRoot, String assetRoot,
            String agent, Date date) throws JSONException {
        JSONObject obj = new JSONObject();
        if ((pageRoot == null || "".equals(pageRoot)) && assetRoot == null
                || "".equals(assetRoot)) {
            obj.put("error",
                    "both pages root path and assets root path cannot be null");
            return obj;
        }
        if (date == null) {
            obj.put("error", "Enter the time at which you want the versions");
            return obj;
        }
        if (agent == null || "".equals(agent)) {
            obj.put("error", "Enter the agent id");
            return obj;
        }
        return obj;
    }

    private String getNormalizedPath(String path) {
        String root = path;
        if (root == null || "".equals(root)) {
            return null;
        }
        while (root.endsWith("/")) {
            root = root.substring(0, root.length() - 1);
        }

        if (root.length() == 0) {
            root = "/";
        }

        return root;
    }

    private Date getDate(String datetime) {
        Date date = null;
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd,hh:mm:ss");
            date = sdf.parse(datetime);
        } catch (Exception e) {
            log.error("exception occured", e);
        }
        return date;
    }
}
