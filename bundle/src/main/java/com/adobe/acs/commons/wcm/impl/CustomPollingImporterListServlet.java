/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2014 Adobe
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
package com.adobe.acs.commons.wcm.impl;

import java.io.IOException;

import javax.servlet.ServletException;

import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Deactivate;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Constants;
import org.osgi.framework.Filter;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;
import org.osgi.service.component.ComponentContext;
import org.osgi.util.tracker.ServiceTracker;

import org.apache.sling.xss.XSSAPI;
import com.day.cq.polling.importer.Importer;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

@SlingServlet(paths = "/bin/acs-commons/custom-importers")
public final class CustomPollingImporterListServlet extends SlingSafeMethodsServlet {

    private static final long serialVersionUID = -4921197948987912363L;

    private transient ServiceTracker tracker;

    @Activate
    protected void activate(ComponentContext ctx) throws InvalidSyntaxException {
        final BundleContext bundleContext = ctx.getBundleContext();
        StringBuilder builder = new StringBuilder();
        builder.append("(&(");
        builder.append(Constants.OBJECTCLASS).append("=").append(Importer.SERVICE_NAME).append(")");
        builder.append("(displayName=*))");
        Filter filter = bundleContext.createFilter(builder.toString());
        this.tracker = new ServiceTracker(bundleContext, filter, null);
        this.tracker.open();
    }

    @Deactivate
    protected void deactivate() {
        this.tracker.close();
    }

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws ServletException,
            IOException {
        XSSAPI xssApi = request.adaptTo(XSSAPI.class);
        JsonObject result = new JsonObject();
        JsonArray list = new JsonArray();
        result.add("list", list);

        ServiceReference[] services = tracker.getServiceReferences();
        if (services != null) {
            for (ServiceReference service : services) {
                String displayName = PropertiesUtil.toString(service.getProperty("displayName"), null);
                String[] schemes = PropertiesUtil.toStringArray(service.getProperty(Importer.SCHEME_PROPERTY));
                if (displayName != null && schemes != null) {
                    for (String scheme : schemes) {
                        JsonObject obj = new JsonObject();
                        obj.addProperty("qtip", "");
                        obj.addProperty("text", displayName);
                        obj.addProperty("text_xss", xssApi.encodeForJSString(displayName));
                        obj.addProperty("value", scheme);
                        list.add(obj);
                    }
                }
            }
        }

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        Gson gson = new Gson();
        gson.toJson(result, response.getWriter());
    }

}
