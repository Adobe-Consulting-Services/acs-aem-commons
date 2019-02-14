/*
 * #%L
 * ACS AEM Commons Bundle
 * %%
 * Copyright (C) 2015 Adobe
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

package com.adobe.acs.commons.wcm.views.impl;

import com.adobe.acs.commons.util.ParameterUtil;
import com.day.cq.wcm.api.Page;
import com.day.cq.wcm.api.PageManager;
import com.day.cq.wcm.api.WCMMode;
import com.day.cq.wcm.api.components.Component;
import com.day.cq.wcm.commons.WCMUtils;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.resource.AbstractResourceVisitor;
import org.apache.sling.api.resource.Resource;
import org.apache.sling.api.resource.ValueMap;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@SuppressWarnings("serial")
@SlingServlet(
        label = "ACS AEM Commons - WCM Views Servlet",
        methods = {"GET"},
        resourceTypes = {"cq/Page"},
        selectors = {"wcm-views"},
        extensions = {"json"},
        metatype = true
)
public class WCMViewsServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(WCMViewsServlet.class);

    private static final String[] DEFAULT_VIEWS = new String[]{};
    private Map<String, String[]> defaultViews = new HashMap<String, String[]>();

    @Property(label = "WCM Views by Path",
            description = "Views to add to the Sidekick by default. Takes format [/path=view-1;view-2]",
            cardinality = Integer.MAX_VALUE,
            value = {})
    public static final String PROP_DEFAULT_VIEWS = "wcm-views";

    @Override
    protected final void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response) throws
            ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");

        if (WCMMode.DISABLED.equals(WCMMode.fromRequest(request))) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("");
            return;
        }
        
        /* Valid WCMMode */

        final PageManager pageManager = request.getResourceResolver().adaptTo(PageManager.class);
        final Page page = pageManager.getContainingPage(request.getResource());

        final WCMViewsResourceVisitor visitor = new WCMViewsResourceVisitor();
        visitor.accept(page.getContentResource());

        final Set<String> viewSet = new HashSet<String>(visitor.getWCMViews());

        // Get the Views provided by the Servlet
        for(final Map.Entry<String, String[]> entry : this.defaultViews.entrySet()) {
            if(StringUtils.startsWith(page.getPath(), entry.getKey())) {
                viewSet.addAll(Arrays.asList(entry.getValue()));
            }
        }
        
        final List<String> views = new ArrayList<String>(viewSet);
        
        Collections.sort(views);

        log.debug("Collected WCM Views {} for Page [ {} ]", views, page.getPath());
        
        final JsonArray jsonArray = new JsonArray();

        for (final String view : views) {
            final JsonObject json = new JsonObject();
            json.addProperty("title", StringUtils.capitalize(view) + " View");
            json.addProperty("value", view);

            jsonArray.add(json);
        }

        Gson gson = new Gson();
        gson.toJson(jsonArray, response.getWriter());
    }

    private static class WCMViewsResourceVisitor extends AbstractResourceVisitor {
        final Set<String> views = new TreeSet<String>();

        public final List<String> getWCMViews() {
            return new ArrayList<String>(this.views);
        }

        @Override
        protected void visit(Resource resource) {
            final ValueMap properties = resource.adaptTo(ValueMap.class);
            final String[] resourceViews = properties.get(WCMViewsFilter.PN_WCM_VIEWS, String[].class);

            if (ArrayUtils.isNotEmpty(resourceViews)) {
                this.views.addAll(Arrays.asList(resourceViews));
            }

            final Component component = WCMUtils.getComponent(resource);
            if (component != null) {
                final String[] componentViews = component.getProperties().get(WCMViewsFilter.PN_WCM_VIEWS, String[].class);

                if (ArrayUtils.isNotEmpty(componentViews)) {
                    this.views.addAll(Arrays.asList(componentViews));
                }
            }
        }
    }

    @Activate
    protected final void activate(final Map<String, String> config) {
        final String[] tmp = PropertiesUtil.toStringArray(config.get(PROP_DEFAULT_VIEWS), DEFAULT_VIEWS);
        this.defaultViews = ParameterUtil.toMap(tmp, "=", ";");
    }
}