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

package com.adobe.acs.commons.wcm.impl;

import com.adobe.acs.commons.json.AbstractJSONObjectVisitor;
import com.adobe.acs.commons.util.BufferingResponse;
import com.adobe.acs.commons.util.InfoWriter;
import com.adobe.acs.commons.util.PathInfoUtil;
import com.day.cq.commons.jcr.JcrConstants;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.scr.annotations.Activate;
import org.apache.felix.scr.annotations.Property;
import org.apache.felix.scr.annotations.sling.SlingServlet;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.json.JSONException;
import org.apache.sling.commons.json.JSONObject;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ACS AEM Commons - CQInclude Property Namespace.
 */
@SuppressWarnings("serial")
@SlingServlet(
        selectors = "overlay.cqinclude.namespace",
        extensions = "json",
        resourceTypes = "sling/servlet/default")
public final class CQIncludePropertyNamespaceServlet extends SlingSafeMethodsServlet {
    private static final Logger log = LoggerFactory.getLogger(CQIncludePropertyNamespaceServlet.class);

    private static final String REQ_ATTR = CQIncludePropertyNamespaceServlet.class.getName() + ".processed";

    private static final String AEM_CQ_INCLUDE_SELECTORS = "overlay.infinity";

    private static final int NAME_PROPERTY_SELECTOR_INDEX = 3;

    private static final String PN_NAME = "name";

    private static final String NT_CQ_WIDGET = "cq:Widget";

    private static final String[] DEFAULT_NAMESPACEABLE_PROPERTY_NAMES = new String[]{
            PN_NAME,
            "cropParameter",
            "fileNameParameter",
            "fileReferenceParameter",
            "mapParameter",
            "rotateParameter",
            "widthParameter",
            "heightParameter"
    };

    private String[] namespaceablePropertyNames = null;

    @Property(label = "Property Names",
            description = "Namespace properties defined in this list. Leave empty for on 'name'. "
                    + " Defaults to [ name, cropParameter, fileNameParameter, fileReferenceParameter, "
                    + "mapParameter, rotateParameter, widthParameter, heightParameter] ",
            value = {
                    PN_NAME,
                    "cropParameter",
                    "fileNameParameter",
                    "fileReferenceParameter",
                    "mapParameter",
                    "rotateParameter",
                    "widthParameter",
                    "heightParameter"
            }
    )
    public static final String PROP_NAMESPACEABLE_PROPERTY_NAMES = "namespace.property-names";

    private static final String[] DEFAULT_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS = new String[]{ "^\\./.*" };

    private List<Pattern> namespaceablePropertyValuePatterns = new ArrayList<Pattern>();

    @Property(label = "Property Value Patterns",
            description = "Namespace properties whose values match a regex in this list. "
                    + "Defaults to [ \"^\\\\./.*\" ]",
            value = { "^\\./.*" })
    public static final String PROP_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS = "namespace.property-value-patterns";


    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!this.accepts(request)) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write(new JSONObject().toString());
        }

        /* Servlet accepts this request */
        RequestUtil.setRequestAttribute(request, REQ_ATTR, true);

        final String namespace =
                URLDecoder.decode(PathInfoUtil.getSelector(request, NAME_PROPERTY_SELECTOR_INDEX), "UTF-8");

        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors(AEM_CQ_INCLUDE_SELECTORS);

        final BufferingResponse bufferingResponse = new BufferingResponse(response);
        request.getRequestDispatcher(request.getResource(), options).forward(request, bufferingResponse);


        try {
            final JSONObject json = new JSONObject(bufferingResponse.getContents());
            final PropertyNamespaceUpdater propertyNamespaceUpdater = new PropertyNamespaceUpdater(namespace);

            propertyNamespaceUpdater.accept(json);
            response.getWriter().write(json.toString());

        } catch (JSONException e) {
            log.error("Error composing the cqinclude JSON representation of the widget overlay for [ {} ]",
                    request.getRequestURI(), e);

            response.setStatus(SlingHttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(new JSONObject().toString());
        }
    }

    protected boolean accepts(SlingHttpServletRequest request) {
        if (request.getAttribute(REQ_ATTR) != null) {
            // Cyclic loop
            log.warn("Identified a cyclic loop in the ACS Commons CQ Include Namespace prefix Servlet for [ {} ]",
                    request.getRequestURI());
            return false;
        }

        for (int i = 0; i <= NAME_PROPERTY_SELECTOR_INDEX; i++) {
            if (StringUtils.isBlank(PathInfoUtil.getSelector(request, i))) {
                // Missing necessary selectors; the first N - 1 should be redundant since the selectors are specified
                // in the Servlet registration
                return false;
            }
        }

        return true;
    }

    @Activate
    protected void activate(final Map<String, Object> config) {
        // Property Names
        namespaceablePropertyNames = PropertiesUtil.toStringArray(config.get(PROP_NAMESPACEABLE_PROPERTY_NAMES),
                DEFAULT_NAMESPACEABLE_PROPERTY_NAMES);

        // Property Value Patterns
        namespaceablePropertyValuePatterns = new ArrayList<Pattern>();
        String[] regexes = PropertiesUtil.toStringArray(config.get(PROP_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS),
                DEFAULT_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS);

        for (final String regex : regexes) {
            namespaceablePropertyValuePatterns.add(Pattern.compile(regex));
        }

        final InfoWriter iw = new InfoWriter();
        iw.title("ACS AEM Commons - CQInclude Property Namespace Servlet");
        iw.message("Namespace-able Property Names: {}", Arrays.asList(namespaceablePropertyNames));
        iw.message("Namespace-able Property Value Patterns: {}", namespaceablePropertyValuePatterns);
        iw.end();
        log.info(iw.toString());
    }

    public final class PropertyNamespaceUpdater extends AbstractJSONObjectVisitor {
        private final Logger log = LoggerFactory.getLogger(PropertyNamespaceUpdater.class);

        private final String namespace;

        private static final String DOT_SLASH = "./";

        public PropertyNamespaceUpdater(final String namespace) {
            this.namespace = namespace;
        }

        private boolean accept(String propertyName, String propertyValue) {
            // Check if the property name denotes namespaceability
            if (namespaceablePropertyNames != null) {
                for (final String name : namespaceablePropertyNames) {
                    if (StringUtils.equals(name, propertyName)) {
                        return true;
                    }
                }
            }

            // Check if the property value denotes namespaceability
            if (namespaceablePropertyValuePatterns != null) {
                for (final Pattern pattern : namespaceablePropertyValuePatterns) {
                    final Matcher matcher = pattern.matcher(propertyValue);
                    if (matcher.matches()) {
                        return true;
                    }
                }
            }

            return false;
        }


        @Override
        protected void visit(JSONObject jsonObject) {

            if (StringUtils.equals(jsonObject.optString(JcrConstants.JCR_PRIMARYTYPE), NT_CQ_WIDGET)) {
                final Iterator<String> keys = jsonObject.keys();

                while (keys.hasNext()) {
                    final String propertyName = keys.next();

                    if (!this.accept(propertyName, jsonObject.optString(propertyName))) {
                        log.debug("Property [ {} ~> {} ] is not a namespaceable property name/value", propertyName,
                                jsonObject.optString(propertyName));
                        continue;
                    }

                    String value = jsonObject.optString(propertyName);

                    if (value != null) {
                        String prefix = "";
                        if (StringUtils.startsWith(value, DOT_SLASH)) {
                            value = StringUtils.removeStart(value, DOT_SLASH);
                            prefix = DOT_SLASH;
                        }

                        if (StringUtils.isNotBlank(value)) {
                            try {
                                jsonObject.put(propertyName, prefix + namespace + "/" + value);
                            } catch (final JSONException e) {
                                log.error("Error updating the Name property of the JSON object", e);
                            }
                        }
                    }
                }
            }
        }
    }
}