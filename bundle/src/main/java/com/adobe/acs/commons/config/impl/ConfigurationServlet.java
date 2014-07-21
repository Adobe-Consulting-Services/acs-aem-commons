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
package com.adobe.acs.commons.config.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Component;
import org.apache.felix.scr.annotations.Properties;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.Reference;
import org.apache.felix.scr.annotations.Service;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONArray;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.config.Configuration;
import com.adobe.acs.commons.config.ConfigurationService;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;

@SuppressWarnings({ "serial" })
@Component(
        label = "ACS AEM Commons - Configuration Servlet",
        description = "Servlet to render json of configurations having columns key,text,value.",
        immediate = false, metatype = false)
@Service
@Properties({
        @Property(name = "sling.servlet.extensions", value = "json"),
        @Property(name = "sling.servlet.selectors", value = "acsconfig"),
        @Property(name = "sling.servlet.resourceTypes",
                value = "sling/servlet/default"),
        @Property(name = "sling.servlet.methods", value = "GET") })
public class ConfigurationServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory
            .getLogger(ConfigurationServlet.class);
    @Reference
    private ConfigurationService configService;

    @Override
    public final void doGet(SlingHttpServletRequest request,
            SlingHttpServletResponse response) throws IOException,
            ServletException {
        PageManager pageManager =
                request.getResourceResolver().adaptTo(PageManager.class);
        Page page = pageManager.getContainingPage(request.getResource());
        Configuration config = configService.getConfiguration(page);
        String suffix = request.getRequestPathInfo().getSuffix();
        String key = suffix == null ? "" : suffix.substring(1);
        List<Map<String, String>> rows = config.getRowsByKey(key);
        JSONArray jarray = new JSONArray();
        try {
            for (Map<String, String> row : rows) {
                JSONObject jobject = new JSONObject();
                jobject.put("value", row.get("value"));
                jobject.put("text", row.get("text"));
                jarray.put(jobject);
            }
        } catch (JSONException e) {
            log.error("Exception in writing json object",e);
        }
        try {
            jarray.write(response.getWriter());
        } catch (JSONException e) {
            log.error("Exception in writing json object",e);
        }
    }
}
