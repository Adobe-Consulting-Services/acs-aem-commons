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

import static com.adobe.acs.commons.json.JsonObjectUtil.getString;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_EXTENSIONS;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_RESOURCE_TYPES;
import static org.apache.sling.api.servlets.ServletResolverConstants.SLING_SERVLET_SELECTORS;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.Servlet;
import javax.servlet.ServletException;

import org.apache.commons.lang.StringUtils;
import org.apache.sling.api.SlingHttpServletRequest;
import org.apache.sling.api.SlingHttpServletResponse;
import org.apache.sling.api.request.RequestDispatcherOptions;
import org.apache.sling.api.request.RequestUtil;
import org.apache.sling.api.servlets.SlingSafeMethodsServlet;
import org.apache.sling.commons.osgi.PropertiesUtil;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.adobe.acs.commons.json.AbstractJSONObjectVisitor;
import com.adobe.acs.commons.util.BufferingResponse;
import com.adobe.acs.commons.util.InfoWriter;
import com.adobe.acs.commons.util.PathInfoUtil;
import com.day.cq.commons.jcr.JcrConstants;
import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

/**
 * ACS AEM Commons - CQInclude Property Namespace.
 */
//@formatter:off
@SuppressWarnings({"serial", "checkstyle:abbreviationaswordinname"})
@Component(
        service = Servlet.class,
        property =
                {
                        SLING_SERVLET_RESOURCE_TYPES + "=sling/servlet/default",
                        SLING_SERVLET_EXTENSIONS + "=json",
                        SLING_SERVLET_SELECTORS + "=overlay.cqinclude.namespace"
                }

)
@Designate(
        ocd = CQIncludePropertyNamespaceServlet.Config.class
)
//@formatter:on
public final class CQIncludePropertyNamespaceServlet extends SlingSafeMethodsServlet {
    //@formatter:off
    private static final Logger log = LoggerFactory.getLogger(CQIncludePropertyNamespaceServlet.class);

    private static final String REQ_ATTR = CQIncludePropertyNamespaceServlet.class.getName() + ".processed";

    private static final String AEM_CQ_INCLUDE_SELECTORS = "overlay.infinity";

    private static final String CQINCLUDE_NAMESPACE_URL_REGEX = "(.+\\.cqinclude\\.namespace\\.)(.+)(\\.json)";

    private static final int NAME_PROPERTY_SELECTOR_INDEX = 3;

    private static final String PN_NAME = "name";

    private static final String PN_XTYPE = "xtype";

    private static final String PN_PATH = "path";

    private static final String NT_CQ_WIDGET = "cq:Widget";

    private static final String ESCAPED_SLASH = "%252F";

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

    @ObjectClassDefinition(
            name = "ACS AEM Commons - CQInclude Property Namespace"
    )
    public @interface Config {

        @AttributeDefinition(name = "Property Names",
                description = "Namespace properties defined in this list. Leave empty for on 'name'. "
                        + " Defaults to [ name, cropParameter, fileNameParameter, fileReferenceParameter, "
                        + "mapParameter, rotateParameter, widthParameter, heightParameter] ",
                defaultValue = {
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
        String[] namespace_property$_$names();

        @AttributeDefinition(name = "Property Value Patterns",
                description = "Namespace properties whose values match a regex in this list. "
                        + "Defaults to [ \"^\\\\./.*\" ]",
                defaultValue = {"^\\./.*"})
        String namespace_property$_$value$_$patterns();

        @AttributeDefinition(name = "Support Multi-level",
                description = "When set to true, cqinclude servlet will support multi-level path-ing if nested cqinclude namespaces. Defaults to false",
                defaultValue = "false")
        boolean namespace_multi$_$level();
    }

    public static final String PROP_NAMESPACEABLE_PROPERTY_NAMES = "namespace.property-names";

    private static final String[] DEFAULT_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS = new String[]{"^\\./.*"};
    private List<Pattern> namespaceablePropertyValuePatterns = new ArrayList<Pattern>();


    public static final String PROP_NAMESPACEABLE_PROPERTY_VALUE_PATTERNS = "namespace.property-value-patterns";


    private static final boolean DEFAULT_SUPPORT_MULTI_LEVEL = false;
    private boolean supportMultiLevel = DEFAULT_SUPPORT_MULTI_LEVEL;

    public static final String PROP_SUPPORT_MULTI_LEVEL = "namespace.multi-level";
    //@formatter:on

    @Override
    protected void doGet(SlingHttpServletRequest request, SlingHttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        if (!this.accepts(request)) {
            response.setStatus(SlingHttpServletResponse.SC_NOT_FOUND);
            response.getWriter().write("{}");
        }

        /* Servlet accepts this request */
        RequestUtil.setRequestAttribute(request, REQ_ATTR, true);

        final String namespace =
                URLDecoder.decode(PathInfoUtil.getSelector(request, NAME_PROPERTY_SELECTOR_INDEX), "UTF-8");

        final RequestDispatcherOptions options = new RequestDispatcherOptions();
        options.setReplaceSelectors(AEM_CQ_INCLUDE_SELECTORS);

        final BufferingResponse bufferingResponse = new BufferingResponse(response);
        request.getRequestDispatcher(request.getResource(), options).forward(request, bufferingResponse);

        Gson gson = new Gson();
         final JsonObject json = gson.toJsonTree(bufferingResponse.getContents()).getAsJsonObject();
         final PropertyNamespaceUpdater propertyNamespaceUpdater = new PropertyNamespaceUpdater(namespace);

         propertyNamespaceUpdater.accept(json);
         response.getWriter().write(json.toString());
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
        supportMultiLevel = PropertiesUtil.toBoolean(config.get(PROP_SUPPORT_MULTI_LEVEL), DEFAULT_SUPPORT_MULTI_LEVEL);

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
        
        protected boolean isCqincludeNamespaceWidget(JsonObject jsonObject) {
            if (StringUtils.equals(getString(jsonObject,JcrConstants.JCR_PRIMARYTYPE), NT_CQ_WIDGET)
                    && (StringUtils.equals(getString(jsonObject,PN_XTYPE), "cqinclude"))) {
                String path = getString(jsonObject,PN_PATH);
                if (StringUtils.isNotBlank(path)
                        && path.matches(CQINCLUDE_NAMESPACE_URL_REGEX)) {
                    return true;

                }
            }

            return false;
        }

        protected JsonObject makeMultiLevel(JsonObject jsonObject) {
            String path = getString(jsonObject, PN_PATH);
            if (StringUtils.isNotBlank(path)) {
                Pattern pattern = Pattern.compile(CQINCLUDE_NAMESPACE_URL_REGEX);
                Matcher m = pattern.matcher(path);
                if (m.matches()) {
                    path = m.group(1) + this.namespace + ESCAPED_SLASH + m.group(2) + m.group(3);
                    jsonObject.addProperty(PN_PATH, path);
                }
            }

            return jsonObject;
        }

        @SuppressWarnings("squid:S3776")
        @Override
        protected void visit(JsonObject jsonObject) {

            if (StringUtils.equals(getString(jsonObject,JcrConstants.JCR_PRIMARYTYPE), NT_CQ_WIDGET)) {
                if (supportMultiLevel && isCqincludeNamespaceWidget(jsonObject)) {
                    jsonObject = makeMultiLevel(jsonObject);
                }

                for (Entry<String, JsonElement> elem : jsonObject.entrySet()) {
                    final String propertyName = elem.getKey();                    
                    String value = elem.getValue().getAsString();
                    if (!this.accept(propertyName, value)) {
                        log.debug("Property [ {} ~> {} ] is not a namespace-able property name/value", propertyName, value);
                        continue;
                    }

                    if (value != null) {
                        String prefix = "";
                        if (StringUtils.startsWith(value, DOT_SLASH)) {
                            value = StringUtils.removeStart(value, DOT_SLASH);
                            prefix = DOT_SLASH;
                        }

                        if (StringUtils.isNotBlank(value)) {
                            jsonObject.addProperty(propertyName, prefix + namespace + "/" + value);
                        }
                    }
                }
            }
        }
    }
}